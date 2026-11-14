package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.*;
import com.example.projetoPBD.pontoCerto.dto.HorarioPrevistoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ApuracaoDiariaDTO;
import com.example.projetoPBD.pontoCerto.repository.*;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.PeriodoFechadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ApuracaoDiariaService {

    private static final int TOLERANCIA_PADRAO_CLT_MINUTOS = 10;
    private static final LocalTime PADRAO_INICIO_NOTURNO = LocalTime.of(22, 0);
    private static final LocalTime PADRAO_FIM_NOTURNO = LocalTime.of(5, 0);
    private static final int DURACAO_HORA_NOTURNA_PADRAO_SEGUNDOS = 3150; // 52 min 30 seg

    private final ApuracaoDiariaRepository apuracaoRepository;
    private final BatidaRepository batidaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final RegimeTrabalhoService regimeTrabalhoService;
    private final RegraApuracaoRepository regraApuracaoRepository;
    private final InconsistenciaService inconsistenciaService;
    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;
    private final FeriadoRepository feriadoRepository;
    private final SecurityTenantContext securityTenantContext;
    private final ObjectMapper objectMapper;

    public ApuracaoDiariaService(
            ApuracaoDiariaRepository apuracaoRepository,
            BatidaRepository batidaRepository,
            FuncionarioRepository funcionarioRepository,
            RegimeTrabalhoService regimeTrabalhoService,
            RegraApuracaoRepository regraApuracaoRepository,
            InconsistenciaService inconsistenciaService,
            FechamentoPeriodoRepository fechamentoPeriodoRepository,
            FeriadoRepository feriadoRepository,
            SecurityTenantContext securityTenantContext
    ) {
        this.apuracaoRepository = apuracaoRepository;
        this.batidaRepository = batidaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.regimeTrabalhoService = regimeTrabalhoService;
        this.regraApuracaoRepository = regraApuracaoRepository;
        this.inconsistenciaService = inconsistenciaService;
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
        this.feriadoRepository = feriadoRepository;
        this.securityTenantContext = securityTenantContext;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public ApuracaoDiaria apurarDia(Funcionario funcionario, LocalDate dataReferencia) {
        if (funcionario == null || dataReferencia == null) {
            return null;
        }

        UUID empresaId = funcionario.getEmpresa().getId();
        validarPeriodoNaoFechado(empresaId, dataReferencia);

        HorarioPrevistoDTO.Response horario = regimeTrabalhoService.obterHorarioPrevisto(funcionario.getId(), dataReferencia);
        boolean ehDiaTrabalho = horario != null && horario.diaTrabalho();
        int minutosPrevistos = (horario != null && horario.cargaDiariaMinutos() != null) ? horario.cargaDiariaMinutos() : 0;
        int toleranciaMinutos = (horario != null && horario.toleranciaMinutos() != null && horario.toleranciaMinutos() > 0)
                ? horario.toleranciaMinutos()
                : TOLERANCIA_PADRAO_CLT_MINUTOS;

        LocalDateTime inicioDia = dataReferencia.atStartOfDay();
        LocalDateTime fimDia = dataReferencia.atTime(LocalTime.MAX);

        List<Batida> batidas = batidaRepository.findAtivasByFuncionarioIdAndDataHoraBetweenOrderByDataHoraAsc(
                funcionario.getId(),
                inicioDia,
                fimDia
        );

        ApuracaoDiaria apuracao = apuracaoRepository
                .findByFuncionarioIdAndDataReferencia(funcionario.getId(), dataReferencia)
                .orElseGet(() -> {
                    ApuracaoDiaria nova = new ApuracaoDiaria();
                    nova.setFuncionario(funcionario);
                    nova.setDataReferencia(dataReferencia);
                    return nova;
                });

        apuracao.setCalculadoEm(LocalDateTime.now());
        apuracao.setMinutosPrevistos(minutosPrevistos);

        // CASO 1: Zero batidas
        if (batidas.isEmpty()) {
            boolean feriado = verificarSeEhFeriado(funcionario, dataReferencia);
            if (feriado || !ehDiaTrabalho) {
                apuracao.setStatus(StatusApuracaoDiaria.FOLGA);
                zerarTotalizadores(apuracao);
                apuracao.setMinutosPrevistos(0);
            } else {
                apuracao.setStatus(StatusApuracaoDiaria.FALTA);
                zerarTotalizadores(apuracao);
                apuracao.setMinutosFalta(minutosPrevistos);
            }
            apuracao.setParesJson("[]");
            apuracao.setHorasExtrasPorFaixaJson("{}");
            return apuracaoRepository.saveAndFlush(apuracao);
        }

        // CASO 2: Quantidade ímpar de marcações (Não inventa batida nem arredonda)
        if (batidas.size() % 2 != 0) {
            try {
                inconsistenciaService.analisarDiaColaborador(funcionario, dataReferencia);
            } catch (Exception ignored) {}

            apuracao.setStatus(StatusApuracaoDiaria.PENDENTE_INCONSISTENCIA);
            zerarTotalizadores(apuracao);
            apuracao.setParesJson("[]");
            apuracao.setHorasExtrasPorFaixaJson("{}");
            return apuracaoRepository.saveAndFlush(apuracao);
        }

        // CASO 3: Quantidade par (>= 2) -> Pareamento sequencial
        apuracao.setStatus(StatusApuracaoDiaria.APURADO);

        List<ApuracaoDiariaDTO.ParMarcacao> pares = new ArrayList<>();
        int minutosTrabalhados = 0;
        int minutosIntervalo = 0;

        for (int i = 0; i < batidas.size(); i += 2) {
            Batida entrada = batidas.get(i);
            Batida saida = batidas.get(i + 1);

            int duracaoPar = (int) ChronoUnit.MINUTES.between(entrada.getDataHora(), saida.getDataHora());
            if (duracaoPar < 0) duracaoPar = 0;
            minutosTrabalhados += duracaoPar;

            pares.add(new ApuracaoDiariaDTO.ParMarcacao(
                    entrada.getDataHora().toLocalTime(),
                    saida.getDataHora().toLocalTime(),
                    duracaoPar,
                    entrada.getTipoOrigem() != null ? entrada.getTipoOrigem().name() : "DESCONHECIDO",
                    saida.getTipoOrigem() != null ? saida.getTipoOrigem().name() : "DESCONHECIDO"
            ));

            if (i > 0) {
                Batida saidaAnterior = batidas.get(i - 1);
                int intervalo = (int) ChronoUnit.MINUTES.between(saidaAnterior.getDataHora(), entrada.getDataHora());
                if (intervalo > 0) {
                    minutosIntervalo += intervalo;
                }
            }
        }

        // Cálculo de Atrasos e Saídas Antecipadas com Tolerância CLT
        int minutosAtraso = 0;
        int minutosSaidaAntecipada = 0;

        if (ehDiaTrabalho && horario.horaEntrada() != null && horario.horaSaida() != null) {
            LocalTime primeiraEntrada = batidas.get(0).getDataHora().toLocalTime();
            LocalTime ultimaSaida = batidas.get(batidas.size() - 1).getDataHora().toLocalTime();

            int atrasoEntrada = 0;
            if (primeiraEntrada.isAfter(horario.horaEntrada())) {
                atrasoEntrada = (int) ChronoUnit.MINUTES.between(horario.horaEntrada(), primeiraEntrada);
            }

            int saidaAntecipada = 0;
            if (ultimaSaida.isBefore(horario.horaSaida())) {
                saidaAntecipada = (int) ChronoUnit.MINUTES.between(ultimaSaida, horario.horaSaida());
            }

            int variacaoNegativaTotal = atrasoEntrada + saidaAntecipada;
            if (variacaoNegativaTotal > toleranciaMinutos) {
                minutosAtraso = atrasoEntrada;
                minutosSaidaAntecipada = saidaAntecipada;
            }
        }

        // Cálculo de Adicional Noturno com Hora Noturna Reduzida
        Optional<RegraApuracao> regraApuracaoOpt = regraApuracaoRepository
                .findFirstByEmpresaIdAndInicioVigenciaLessThanEqualOrderByInicioVigenciaDesc(empresaId, dataReferencia);

        RegraAdicionalNoturno regraNoturno = regraApuracaoOpt.map(RegraApuracao::getAdicionalNoturno).orElse(null);
        LocalTime inicioNoturno = (regraNoturno != null && regraNoturno.getHoraInicio() != null)
                ? regraNoturno.getHoraInicio().toLocalTime()
                : PADRAO_INICIO_NOTURNO;
        LocalTime fimNoturno = (regraNoturno != null && regraNoturno.getHoraFim() != null)
                ? regraNoturno.getHoraFim().toLocalTime()
                : PADRAO_FIM_NOTURNO;
        int duracaoHoraSegundos = (regraNoturno != null && regraNoturno.getDuracaoHoraNoturnaSegundos() != null)
                ? regraNoturno.getDuracaoHoraNoturnaSegundos()
                : DURACAO_HORA_NOTURNA_PADRAO_SEGUNDOS;

        int minutosNoturnosRelogio = 0;
        for (int i = 0; i < batidas.size(); i += 2) {
            minutosNoturnosRelogio += calcularMinutosNoturnosRelogio(
                    batidas.get(i).getDataHora(),
                    batidas.get(i + 1).getDataHora(),
                    inicioNoturno,
                    fimNoturno
            );
        }

        // Fator de conversão: 3600 / duracaoHoraSegundos (padrão 3600 / 3150 = 8/7)
        int minutosAdicionalNoturno = (int) Math.round(minutosNoturnosRelogio * (3600.0 / duracaoHoraSegundos));

        // Cálculo de Horas Extras e Rateio por Faixas
        int excedenteTrabalhado = minutosTrabalhados - minutosPrevistos;
        int minutosHoraExtraTotal = 0;
        Map<String, Integer> horasExtrasPorFaixa = new LinkedHashMap<>();

        if (excedenteTrabalhado > 0) {
            if (excedenteTrabalhado > toleranciaMinutos || !ehDiaTrabalho) {
                minutosHoraExtraTotal = excedenteTrabalhado;
                distribuirHorasExtras(horasExtrasPorFaixa, minutosHoraExtraTotal, regraApuracaoOpt.orElse(null), dataReferencia);
            }
        }

        apuracao.setMinutosTrabalhados(minutosTrabalhados);
        apuracao.setMinutosIntervalo(minutosIntervalo);
        apuracao.setMinutosAtraso(minutosAtraso);
        apuracao.setMinutosSaidaAntecipada(minutosSaidaAntecipada);
        apuracao.setMinutosAdicionalNoturno(minutosAdicionalNoturno);
        apuracao.setMinutosFalta(0);
        apuracao.setMinutosHoraExtraTotal(minutosHoraExtraTotal);

        try {
            apuracao.setParesJson(objectMapper.writeValueAsString(pares));
            apuracao.setHorasExtrasPorFaixaJson(objectMapper.writeValueAsString(horasExtrasPorFaixa));
        } catch (Exception e) {
            apuracao.setParesJson("[]");
            apuracao.setHorasExtrasPorFaixaJson("{}");
        }

        return apuracaoRepository.saveAndFlush(apuracao);
    }

    @Transactional
    public ApuracaoDiariaDTO.ResumoPeriodoResponse recalcularPeriodo(UUID funcionarioId, LocalDate dataInicio, LocalDate dataFim) {
        if (funcionarioId == null || dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Colaborador, data de início e fim são obrigatórios para recálculo.");
        }
        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("Data de fim não pode ser anterior à data de início.");
        }

        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));

        validarPeriodoNaoFechado(funcionario.getEmpresa().getId(), dataInicio);
        validarPeriodoNaoFechado(funcionario.getEmpresa().getId(), dataFim);

        LocalDate cursor = dataInicio;
        List<ApuracaoDiaria> apuracoes = new ArrayList<>();
        while (!cursor.isAfter(dataFim)) {
            ApuracaoDiaria ap = apurarDia(funcionario, cursor);
            if (ap != null) {
                apuracoes.add(ap);
            }
            cursor = cursor.plusDays(1);
        }

        return montarResumoPeriodo(funcionario, dataInicio, dataFim, apuracoes);
    }

    @Transactional(readOnly = true)
    public ApuracaoDiariaDTO.ResumoPeriodoResponse consultarPeriodo(UUID funcionarioId, LocalDate dataInicio, LocalDate dataFim) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));

        List<ApuracaoDiaria> apuracoes = apuracaoRepository
                .findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAsc(funcionarioId, dataInicio, dataFim);

        return montarResumoPeriodo(funcionario, dataInicio, dataFim, apuracoes);
    }

    private void distribuirHorasExtras(
            Map<String, Integer> destino,
            int totalMinutosExtras,
            RegraApuracao regra,
            LocalDate dataReferencia
    ) {
        if (totalMinutosExtras <= 0) return;

        boolean ehDomingo = dataReferencia.getDayOfWeek() == DayOfWeek.SUNDAY;

        if (regra != null && regra.getFaixaHoraExtra() != null && !regra.getFaixaHoraExtra().isEmpty()) {
            List<FaixaHoraExtra> faixas = new ArrayList<>(regra.getFaixaHoraExtra());
            faixas.sort(Comparator.comparing(FaixaHoraExtra::getOrdem));

            int minutosRestantes = totalMinutosExtras;
            for (FaixaHoraExtra faixa : faixas) {
                if (minutosRestantes <= 0) break;

                String chave = formatarPercentual(faixa.getPercentual());
                Integer duracao = faixa.getDuracaoMinutos();

                int minutosFaixa;
                if (duracao != null && duracao > 0) {
                    minutosFaixa = Math.min(minutosRestantes, duracao);
                } else {
                    minutosFaixa = minutosRestantes;
                }

                destino.put(chave, destino.getOrDefault(chave, 0) + minutosFaixa);
                minutosRestantes -= minutosFaixa;
            }

            if (minutosRestantes > 0) {
                String chaveUltima = formatarPercentual(faixas.get(faixas.size() - 1).getPercentual());
                destino.put(chaveUltima, destino.getOrDefault(chaveUltima, 0) + minutosRestantes);
            }
        } else {
            // Regra padrão CLT de fallback
            if (ehDomingo) {
                destino.put("100%", totalMinutosExtras);
            } else {
                destino.put("50%", totalMinutosExtras);
            }
        }
    }

    private String formatarPercentual(BigDecimal percentual) {
        if (percentual == null) return "50%";
        return percentual.stripTrailingZeros().toPlainString() + "%";
    }

    private int calcularMinutosNoturnosRelogio(
            LocalDateTime entrada,
            LocalDateTime saida,
            LocalTime inicioNoturno,
            LocalTime fimNoturno
    ) {
        int minutos = 0;
        LocalDateTime cursor = entrada;
        while (cursor.isBefore(saida)) {
            LocalTime t = cursor.toLocalTime();
            boolean ehNoturno;
            if (inicioNoturno.isAfter(fimNoturno)) {
                ehNoturno = !t.isBefore(inicioNoturno) || t.isBefore(fimNoturno);
            } else {
                ehNoturno = !t.isBefore(inicioNoturno) && t.isBefore(fimNoturno);
            }
            if (ehNoturno) {
                minutos++;
            }
            cursor = cursor.plusMinutes(1);
        }
        return minutos;
    }

    private boolean verificarSeEhFeriado(Funcionario funcionario, LocalDate data) {
        try {
            Empresa empresa = funcionario.getEmpresa();
            String uf = funcionario.getLocalDeTrabalho() != null ? funcionario.getLocalDeTrabalho().getUf() : "";
            String municipio = funcionario.getLocalDeTrabalho() != null ? funcionario.getLocalDeTrabalho().getMunicipio() : "";
            List<Feriado> feriados = feriadoRepository.buscarCalendarioAnual(
                    data.getYear(),
                    uf != null ? uf : "",
                    municipio != null ? municipio : "",
                    empresa.getId()
            );
            return feriados.stream().anyMatch(f -> data.equals(f.getData()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private void zerarTotalizadores(ApuracaoDiaria a) {
        a.setMinutosTrabalhados(0);
        a.setMinutosIntervalo(0);
        a.setMinutosAtraso(0);
        a.setMinutosSaidaAntecipada(0);
        a.setMinutosAdicionalNoturno(0);
        a.setMinutosFalta(0);
        a.setMinutosHoraExtraTotal(0);
    }

    private void validarPeriodoNaoFechado(UUID empresaId, LocalDate data) {
        String mesReferencia = data.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Optional<FechamentoPeriodo> fechamentoOpt = fechamentoPeriodoRepository
                .findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, mesReferencia);

        if (fechamentoOpt.isPresent()) {
            StatusFechamentoPeriodo status = fechamentoOpt.get().getStatus();
            if (status == StatusFechamentoPeriodo.CONCLUIDO || status == StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS) {
                throw new PeriodoFechadoException("O período " + mesReferencia + " encontra-se fechado. Operação não permitida.");
            }
        }
    }

    private ApuracaoDiariaDTO.ResumoPeriodoResponse montarResumoPeriodo(
            Funcionario funcionario,
            LocalDate inicio,
            LocalDate fim,
            List<ApuracaoDiaria> apuracoes
    ) {
        int totalPrevisto = 0;
        int totalTrabalhado = 0;
        int totalIntervalo = 0;
        int totalAtraso = 0;
        int totalSaidaAntecipada = 0;
        int totalNoturno = 0;
        int totalFalta = 0;
        int totalHoraExtra = 0;

        List<ApuracaoDiariaDTO.Response> diasResponses = new ArrayList<>();

        for (ApuracaoDiaria ap : apuracoes) {
            totalPrevisto += ap.getMinutosPrevistos() != null ? ap.getMinutosPrevistos() : 0;
            totalTrabalhado += ap.getMinutosTrabalhados() != null ? ap.getMinutosTrabalhados() : 0;
            totalIntervalo += ap.getMinutosIntervalo() != null ? ap.getMinutosIntervalo() : 0;
            totalAtraso += ap.getMinutosAtraso() != null ? ap.getMinutosAtraso() : 0;
            totalSaidaAntecipada += ap.getMinutosSaidaAntecipada() != null ? ap.getMinutosSaidaAntecipada() : 0;
            totalNoturno += ap.getMinutosAdicionalNoturno() != null ? ap.getMinutosAdicionalNoturno() : 0;
            totalFalta += ap.getMinutosFalta() != null ? ap.getMinutosFalta() : 0;
            totalHoraExtra += ap.getMinutosHoraExtraTotal() != null ? ap.getMinutosHoraExtraTotal() : 0;

            diasResponses.add(toDTO(ap));
        }

        return new ApuracaoDiariaDTO.ResumoPeriodoResponse(
                funcionario.getId(),
                funcionario.getNomeCompleto(),
                inicio,
                fim,
                apuracoes.size(),
                totalPrevisto,
                totalTrabalhado,
                totalIntervalo,
                totalAtraso,
                totalSaidaAntecipada,
                totalNoturno,
                totalFalta,
                totalHoraExtra,
                diasResponses
        );
    }

    public ApuracaoDiariaDTO.Response toDTO(ApuracaoDiaria ap) {
        List<ApuracaoDiariaDTO.ParMarcacao> pares = List.of();
        Map<String, Integer> extrasPorFaixa = Map.of();

        try {
            if (ap.getParesJson() != null && !ap.getParesJson().isBlank()) {
                pares = objectMapper.readValue(ap.getParesJson(), new TypeReference<>() {});
            }
        } catch (Exception ignored) {}

        try {
            if (ap.getHorasExtrasPorFaixaJson() != null && !ap.getHorasExtrasPorFaixaJson().isBlank()) {
                extrasPorFaixa = objectMapper.readValue(ap.getHorasExtrasPorFaixaJson(), new TypeReference<>() {});
            }
        } catch (Exception ignored) {}

        Funcionario f = ap.getFuncionario();
        return new ApuracaoDiariaDTO.Response(
                ap.getId(),
                f != null ? f.getId() : null,
                f != null ? f.getNomeCompleto() : null,
                f != null ? f.getMatricula() : null,
                ap.getDataReferencia(),
                ap.getStatus(),
                ap.getMinutosPrevistos(),
                ap.getMinutosTrabalhados(),
                ap.getMinutosIntervalo(),
                ap.getMinutosAtraso(),
                ap.getMinutosSaidaAntecipada(),
                ap.getMinutosAdicionalNoturno(),
                ap.getMinutosFalta(),
                ap.getMinutosHoraExtraTotal(),
                pares,
                extrasPorFaixa,
                ap.getCalculadoEm()
        );
    }
}
