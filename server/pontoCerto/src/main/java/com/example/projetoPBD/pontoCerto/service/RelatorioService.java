package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusInconsistencia;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ApuracaoDiariaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BancoHorasDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RelatorioDTO;
import com.example.projetoPBD.pontoCerto.repository.ApuracaoDiariaRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.InconsistenciaRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    private static final DateTimeFormatter DATA_BR_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_BR_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FuncionarioRepository funcionarioRepository;
    private final ApuracaoDiariaRepository apuracaoDiariaRepository;
    private final ApuracaoDiariaService apuracaoDiariaService;
    private final BancoHorasService bancoHorasService;
    private final InconsistenciaRepository inconsistenciaRepository;

    public RelatorioService(
            FuncionarioRepository funcionarioRepository,
            ApuracaoDiariaRepository apuracaoDiariaRepository,
            ApuracaoDiariaService apuracaoDiariaService,
            BancoHorasService bancoHorasService,
            InconsistenciaRepository inconsistenciaRepository
    ) {
        this.funcionarioRepository = funcionarioRepository;
        this.apuracaoDiariaRepository = apuracaoDiariaRepository;
        this.apuracaoDiariaService = apuracaoDiariaService;
        this.bancoHorasService = bancoHorasService;
        this.inconsistenciaRepository = inconsistenciaRepository;
    }

    /**
     * Relatório 1: Horas Extras por período, com filtro por local de trabalho e por equipe/gestor.
     */
    @Transactional(readOnly = true)
    public RelatorioDTO.HorasExtrasRelatorioResponse gerarRelatorioHorasExtras(
            LocalDate dataInicio,
            LocalDate dataFim,
            String competencia,
            UUID localTrabalhoId,
            UUID gestorIdFiltro
    ) {
        Funcionario logado = obterFuncionarioLogado();
        UUID gestorEfetivo = resolverGestorEfetivo(logado, gestorIdFiltro);
        LocalDate[] intervalo = resolverIntervalo(dataInicio, dataFim, competencia);
        LocalDate ini = intervalo[0];
        LocalDate fim = intervalo[1];

        List<Funcionario> colaboradores = filtrarColaboradoresEscopo(logado, localTrabalhoId, gestorEfetivo);
        Map<UUID, List<ApuracaoDiaria>> apuracoesPorFunc = carregarApuracoesAgrupadas(
                logado.getEmpresa().getId(), ini, fim, colaboradores
        );

        int totalExtrasGeral = 0;
        Map<String, Integer> extrasPorFaixaGeral = new TreeMap<>();
        List<RelatorioDTO.HorasExtrasColaboradorItem> itens = new ArrayList<>();

        for (Funcionario f : colaboradores) {
            List<ApuracaoDiaria> aps = apuracoesPorFunc.getOrDefault(f.getId(), List.of());
            int diasComExtra = 0;
            int totalFunc = 0;
            Map<String, Integer> faixasFunc = new TreeMap<>();

            for (ApuracaoDiaria ap : aps) {
                int extraDia = ap.getMinutosHoraExtraTotal() != null ? ap.getMinutosHoraExtraTotal() : 0;
                if (extraDia > 0) {
                    diasComExtra++;
                    totalFunc += extraDia;
                }
                ApuracaoDiariaDTO.Response dto = apuracaoDiariaService.toDTO(ap);
                if (dto.horasExtrasPorFaixa() != null) {
                    dto.horasExtrasPorFaixa().forEach((faixa, mins) -> {
                        int val = mins != null ? mins : 0;
                        faixasFunc.merge(faixa, val, Integer::sum);
                        extrasPorFaixaGeral.merge(faixa, val, Integer::sum);
                    });
                }
            }

            totalExtrasGeral += totalFunc;
            itens.add(new RelatorioDTO.HorasExtrasColaboradorItem(
                    f.getId(),
                    f.getNomeCompleto(),
                    f.getMatricula(),
                    f.getCargo(),
                    f.getLocalDeTrabalho() != null ? f.getLocalDeTrabalho().getId() : null,
                    f.getLocalDeTrabalho() != null ? f.getLocalDeTrabalho().getNome() : null,
                    f.getGestor() != null ? f.getGestor().getId() : null,
                    f.getGestor() != null ? f.getGestor().getNomeCompleto() : null,
                    diasComExtra,
                    totalFunc,
                    faixasFunc
            ));
        }

        return new RelatorioDTO.HorasExtrasRelatorioResponse(
                ini, fim, localTrabalhoId, gestorEfetivo, totalExtrasGeral, extrasPorFaixaGeral, itens
        );
    }

    @Transactional(readOnly = true)
    public String exportarHorasExtrasCsv(
            LocalDate dataInicio,
            LocalDate dataFim,
            String competencia,
            UUID localTrabalhoId,
            UUID gestorIdFiltro
    ) {
        RelatorioDTO.HorasExtrasRelatorioResponse rel = gerarRelatorioHorasExtras(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorIdFiltro
        );
        StringBuilder sb = new StringBuilder();
        sb.append("Colaborador;Matricula;Cargo;LocalTrabalho;Gestor;DiasComExtra;TotalExtrasMinutos;TotalExtrasHHMM;ExtrasPorFaixa\n");
        for (RelatorioDTO.HorasExtrasColaboradorItem item : rel.itens()) {
            String faixasStr = item.extrasPorFaixa().entrySet().stream()
                    .map(e -> e.getKey() + "=" + formatarMinutos(e.getValue()))
                    .collect(Collectors.joining(" | "));
            sb.append(csv(item.nomeCompleto())).append(';')
                    .append(csv(item.matricula())).append(';')
                    .append(csv(item.cargo())).append(';')
                    .append(csv(item.localTrabalhoNome())).append(';')
                    .append(csv(item.gestorNome())).append(';')
                    .append(item.diasComExtra()).append(';')
                    .append(item.totalMinutosExtras()).append(';')
                    .append(formatarMinutos(item.totalMinutosExtras())).append(';')
                    .append(csv(faixasStr)).append('\n');
        }
        return sb.toString();
    }

    /**
     * Relatório 2: Absenteísmo (faltas, atrasos, saídas antecipadas e abonos por colaborador).
     */
    @Transactional(readOnly = true)
    public RelatorioDTO.AbsenteismoRelatorioResponse gerarRelatorioAbsenteismo(
            LocalDate dataInicio,
            LocalDate dataFim,
            String competencia,
            UUID localTrabalhoId,
            UUID gestorIdFiltro
    ) {
        Funcionario logado = obterFuncionarioLogado();
        UUID gestorEfetivo = resolverGestorEfetivo(logado, gestorIdFiltro);
        LocalDate[] intervalo = resolverIntervalo(dataInicio, dataFim, competencia);
        LocalDate ini = intervalo[0];
        LocalDate fim = intervalo[1];

        List<Funcionario> colaboradores = filtrarColaboradoresEscopo(logado, localTrabalhoId, gestorEfetivo);
        Map<UUID, List<ApuracaoDiaria>> apuracoesPorFunc = carregarApuracoesAgrupadas(
                logado.getEmpresa().getId(), ini, fim, colaboradores
        );

        int totalFaltaGeral = 0;
        int totalAtrasoGeral = 0;
        int totalSaidaAntGeral = 0;
        int totalAbonadoGeral = 0;
        List<RelatorioDTO.AbsenteismoColaboradorItem> itens = new ArrayList<>();

        for (Funcionario f : colaboradores) {
            List<ApuracaoDiaria> aps = apuracoesPorFunc.getOrDefault(f.getId(), List.of());
            int diasComFalta = 0;
            int diasComAtraso = 0;
            int minPrevistos = 0;
            int minFalta = 0;
            int minAtraso = 0;
            int minSaidaAnt = 0;
            int minAbonado = 0;

            for (ApuracaoDiaria ap : aps) {
                int p = ap.getMinutosPrevistos() != null ? ap.getMinutosPrevistos() : 0;
                int flt = ap.getMinutosFalta() != null ? ap.getMinutosFalta() : 0;
                int atr = ap.getMinutosAtraso() != null ? ap.getMinutosAtraso() : 0;
                int sai = ap.getMinutosSaidaAntecipada() != null ? ap.getMinutosSaidaAntecipada() : 0;
                int ab = ap.getMinutosAbonados() != null ? ap.getMinutosAbonados() : 0;

                if (flt > 0) diasComFalta++;
                if (atr > 0) diasComAtraso++;

                minPrevistos += p;
                minFalta += flt;
                minAtraso += atr;
                minSaidaAnt += sai;
                minAbonado += ab;
            }

            double pctAbsenteismo = 0.0;
            if (minPrevistos > 0) {
                double bruto = ((minFalta + minAtraso + minSaidaAnt) * 100.0) / minPrevistos;
                pctAbsenteismo = Math.round(bruto * 100.0) / 100.0;
            }

            totalFaltaGeral += minFalta;
            totalAtrasoGeral += minAtraso;
            totalSaidaAntGeral += minSaidaAnt;
            totalAbonadoGeral += minAbonado;

            itens.add(new RelatorioDTO.AbsenteismoColaboradorItem(
                    f.getId(),
                    f.getNomeCompleto(),
                    f.getMatricula(),
                    f.getCargo(),
                    f.getLocalDeTrabalho() != null ? f.getLocalDeTrabalho().getId() : null,
                    f.getLocalDeTrabalho() != null ? f.getLocalDeTrabalho().getNome() : null,
                    f.getGestor() != null ? f.getGestor().getId() : null,
                    f.getGestor() != null ? f.getGestor().getNomeCompleto() : null,
                    diasComFalta,
                    diasComAtraso,
                    minPrevistos,
                    minFalta,
                    minAtraso,
                    minSaidaAnt,
                    minAbonado,
                    pctAbsenteismo
            ));
        }

        return new RelatorioDTO.AbsenteismoRelatorioResponse(
                ini, fim, localTrabalhoId, gestorEfetivo,
                totalFaltaGeral, totalAtrasoGeral, totalSaidaAntGeral, totalAbonadoGeral, itens
        );
    }

    @Transactional(readOnly = true)
    public String exportarAbsenteismoCsv(
            LocalDate dataInicio,
            LocalDate dataFim,
            String competencia,
            UUID localTrabalhoId,
            UUID gestorIdFiltro
    ) {
        RelatorioDTO.AbsenteismoRelatorioResponse rel = gerarRelatorioAbsenteismo(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorIdFiltro
        );
        StringBuilder sb = new StringBuilder();
        sb.append("Colaborador;Matricula;LocalTrabalho;Gestor;DiasComFalta;DiasComAtraso;FaltasHHMM;AtrasosHHMM;SaidaAntecipadaHHMM;AbonadosHHMM;PercentualAbsenteismo\n");
        for (RelatorioDTO.AbsenteismoColaboradorItem item : rel.itens()) {
            sb.append(csv(item.nomeCompleto())).append(';')
                    .append(csv(item.matricula())).append(';')
                    .append(csv(item.localTrabalhoNome())).append(';')
                    .append(csv(item.gestorNome())).append(';')
                    .append(item.diasComFalta()).append(';')
                    .append(item.diasComAtraso()).append(';')
                    .append(formatarMinutos(item.totalMinutosFalta())).append(';')
                    .append(formatarMinutos(item.totalMinutosAtraso())).append(';')
                    .append(formatarMinutos(item.totalMinutosSaidaAntecipada())).append(';')
                    .append(formatarMinutos(item.totalMinutosAbonados())).append(';')
                    .append(String.format(Locale.US, "%.2f%%", item.percentualAbsenteismo())).append('\n');
        }
        return sb.toString();
    }

    /**
     * Relatório 3: Banco de Horas com saldo inicial, créditos, débitos, compensações e saldo final por colaborador.
     */
    @Transactional(readOnly = true)
    public RelatorioDTO.BancoHorasRelatorioResponse gerarRelatorioBancoHoras(
            LocalDate dataInicio,
            LocalDate dataFim,
            String competencia,
            UUID localTrabalhoId,
            UUID gestorIdFiltro
    ) {
        Funcionario logado = obterFuncionarioLogado();
        UUID gestorEfetivo = resolverGestorEfetivo(logado, gestorIdFiltro);
        LocalDate[] intervalo = resolverIntervalo(dataInicio, dataFim, competencia);
        LocalDate ini = intervalo[0];
        LocalDate fim = intervalo[1];

        List<Funcionario> colaboradores = filtrarColaboradoresEscopo(logado, localTrabalhoId, gestorEfetivo);
        int saldoGeral = 0;
        List<RelatorioDTO.BancoHorasColaboradorItem> itens = new ArrayList<>();

        for (Funcionario f : colaboradores) {
            BancoHorasDTO.ExtratoPeriodoResponse ext = bancoHorasService.consultarExtrato(f.getId(), ini, fim);
            saldoGeral += ext.saldoFinalMinutos();
            itens.add(new RelatorioDTO.BancoHorasColaboradorItem(
                    f.getId(),
                    f.getNomeCompleto(),
                    f.getMatricula(),
                    f.getCargo(),
                    f.getLocalDeTrabalho() != null ? f.getLocalDeTrabalho().getId() : null,
                    f.getLocalDeTrabalho() != null ? f.getLocalDeTrabalho().getNome() : null,
                    f.getGestor() != null ? f.getGestor().getId() : null,
                    f.getGestor() != null ? f.getGestor().getNomeCompleto() : null,
                    ext.saldoInicialMinutos(),
                    ext.creditosPeriodoMinutos(),
                    ext.debitosPeriodoMinutos(),
                    ext.compensacoesPeriodoMinutos(),
                    ext.saldoFinalMinutos()
            ));
        }

        return new RelatorioDTO.BancoHorasRelatorioResponse(
                ini, fim, localTrabalhoId, gestorEfetivo, saldoGeral, itens
        );
    }

    @Transactional(readOnly = true)
    public String exportarBancoHorasCsv(
            LocalDate dataInicio,
            LocalDate dataFim,
            String competencia,
            UUID localTrabalhoId,
            UUID gestorIdFiltro
    ) {
        RelatorioDTO.BancoHorasRelatorioResponse rel = gerarRelatorioBancoHoras(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorIdFiltro
        );
        StringBuilder sb = new StringBuilder();
        sb.append("Colaborador;Matricula;LocalTrabalho;Gestor;SaldoInicialHHMM;CreditosHHMM;DebitosHHMM;CompensacoesHHMM;SaldoFinalHHMM;SaldoFinalMinutos\n");
        for (RelatorioDTO.BancoHorasColaboradorItem item : rel.itens()) {
            sb.append(csv(item.nomeCompleto())).append(';')
                    .append(csv(item.matricula())).append(';')
                    .append(csv(item.localTrabalhoNome())).append(';')
                    .append(csv(item.gestorNome())).append(';')
                    .append(formatarMinutos(item.saldoInicialMinutos())).append(';')
                    .append(formatarMinutos(item.creditosPeriodoMinutos())).append(';')
                    .append(formatarMinutos(item.debitosPeriodoMinutos())).append(';')
                    .append(formatarMinutos(item.compensacoesPeriodoMinutos())).append(';')
                    .append(formatarMinutos(item.saldoFinalMinutos())).append(';')
                    .append(item.saldoFinalMinutos()).append('\n');
        }
        return sb.toString();
    }

    /**
     * Relatório 4: Pendências (inconsistências) separando as tratadas (RESOLVIDA) das que continuam abertas (ABERTA).
     */
    @Transactional(readOnly = true)
    public RelatorioDTO.PendenciasRelatorioResponse gerarRelatorioPendencias(
            LocalDate dataInicio,
            LocalDate dataFim,
            String competencia,
            UUID localTrabalhoId,
            UUID gestorIdFiltro
    ) {
        Funcionario logado = obterFuncionarioLogado();
        UUID gestorEfetivo = resolverGestorEfetivo(logado, gestorIdFiltro);
        LocalDate[] intervalo = resolverIntervalo(dataInicio, dataFim, competencia);
        LocalDate ini = intervalo[0];
        LocalDate fim = intervalo[1];

        List<Funcionario> colaboradores = filtrarColaboradoresEscopo(logado, localTrabalhoId, gestorEfetivo);
        Map<UUID, Funcionario> mapaColab = colaboradores.stream()
                .collect(Collectors.toMap(Funcionario::getId, f -> f));

        List<Inconsistencia> todasEmpresa = inconsistenciaRepository
                .findByEmpresaIdOrderByDataHoraOrigemDesc(logado.getEmpresa().getId());

        List<RelatorioDTO.PendenciaItem> abertas = new ArrayList<>();
        List<RelatorioDTO.PendenciaItem> tratadas = new ArrayList<>();

        for (Inconsistencia inc : todasEmpresa) {
            if (inc.getDataReferencia() == null
                    || inc.getDataReferencia().isBefore(ini)
                    || inc.getDataReferencia().isAfter(fim)) {
                continue;
            }

            // Inconsistências sem colaborador vinculado (ex: órfã/equipamento desconhecido) só aparecem para RH sem filtro de equipe/local
            Funcionario f = inc.getFuncionario();
            if (f != null) {
                if (!mapaColab.containsKey(f.getId())) {
                    continue;
                }
            } else {
                if (logado.getPerfilAcesso() == PerfilAcesso.GESTOR || gestorEfetivo != null || localTrabalhoId != null) {
                    continue;
                }
            }

            RelatorioDTO.PendenciaItem item = new RelatorioDTO.PendenciaItem(
                    inc.getId(),
                    inc.getTipo(),
                    inc.getSituacao(),
                    inc.getDataReferencia(),
                    inc.getDataHoraOrigem(),
                    f != null ? f.getId() : null,
                    f != null ? f.getNomeCompleto() : "Não vinculado",
                    f != null ? f.getMatricula() : "-",
                    f != null && f.getLocalDeTrabalho() != null ? f.getLocalDeTrabalho().getNome() : null,
                    f != null && f.getGestor() != null ? f.getGestor().getNomeCompleto() : null,
                    inc.getDescricaoOrigem(),
                    inc.getAcaoTomada(),
                    inc.getTratadoPor(),
                    inc.getTratadoEm(),
                    inc.getMotivoTratamento()
            );

            if (inc.getSituacao() == StatusInconsistencia.RESOLVIDA) {
                tratadas.add(item);
            } else {
                abertas.add(item);
            }
        }

        return new RelatorioDTO.PendenciasRelatorioResponse(
                ini, fim, localTrabalhoId, gestorEfetivo, abertas.size(), tratadas.size(), abertas, tratadas
        );
    }

    @Transactional(readOnly = true)
    public String exportarPendenciasCsv(
            LocalDate dataInicio,
            LocalDate dataFim,
            String competencia,
            UUID localTrabalhoId,
            UUID gestorIdFiltro
    ) {
        RelatorioDTO.PendenciasRelatorioResponse rel = gerarRelatorioPendencias(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorIdFiltro
        );
        StringBuilder sb = new StringBuilder();
        sb.append("Situacao;Tipo;DataReferencia;Colaborador;Matricula;LocalTrabalho;Gestor;Descricao;AcaoTomada;TratadoPor;TratadoEm;MotivoTratamento\n");
        List<RelatorioDTO.PendenciaItem> todas = new ArrayList<>(rel.abertas().size() + rel.tratadas().size());
        todas.addAll(rel.abertas());
        todas.addAll(rel.tratadas());

        for (RelatorioDTO.PendenciaItem item : todas) {
            sb.append(item.situacao() != null ? item.situacao().name() : "").append(';')
                    .append(item.tipo() != null ? item.tipo().name() : "").append(';')
                    .append(item.dataReferencia() != null ? item.dataReferencia().format(DATA_BR_FMT) : "").append(';')
                    .append(csv(item.nomeColaborador())).append(';')
                    .append(csv(item.matricula())).append(';')
                    .append(csv(item.localTrabalhoNome())).append(';')
                    .append(csv(item.gestorNome())).append(';')
                    .append(csv(item.descricaoOrigem())).append(';')
                    .append(item.acaoTomada() != null ? item.acaoTomada().name() : "").append(';')
                    .append(csv(item.tratadoPor())).append(';')
                    .append(item.tratadoEm() != null ? item.tratadoEm().format(DATA_HORA_BR_FMT) : "").append(';')
                    .append(csv(item.motivoTratamento())).append('\n');
        }
        return sb.toString();
    }

    protected Funcionario obterFuncionarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails fud) {
            return funcionarioRepository.findById(fud.getFuncionario().getId())
                    .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário autenticado não encontrado."));
        }
        throw new AcessoNegadoException("Usuário não autenticado.");
    }

    protected UUID resolverGestorEfetivo(Funcionario logado, UUID gestorIdFiltro) {
        if (logado.getPerfilAcesso() == PerfilAcesso.COLABORADOR) {
            throw new AcessoNegadoException("Relatórios gerenciais são restritos a perfis de Gestor e RH.");
        }

        if (logado.getPerfilAcesso() == PerfilAcesso.GESTOR) {
            if (gestorIdFiltro != null && !gestorIdFiltro.equals(logado.getId())) {
                throw new AcessoNegadoException("Gestor não tem permissão para consultar relatórios de outra equipe.");
            }
            return logado.getId();
        }

        return gestorIdFiltro;
    }

    protected List<Funcionario> filtrarColaboradoresEscopo(
            Funcionario logado,
            UUID localTrabalhoId,
            UUID gestorEfetivo
    ) {
        UUID empresaId = logado.getEmpresa().getId();
        List<Funcionario> base;

        if (logado.getPerfilAcesso() == PerfilAcesso.GESTOR) {
            base = funcionarioRepository.findByGestorIdOrderByNomeCompletoAsc(logado.getId())
                    .stream()
                    .filter(f -> f.getEmpresa().getId().equals(empresaId))
                    .toList();
        } else {
            base = funcionarioRepository.findByEmpresaIdOrderByNomeCompletoAsc(empresaId);
            if (gestorEfetivo != null) {
                base = base.stream()
                        .filter(f -> f.getGestor() != null && f.getGestor().getId().equals(gestorEfetivo))
                        .toList();
            }
        }

        if (localTrabalhoId != null) {
            base = base.stream()
                    .filter(f -> f.getLocalDeTrabalho() != null && f.getLocalDeTrabalho().getId().equals(localTrabalhoId))
                    .toList();
        }

        return base;
    }

    protected Map<UUID, List<ApuracaoDiaria>> carregarApuracoesAgrupadas(
            UUID empresaId,
            LocalDate ini,
            LocalDate fim,
            List<Funcionario> colaboradores
    ) {
        Set<UUID> idsPermitidos = colaboradores.stream().map(Funcionario::getId).collect(Collectors.toSet());
        if (idsPermitidos.isEmpty()) {
            return Map.of();
        }
        return apuracaoDiariaRepository
                .findByEmpresaIdAndDataReferenciaBetweenOrderByDataReferenciaAsc(empresaId, ini, fim)
                .stream()
                .filter(ap -> ap.getFuncionario() != null && idsPermitidos.contains(ap.getFuncionario().getId()))
                .collect(Collectors.groupingBy(ap -> ap.getFuncionario().getId()));
    }

    protected LocalDate[] resolverIntervalo(LocalDate inicio, LocalDate fim, String competencia) {
        if (inicio != null && fim != null) {
            if (fim.isBefore(inicio)) {
                throw new IllegalArgumentException("A data fim não pode ser anterior à data início.");
            }
            return new LocalDate[]{inicio, fim};
        }
        if (competencia != null && !competencia.isBlank()) {
            try {
                YearMonth ym = YearMonth.parse(competencia.trim());
                return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Competência inválida (" + competencia + "). Utilize YYYY-MM.");
            }
        }
        YearMonth atual = YearMonth.now();
        return new LocalDate[]{atual.atDay(1), atual.atEndOfMonth()};
    }

    protected String formatarMinutos(int minutos) {
        int abs = Math.abs(minutos);
        String sinal = minutos < 0 ? "-" : "";
        return String.format("%s%02d:%02d", sinal, abs / 60, abs % 60);
    }

    protected String csv(String valor) {
        if (valor == null) return "";
        String limpo = valor.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ");
        if (limpo.contains(";") || limpo.contains("\"")) {
            return "\"" + limpo + "\"";
        }
        return limpo;
    }
}
