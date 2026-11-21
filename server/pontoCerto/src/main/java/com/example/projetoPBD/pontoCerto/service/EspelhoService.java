package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.DiaSemana;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem;
import com.example.projetoPBD.pontoCerto.dto.HorarioPrevistoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ApuracaoDiariaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EspelhoPontoDTO;
import com.example.projetoPBD.pontoCerto.repository.*;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EspelhoService {

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA_BR_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_BR_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FuncionarioRepository funcionarioRepository;
    private final BatidaRepository batidaRepository;
    private final ApuracaoDiariaRepository apuracaoDiariaRepository;
    private final ApuracaoDiariaService apuracaoDiariaService;
    private final RegimeTrabalhoService regimeTrabalhoService;
    private final OcorrenciaColaboradorRepository ocorrenciaRepository;
    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;
    private final FeriadoRepository feriadoRepository;

    public EspelhoService(
            FuncionarioRepository funcionarioRepository,
            BatidaRepository batidaRepository,
            ApuracaoDiariaRepository apuracaoDiariaRepository,
            ApuracaoDiariaService apuracaoDiariaService,
            RegimeTrabalhoService regimeTrabalhoService,
            OcorrenciaColaboradorRepository ocorrenciaRepository,
            FechamentoPeriodoRepository fechamentoPeriodoRepository,
            FeriadoRepository feriadoRepository
    ) {
        this.funcionarioRepository = funcionarioRepository;
        this.batidaRepository = batidaRepository;
        this.apuracaoDiariaRepository = apuracaoDiariaRepository;
        this.apuracaoDiariaService = apuracaoDiariaService;
        this.regimeTrabalhoService = regimeTrabalhoService;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
        this.feriadoRepository = feriadoRepository;
    }

    /**
     * Consulta o espelho de ponto mensal (dia a dia) do colaborador na competência informada,
     * aplicando o controle de acesso por perfil (Colaborador, Gestor e RH).
     */
    @Transactional(readOnly = true)
    public EspelhoPontoDTO.EspelhoMensalResponse consultarEspelhoMensal(UUID funcionarioIdAlvo, String competenciaStr) {
        Funcionario logado = obterFuncionarioLogado();
        UUID alvoId = funcionarioIdAlvo != null ? funcionarioIdAlvo : logado.getId();

        Funcionario alvo = funcionarioRepository.findById(alvoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));

        validarPermissaoAcesso(logado, alvo);

        YearMonth ym = parseCompetencia(competenciaStr);
        LocalDate dataInicio = ym.atDay(1);
        LocalDate dataFim = ym.atEndOfMonth();
        UUID empresaId = alvo.getEmpresa().getId();

        // Consultas em lote para evitar N+1 ao longo dos dias do mês
        Map<LocalDate, List<Batida>> batidasPorDia = batidaRepository
                .findAtivasByFuncionarioIdAndDataHoraBetweenOrderByDataHoraAsc(
                        alvoId,
                        dataInicio.atStartOfDay(),
                        dataFim.atTime(LocalTime.MAX)
                )
                .stream()
                .collect(Collectors.groupingBy(b -> b.getDataHora().toLocalDate()));

        Map<LocalDate, ApuracaoDiaria> apuracoesPorDia = apuracaoDiariaRepository
                .findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAsc(alvoId, dataInicio, dataFim)
                .stream()
                .collect(Collectors.toMap(ApuracaoDiaria::getDataReferencia, a -> a, (a1, a2) -> a1));

        List<OcorrenciaColaborador> ocorrenciasMes = ocorrenciaRepository.findFiltradas(empresaId, alvoId, dataInicio, dataFim);

        Map<LocalDate, Feriado> feriadosPorDia = carregarFeriadosDoAno(alvo, ym.getYear());

        EspelhoPontoDTO.FechamentoInfo fechamentoInfo = montarInfoFechamento(empresaId, ym.toString());

        List<EspelhoPontoDTO.DiaEspelhoResponse> dias = new ArrayList<>();
        int totalDiasTrabalhados = 0;
        int totalPrevisto = 0;
        int totalTrabalhado = 0;
        int totalIntervalo = 0;
        int totalAtraso = 0;
        int totalSaidaAntecipada = 0;
        int totalNoturno = 0;
        int totalFalta = 0;
        int totalHoraExtra = 0;
        int totalAbonado = 0;
        Map<String, Integer> extrasPorFaixaTotal = new TreeMap<>();

        for (LocalDate data = dataInicio; !data.isAfter(dataFim); data = data.plusDays(1)) {
            HorarioPrevistoDTO.Response hp = regimeTrabalhoService.obterHorarioPrevisto(alvoId, data);
            Feriado feriado = feriadosPorDia.get(data);

            EspelhoPontoDTO.HorarioPrevistoDia previstoDia = new EspelhoPontoDTO.HorarioPrevistoDia(
                    hp.diaTrabalho(),
                    hp.descricaoRegime(),
                    hp.horaEntrada(),
                    hp.intervaloInicio(),
                    hp.intervaloFim(),
                    hp.horaSaida(),
                    hp.cargaDiariaMinutos() != null ? hp.cargaDiariaMinutos() : 0,
                    hp.mensagem()
            );

            List<EspelhoPontoDTO.MarcacaoItem> marcacoesDia = batidasPorDia
                    .getOrDefault(data, List.of())
                    .stream()
                    .map(this::toMarcacaoItem)
                    .toList();

            ApuracaoDiaria ap = apuracoesPorDia.get(data);
            ApuracaoDiariaDTO.Response apDto = ap != null ? apuracaoDiariaService.toDTO(ap) : null;

            OcorrenciaColaborador ocorrenciaDia = buscarOcorrenciaNaData(ocorrenciasMes, data);

            int minPrevisto = apDto != null && apDto.minutosPrevistos() != null
                    ? apDto.minutosPrevistos()
                    : (hp.diaTrabalho() && feriado == null ? previstoDia.cargaPrevistaMinutos() : 0);
            int minTrabalhado = apDto != null && apDto.minutosTrabalhados() != null ? apDto.minutosTrabalhados() : 0;
            int minIntervalo = apDto != null && apDto.minutosIntervalo() != null ? apDto.minutosIntervalo() : 0;
            int minAtraso = apDto != null && apDto.minutosAtraso() != null ? apDto.minutosAtraso() : 0;
            int minSaidaAnt = apDto != null && apDto.minutosSaidaAntecipada() != null ? apDto.minutosSaidaAntecipada() : 0;
            int minNoturno = apDto != null && apDto.minutosAdicionalNoturno() != null ? apDto.minutosAdicionalNoturno() : 0;
            int minFalta = apDto != null && apDto.minutosFalta() != null ? apDto.minutosFalta() : 0;
            int minExtra = apDto != null && apDto.minutosHoraExtraTotal() != null ? apDto.minutosHoraExtraTotal() : 0;
            int minAbonado = apDto != null && apDto.minutosAbonados() != null ? apDto.minutosAbonados() : 0;
            Map<String, Integer> extrasDia = apDto != null && apDto.horasExtrasPorFaixa() != null
                    ? apDto.horasExtrasPorFaixa()
                    : Map.of();

            UUID ocorrenciaId = apDto != null && apDto.ocorrenciaId() != null
                    ? apDto.ocorrenciaId()
                    : (ocorrenciaDia != null ? ocorrenciaDia.getId() : null);
            String tipoOcorrenciaNome = apDto != null && apDto.tipoOcorrenciaNome() != null
                    ? apDto.tipoOcorrenciaNome()
                    : (ocorrenciaDia != null && ocorrenciaDia.getTipoOcorrencia() != null
                    ? ocorrenciaDia.getTipoOcorrencia().getNome()
                    : null);

            if (minTrabalhado > 0 || !marcacoesDia.isEmpty()) {
                totalDiasTrabalhados++;
            }

            totalPrevisto += minPrevisto;
            totalTrabalhado += minTrabalhado;
            totalIntervalo += minIntervalo;
            totalAtraso += minAtraso;
            totalSaidaAntecipada += minSaidaAnt;
            totalNoturno += minNoturno;
            totalFalta += minFalta;
            totalHoraExtra += minExtra;
            totalAbonado += minAbonado;

            extrasDia.forEach((faixa, mins) ->
                    extrasPorFaixaTotal.merge(faixa, mins != null ? mins : 0, Integer::sum)
            );

            dias.add(new EspelhoPontoDTO.DiaEspelhoResponse(
                    data,
                    DiaSemana.from(data.getDayOfWeek()),
                    feriado != null,
                    feriado != null ? feriado.getDescricao() : null,
                    previstoDia,
                    marcacoesDia,
                    apDto != null ? apDto.pares() : List.of(),
                    apDto != null ? apDto.status() : null,
                    minPrevisto,
                    minTrabalhado,
                    minIntervalo,
                    minAtraso,
                    minSaidaAnt,
                    minNoturno,
                    minFalta,
                    minExtra,
                    extrasDia,
                    minAbonado,
                    ocorrenciaId,
                    tipoOcorrenciaNome
            ));
        }

        Empresa empresa = alvo.getEmpresa();
        EspelhoPontoDTO.CabecalhoEmpresa cabEmpresa = new EspelhoPontoDTO.CabecalhoEmpresa(
                empresa.getId(),
                empresa.getRazaoSocial(),
                empresa.getCnpj(),
                empresa.getEndereco()
        );

        EspelhoPontoDTO.CabecalhoColaborador cabColab = new EspelhoPontoDTO.CabecalhoColaborador(
                alvo.getId(),
                alvo.getNomeCompleto(),
                alvo.getMatricula(),
                alvo.getCpf(),
                alvo.getPisPasep(),
                alvo.getCargo(),
                alvo.getLocalDeTrabalho() != null ? alvo.getLocalDeTrabalho().getNome() : null
        );

        EspelhoPontoDTO.TotaisPeriodo totais = new EspelhoPontoDTO.TotaisPeriodo(
                dias.size(),
                totalDiasTrabalhados,
                totalPrevisto,
                totalTrabalhado,
                totalIntervalo,
                totalAtraso,
                totalSaidaAntecipada,
                totalNoturno,
                totalFalta,
                totalHoraExtra,
                totalAbonado,
                extrasPorFaixaTotal
        );

        return new EspelhoPontoDTO.EspelhoMensalResponse(
                ym.toString(),
                dataInicio,
                dataFim,
                cabEmpresa,
                cabColab,
                fechamentoInfo,
                totais,
                dias
        );
    }

    /**
     * Gera versão HTML A4 otimizada para impressão legível em navegador/PDF.
     */
    @Transactional(readOnly = true)
    public String gerarHtmlImpressao(UUID funcionarioIdAlvo, String competenciaStr) {
        EspelhoPontoDTO.EspelhoMensalResponse espelho = consultarEspelhoMensal(funcionarioIdAlvo, competenciaStr);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\">");
        sb.append("<title>Espelho de Ponto - ").append(escapeHtml(espelho.colaborador().nomeCompleto()))
                .append(" (").append(espelho.competencia()).append(")</title>");
        sb.append("<style>");
        sb.append("@page { size: A4 landscape; margin: 10mm; }");
        sb.append("body { font-family: Arial, Helvetica, sans-serif; font-size: 11px; color: #111827; margin: 0; padding: 12px; }");
        sb.append("h1 { font-size: 16px; margin: 0 0 4px 0; }");
        sb.append(".header-box { border: 1px solid #d1d5db; padding: 10px; margin-bottom: 10px; border-radius: 4px; }");
        sb.append(".grid { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; }");
        sb.append(".badge-fechado { display: inline-block; background: #fee2e2; color: #991b1b; border: 1px solid #f87171; padding: 3px 8px; border-radius: 4px; font-weight: bold; }");
        sb.append(".badge-aberto { display: inline-block; background: #dcfce7; color: #166534; border: 1px solid #86efac; padding: 3px 8px; border-radius: 4px; font-weight: bold; }");
        sb.append("table { width: 100%; border-collapse: collapse; margin-top: 8px; }");
        sb.append("th, td { border: 1px solid #d1d5db; padding: 4px 6px; text-align: left; vertical-align: top; }");
        sb.append("th { background: #f3f4f6; font-weight: bold; }");
        sb.append("tr.folga { background: #f9fafb; color: #6b7280; }");
        sb.append(".totals { margin-top: 12px; border: 1px solid #d1d5db; padding: 8px; background: #f9fafb; }");
        sb.append(".signatures { margin-top: 28px; display: flex; justify-content: space-around; }");
        sb.append(".sig-line { width: 260px; border-top: 1px solid #111827; text-align: center; padding-top: 4px; }");
        sb.append("@media print { body { padding: 0; } .no-print { display: none; } }");
        sb.append("</style></head><body>");

        sb.append("<div class=\"header-box\"><div class=\"grid\">");
        sb.append("<div><h1>Espelho de Ponto Eletrônico</h1>");
        sb.append("<div><strong>Empresa:</strong> ").append(escapeHtml(espelho.empresa().razaoSocial()))
                .append(" | <strong>CNPJ:</strong> ").append(escapeHtml(espelho.empresa().cnpj())).append("</div>");
        sb.append("<div><strong>Colaborador:</strong> ").append(escapeHtml(espelho.colaborador().nomeCompleto()))
                .append(" | <strong>Matrícula:</strong> ").append(escapeHtml(espelho.colaborador().matricula()))
                .append(" | <strong>PIS:</strong> ").append(escapeHtml(espelho.colaborador().pisPasep())).append("</div>");
        sb.append("<div><strong>Competência:</strong> ").append(espelho.competencia())
                .append(" (").append(espelho.dataInicio().format(DATA_BR_FMT))
                .append(" a ").append(espelho.dataFim().format(DATA_BR_FMT)).append(")</div>");
        sb.append("</div><div>");
        if (espelho.fechamento().periodoFechado()) {
            String dataFech = espelho.fechamento().dataFechamento() != null
                    ? espelho.fechamento().dataFechamento().format(DATA_HORA_BR_FMT)
                    : "-";
            sb.append("<div class=\"badge-fechado\">PERÍODO FECHADO em ").append(dataFech).append("</div>");
        } else {
            sb.append("<div class=\"badge-aberto\">PERÍODO ABERTO</div>");
        }
        sb.append("</div></div></div>");

        sb.append("<table><thead><tr>");
        sb.append("<th>Data</th><th>Dia</th><th>Horário Previsto</th><th>Marcações (Origem)</th>");
        sb.append("<th>Trab.</th><th>Atraso</th><th>Extras</th><th>Noturno</th><th>Falta</th><th>Ocorrência</th>");
        sb.append("</tr></thead><tbody>");

        for (EspelhoPontoDTO.DiaEspelhoResponse dia : espelho.dias()) {
            boolean ehFolga = !dia.previsto().diaTrabalho() || dia.feriado();
            sb.append(ehFolga ? "<tr class=\"folga\">" : "<tr>");
            sb.append("<td>").append(dia.data().format(DATA_BR_FMT)).append("</td>");
            sb.append("<td>").append(dia.diaSemana().name()).append("</td>");
            sb.append("<td>").append(formatarPrevistoHtml(dia)).append("</td>");
            sb.append("<td>").append(formatarMarcacoesHtml(dia.marcacoes())).append("</td>");
            sb.append("<td>").append(formatarMinutos(dia.minutosTrabalhados())).append("</td>");
            sb.append("<td>").append(formatarMinutos(dia.minutosAtraso())).append("</td>");
            sb.append("<td>").append(formatarExtrasDiaHtml(dia)).append("</td>");
            sb.append("<td>").append(formatarMinutos(dia.minutosAdicionalNoturno())).append("</td>");
            sb.append("<td>").append(formatarMinutos(dia.minutosFalta())).append("</td>");
            sb.append("<td>").append(dia.tipoOcorrenciaNome() != null ? escapeHtml(dia.tipoOcorrenciaNome()) : "-").append("</td>");
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");

        EspelhoPontoDTO.TotaisPeriodo t = espelho.totais();
        sb.append("<div class=\"totals\"><strong>Totais do Período:</strong> ");
        sb.append("Previsto: ").append(formatarMinutos(t.totalMinutosPrevistos())).append(" | ");
        sb.append("Trabalhado: ").append(formatarMinutos(t.totalMinutosTrabalhados())).append(" | ");
        sb.append("Atraso: ").append(formatarMinutos(t.totalMinutosAtraso())).append(" | ");
        sb.append("Faltas: ").append(formatarMinutos(t.totalMinutosFalta())).append(" | ");
        sb.append("Noturno: ").append(formatarMinutos(t.totalMinutosAdicionalNoturno())).append(" | ");
        sb.append("Extras Total: ").append(formatarMinutos(t.totalMinutosHoraExtra()));
        if (!t.horasExtrasPorFaixa().isEmpty()) {
            String faixasStr = t.horasExtrasPorFaixa().entrySet().stream()
                    .map(e -> e.getKey() + ": " + formatarMinutos(e.getValue()))
                    .collect(Collectors.joining(", "));
            sb.append(" (").append(faixasStr).append(")");
        }
        sb.append("</div>");

        sb.append("<div class=\"signatures\">");
        sb.append("<div class=\"sig-line\">").append(escapeHtml(espelho.colaborador().nomeCompleto())).append(" (Colaborador)</div>");
        sb.append("<div class=\"sig-line\">Responsável RH / Gestor</div>");
        sb.append("</div></body></html>");
        return sb.toString();
    }

    private void validarPermissaoAcesso(Funcionario logado, Funcionario alvo) {
        if (!alvo.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
            throw new AcessoNegadoException("Colaborador não pertence à sua empresa.");
        }

        if (logado.getId().equals(alvo.getId())) {
            return;
        }

        if (logado.getPerfilAcesso() == PerfilAcesso.RH) {
            return;
        }

        if (logado.getPerfilAcesso() == PerfilAcesso.GESTOR) {
            boolean ehSubordinado = alvo.getGestor() != null && alvo.getGestor().getId().equals(logado.getId());
            if (!ehSubordinado) {
                throw new AcessoNegadoException("Gestor só pode acessar o espelho de colaboradores da sua equipe.");
            }
            return;
        }

        throw new AcessoNegadoException("Colaborador só pode acessar o próprio espelho de ponto.");
    }

    private EspelhoPontoDTO.MarcacaoItem toMarcacaoItem(Batida b) {
        return new EspelhoPontoDTO.MarcacaoItem(
                b.getId(),
                b.getNsr(),
                b.getDataHora(),
                b.getDataHora().toLocalTime().withSecond(0).withNano(0),
                b.getTipoRegistro(),
                b.getTipoOrigem(),
                traduzirOrigem(b.getTipoOrigem()),
                b.getEquipamento() != null ? b.getEquipamento().getIdentificacao() : null,
                b.getMotivoAjuste(),
                b.getAutorAjuste()
        );
    }

    private String traduzirOrigem(TipoOrigem origem) {
        if (origem == null) {
            return "Não informada";
        }
        return switch (origem) {
            case ARQUIVO_IMPORTADO -> "Relógio";
            case ESTACAO -> "Estação";
            case AJUSTE_MANUAL -> "Ajuste";
            case APLICATIVO_WEB -> "Aplicativo Web";
        };
    }

    private EspelhoPontoDTO.FechamentoInfo montarInfoFechamento(UUID empresaId, String competencia) {
        Optional<FechamentoPeriodo> fechOpt = fechamentoPeriodoRepository
                .findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, competencia);

        if (fechOpt.isEmpty()) {
            return new EspelhoPontoDTO.FechamentoInfo(
                    StatusFechamentoPeriodo.ABERTO,
                    false,
                    null,
                    null,
                    null
            );
        }

        FechamentoPeriodo fp = fechOpt.get();
        boolean fechado = fp.getStatus() == StatusFechamentoPeriodo.CONCLUIDO
                || fp.getStatus() == StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS;

        return new EspelhoPontoDTO.FechamentoInfo(
                fp.getStatus(),
                fechado,
                fechado ? fp.getDataFechamento() : null,
                fechado ? fp.getFechadoPor() : null,
                fp.getAvisoPendencias()
        );
    }

    private Map<LocalDate, Feriado> carregarFeriadosDoAno(Funcionario funcionario, int ano) {
        try {
            String uf = funcionario.getLocalDeTrabalho() != null ? funcionario.getLocalDeTrabalho().getUf() : "";
            String municipio = funcionario.getLocalDeTrabalho() != null ? funcionario.getLocalDeTrabalho().getMunicipio() : "";
            return feriadoRepository.buscarCalendarioAnual(ano, uf, municipio, funcionario.getEmpresa().getId())
                    .stream()
                    .collect(Collectors.toMap(Feriado::getData, f -> f, (f1, f2) -> f1));
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private OcorrenciaColaborador buscarOcorrenciaNaData(List<OcorrenciaColaborador> ocorrencias, LocalDate data) {
        return ocorrencias.stream()
                .filter(o -> !data.isBefore(o.getDataInicio()) && !data.isAfter(o.getDataFim()))
                .findFirst()
                .orElse(null);
    }

    private YearMonth parseCompetencia(String competenciaStr) {
        if (competenciaStr == null || competenciaStr.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(competenciaStr.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Competência inválida (" + competenciaStr + "). Utilize o formato YYYY-MM.");
        }
    }

    private Funcionario obterFuncionarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails fud) {
            return funcionarioRepository.findById(fud.getFuncionario().getId())
                    .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário autenticado não encontrado."));
        }
        throw new AcessoNegadoException("Usuário não autenticado.");
    }

    private String formatarPrevistoHtml(EspelhoPontoDTO.DiaEspelhoResponse dia) {
        if (dia.feriado()) {
            return "Feriado: " + escapeHtml(dia.descricaoFeriado());
        }
        if (!dia.previsto().diaTrabalho()) {
            return "Folga";
        }
        String ent = dia.previsto().horaEntrada() != null ? dia.previsto().horaEntrada().format(HORA_FMT) : "-";
        String sai = dia.previsto().horaSaida() != null ? dia.previsto().horaSaida().format(HORA_FMT) : "-";
        return ent + " às " + sai;
    }

    private String formatarMarcacoesHtml(List<EspelhoPontoDTO.MarcacaoItem> marcacoes) {
        if (marcacoes == null || marcacoes.isEmpty()) {
            return "-";
        }
        return marcacoes.stream()
                .map(m -> m.horario().format(HORA_FMT) + " (" + m.origemLabel() + ")")
                .collect(Collectors.joining(" | "));
    }

    private String formatarExtrasDiaHtml(EspelhoPontoDTO.DiaEspelhoResponse dia) {
        if (dia.minutosHoraExtraTotal() == null || dia.minutosHoraExtraTotal() == 0) {
            return "00:00";
        }
        if (dia.horasExtrasPorFaixa() == null || dia.horasExtrasPorFaixa().isEmpty()) {
            return formatarMinutos(dia.minutosHoraExtraTotal());
        }
        return dia.horasExtrasPorFaixa().entrySet().stream()
                .map(e -> formatarMinutos(e.getValue()) + " (" + e.getKey() + ")")
                .collect(Collectors.joining(", "));
    }

    private String formatarMinutos(Integer totalMinutos) {
        int m = totalMinutos != null ? totalMinutos : 0;
        return String.format("%02d:%02d", m / 60, m % 60);
    }

    private String escapeHtml(String s) {
        if (s == null) return "-";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
