package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ApuracaoDiariaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BancoHorasDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ExportacaoFolhaDTO;
import com.example.projetoPBD.pontoCerto.repository.*;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.PeriodoNaoFechadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExportacaoFolhaService {

    private static final String CABECALHO_CSV =
            "Competencia;Matricula;CPF;NomeColaborador;Cargo;HorasNormaisMin;HorasExtras50Min;HorasExtras100Min;OutrasExtrasPorFaixa;AdicionalNoturnoMin;FaltasMin;AtrasosMin;SaldoBancoHorasMin\n";

    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ApuracaoDiariaRepository apuracaoDiariaRepository;
    private final ApuracaoDiariaService apuracaoDiariaService;
    private final BancoHorasService bancoHorasService;
    private final HistoricoExportacaoFolhaRepository historicoRepository;

    public ExportacaoFolhaService(
            FechamentoPeriodoRepository fechamentoPeriodoRepository,
            FuncionarioRepository funcionarioRepository,
            ApuracaoDiariaRepository apuracaoDiariaRepository,
            ApuracaoDiariaService apuracaoDiariaService,
            BancoHorasService bancoHorasService,
            HistoricoExportacaoFolhaRepository historicoRepository
    ) {
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.apuracaoDiariaRepository = apuracaoDiariaRepository;
        this.apuracaoDiariaService = apuracaoDiariaService;
        this.bancoHorasService = bancoHorasService;
        this.historicoRepository = historicoRepository;
    }

    /**
     * Gera o resumo estruturado da exportação da folha (sem gravar histórico).
     * Exige que o período esteja fechado e usuário com perfil RH.
     */
    @Transactional(readOnly = true)
    public ExportacaoFolhaDTO.ResumoExportacaoResponse consultarPreviaExportacao(String competenciaStr) {
        Funcionario rh = obterRhAutenticado();
        YearMonth ym = parseCompetencia(competenciaStr);
        String competencia = ym.toString();
        Empresa empresa = rh.getEmpresa();

        validarPeriodoFechadoParaFolha(empresa.getId(), competencia);

        List<ExportacaoFolhaDTO.LinhaColaboradorFolha> linhas = consolidarLinhas(empresa.getId(), ym);
        String csv = montarConteudoCsv(competencia, linhas);
        String hash = calcularSha256(csv);
        ExportacaoFolhaDTO.TotaisArquivoFolha totais = calcularTotais(linhas);

        return new ExportacaoFolhaDTO.ResumoExportacaoResponse(
                competencia,
                "folha-" + competencia + ".csv",
                linhas.size(),
                hash,
                totais,
                linhas
        );
    }

    /**
     * Executa a exportação determinística para a folha de pagamento e registra a trilha de auditoria.
     */
    @Transactional
    public ExportacaoFolhaDTO.ResumoExportacaoResponse exportarFolha(String competenciaStr) {
        Funcionario rh = obterRhAutenticado();
        YearMonth ym = parseCompetencia(competenciaStr);
        String competencia = ym.toString();
        Empresa empresa = rh.getEmpresa();

        validarPeriodoFechadoParaFolha(empresa.getId(), competencia);

        List<ExportacaoFolhaDTO.LinhaColaboradorFolha> linhas = consolidarLinhas(empresa.getId(), ym);
        String csv = montarConteudoCsv(competencia, linhas);
        String hash = calcularSha256(csv);
        String nomeArquivo = "folha-" + competencia + ".csv";

        HistoricoExportacaoFolha hist = new HistoricoExportacaoFolha();
        hist.setEmpresa(empresa);
        hist.setCompetencia(competencia);
        hist.setAutor(rh.getNomeCompleto() + " (" + rh.getUsuario() + ")");
        hist.setAutorId(rh.getId());
        hist.setDataHora(LocalDateTime.now());
        hist.setNomeArquivo(nomeArquivo);
        hist.setQuantidadeColaboradores(linhas.size());
        hist.setHashSha256(hash);
        historicoRepository.saveAndFlush(hist);

        return new ExportacaoFolhaDTO.ResumoExportacaoResponse(
                competencia,
                nomeArquivo,
                linhas.size(),
                hash,
                calcularTotais(linhas),
                linhas
        );
    }

    /**
     * Executa a exportação e devolve o conteúdo CSV exato (determinístico) para download.
     */
    @Transactional
    public String exportarFolhaCsv(String competenciaStr) {
        ExportacaoFolhaDTO.ResumoExportacaoResponse resumo = exportarFolha(competenciaStr);
        return montarConteudoCsv(resumo.competencia(), resumo.linhas());
    }

    /**
     * Lista o histórico auditável de exportações de folha da empresa.
     */
    @Transactional(readOnly = true)
    public List<ExportacaoFolhaDTO.HistoricoItemResponse> listarHistorico(String competenciaStr) {
        Funcionario rh = obterRhAutenticado();
        UUID empresaId = rh.getEmpresa().getId();

        List<HistoricoExportacaoFolha> registros;
        if (competenciaStr != null && !competenciaStr.isBlank()) {
            YearMonth ym = parseCompetencia(competenciaStr);
            registros = historicoRepository.findByEmpresaIdAndCompetenciaOrderByDataHoraDesc(empresaId, ym.toString());
        } else {
            registros = historicoRepository.findByEmpresaIdOrderByDataHoraDesc(empresaId);
        }

        return registros.stream()
                .map(h -> new ExportacaoFolhaDTO.HistoricoItemResponse(
                        h.getId(),
                        h.getCompetencia(),
                        h.getAutor(),
                        h.getAutorId(),
                        h.getDataHora(),
                        h.getNomeArquivo(),
                        h.getQuantidadeColaboradores(),
                        h.getHashSha256()
                ))
                .toList();
    }

    public String montarConteudoCsv(String competencia, List<ExportacaoFolhaDTO.LinhaColaboradorFolha> linhas) {
        StringBuilder sb = new StringBuilder(CABECALHO_CSV);
        for (ExportacaoFolhaDTO.LinhaColaboradorFolha l : linhas) {
            String outrasExtras = l.extrasPorFaixa().entrySet().stream()
                    .filter(e -> !"50%".equals(e.getKey()) && !"100%".equals(e.getKey()))
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("|"));

            sb.append(competencia).append(';')
                    .append(csv(l.matricula())).append(';')
                    .append(csv(l.cpf())).append(';')
                    .append(csv(l.nomeCompleto())).append(';')
                    .append(csv(l.cargo())).append(';')
                    .append(l.horasNormaisMinutos()).append(';')
                    .append(l.horasExtras50Minutos()).append(';')
                    .append(l.horasExtras100Minutos()).append(';')
                    .append(csv(outrasExtras)).append(';')
                    .append(l.adicionalNoturnoMinutos()).append(';')
                    .append(l.faltasMinutos()).append(';')
                    .append(l.atrasosMinutos()).append(';')
                    .append(l.saldoBancoHorasMinutos()).append('\n');
        }
        return sb.toString();
    }

    private List<ExportacaoFolhaDTO.LinhaColaboradorFolha> consolidarLinhas(UUID empresaId, YearMonth ym) {
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Funcionario> colaboradores = funcionarioRepository.findByEmpresaIdOrderByNomeCompletoAsc(empresaId)
                .stream()
                .sorted(Comparator
                        .comparing(Funcionario::getMatricula, Comparator.nullsLast(String::compareTo))
                        .thenComparing(Funcionario::getNomeCompleto, Comparator.nullsLast(String::compareTo))
                        .thenComparing(Funcionario::getId))
                .toList();

        Map<UUID, List<ApuracaoDiaria>> apuracoesPorFunc = apuracaoDiariaRepository
                .findByEmpresaIdAndDataReferenciaBetweenOrderByDataReferenciaAsc(empresaId, inicio, fim)
                .stream()
                .filter(ap -> ap.getFuncionario() != null)
                .collect(Collectors.groupingBy(ap -> ap.getFuncionario().getId()));

        List<ExportacaoFolhaDTO.LinhaColaboradorFolha> linhas = new ArrayList<>();
        for (Funcionario f : colaboradores) {
            List<ApuracaoDiaria> aps = apuracoesPorFunc.getOrDefault(f.getId(), List.of());

            int totalTrabalhado = 0;
            int totalExtras = 0;
            int totalNoturno = 0;
            int totalFaltas = 0;
            int totalAtrasos = 0;
            Map<String, Integer> extrasPorFaixa = new TreeMap<>();

            for (ApuracaoDiaria ap : aps) {
                totalTrabalhado += ap.getMinutosTrabalhados() != null ? ap.getMinutosTrabalhados() : 0;
                totalExtras += ap.getMinutosHoraExtraTotal() != null ? ap.getMinutosHoraExtraTotal() : 0;
                totalNoturno += ap.getMinutosAdicionalNoturno() != null ? ap.getMinutosAdicionalNoturno() : 0;
                totalFaltas += ap.getMinutosFalta() != null ? ap.getMinutosFalta() : 0;
                totalAtrasos += (ap.getMinutosAtraso() != null ? ap.getMinutosAtraso() : 0)
                        + (ap.getMinutosSaidaAntecipada() != null ? ap.getMinutosSaidaAntecipada() : 0);

                ApuracaoDiariaDTO.Response dto = apuracaoDiariaService.toDTO(ap);
                if (dto.horasExtrasPorFaixa() != null) {
                    dto.horasExtrasPorFaixa().forEach((faixa, mins) ->
                            extrasPorFaixa.merge(faixa, mins != null ? mins : 0, Integer::sum)
                    );
                }
            }

            int horasNormais = Math.max(0, totalTrabalhado - totalExtras);
            int extras50 = extrasPorFaixa.getOrDefault("50%", 0);
            int extras100 = extrasPorFaixa.getOrDefault("100%", 0);

            BancoHorasDTO.ExtratoPeriodoResponse extratoBh = bancoHorasService.consultarExtrato(f.getId(), inicio, fim);
            int saldoBh = extratoBh != null ? extratoBh.saldoFinalMinutos() : 0;

            linhas.add(new ExportacaoFolhaDTO.LinhaColaboradorFolha(
                    f.getId(),
                    f.getMatricula(),
                    f.getCpf(),
                    f.getNomeCompleto(),
                    f.getCargo(),
                    horasNormais,
                    extras50,
                    extras100,
                    extrasPorFaixa,
                    totalNoturno,
                    totalFaltas,
                    totalAtrasos,
                    saldoBh
            ));
        }
        return linhas;
    }

    private ExportacaoFolhaDTO.TotaisArquivoFolha calcularTotais(List<ExportacaoFolhaDTO.LinhaColaboradorFolha> linhas) {
        int normais = 0, ex50 = 0, ex100 = 0, noturno = 0, faltas = 0, atrasos = 0, saldoBh = 0;
        for (ExportacaoFolhaDTO.LinhaColaboradorFolha l : linhas) {
            normais += l.horasNormaisMinutos();
            ex50 += l.horasExtras50Minutos();
            ex100 += l.horasExtras100Minutos();
            noturno += l.adicionalNoturnoMinutos();
            faltas += l.faltasMinutos();
            atrasos += l.atrasosMinutos();
            saldoBh += l.saldoBancoHorasMinutos();
        }
        return new ExportacaoFolhaDTO.TotaisArquivoFolha(normais, ex50, ex100, noturno, faltas, atrasos, saldoBh);
    }

    private void validarPeriodoFechadoParaFolha(UUID empresaId, String competencia) {
        Optional<FechamentoPeriodo> fechamentoOpt = fechamentoPeriodoRepository
                .findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, competencia);

        if (fechamentoOpt.isEmpty()) {
            throw new PeriodoNaoFechadoException(
                    "Exportação recusada: a competência " + competencia
                            + " ainda não possui fechamento registrado. Realize o fechamento do período antes de exportar para a folha."
            );
        }

        StatusFechamentoPeriodo status = fechamentoOpt.get().getStatus();
        if (status != StatusFechamentoPeriodo.CONCLUIDO && status != StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS) {
            throw new PeriodoNaoFechadoException(
                    "Exportação recusada: a competência " + competencia + " encontra-se com status " + status
                            + ". Apenas competências fechadas podem ser exportadas para a folha."
            );
        }
    }

    private Funcionario obterRhAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof FuncionarioUserDetails fud)) {
            throw new AcessoNegadoException("Usuário não autenticado.");
        }
        Funcionario f = funcionarioRepository.findById(fud.getFuncionario().getId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário autenticado não encontrado."));
        if (f.getPerfilAcesso() != PerfilAcesso.RH) {
            throw new AcessoNegadoException("Apenas usuários com perfil RH podem exportar dados para a folha de pagamento.");
        }
        return f;
    }

    private YearMonth parseCompetencia(String competenciaStr) {
        if (competenciaStr == null || competenciaStr.isBlank()) {
            throw new IllegalArgumentException("A competência (YYYY-MM) é obrigatória para exportação da folha.");
        }
        try {
            return YearMonth.parse(competenciaStr.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Competência inválida (" + competenciaStr + "). Utilize o formato YYYY-MM.");
        }
    }

    private String calcularSha256(String conteudo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(conteudo.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível.", e);
        }
    }

    private String csv(String valor) {
        if (valor == null) return "";
        String limpo = valor.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ");
        if (!limpo.isEmpty() && (limpo.charAt(0) == '=' || limpo.charAt(0) == '+'
                || limpo.charAt(0) == '-' || limpo.charAt(0) == '@' || limpo.charAt(0) == '\t')) {
            limpo = "'" + limpo;
        }
        if (limpo.contains(";") || limpo.contains("\"")) {
            return "\"" + limpo + "\"";
        }
        return limpo;
    }
}
