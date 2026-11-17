package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.*;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FechamentoPeriodoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.InconsistenciaDTO;
import com.example.projetoPBD.pontoCerto.repository.*;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.FechamentoComPendenciasException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FechamentoPeriodoService {

    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;
    private final HistoricoFechamentoPeriodoRepository historicoRepository;
    private final InconsistenciaRepository inconsistenciaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final BancoHorasService bancoHorasService;
    private final SecurityTenantContext securityTenantContext;

    public FechamentoPeriodoService(
            FechamentoPeriodoRepository fechamentoPeriodoRepository,
            HistoricoFechamentoPeriodoRepository historicoRepository,
            InconsistenciaRepository inconsistenciaRepository,
            FuncionarioRepository funcionarioRepository,
            BancoHorasService bancoHorasService,
            SecurityTenantContext securityTenantContext
    ) {
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
        this.historicoRepository = historicoRepository;
        this.inconsistenciaRepository = inconsistenciaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.bancoHorasService = bancoHorasService;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional(readOnly = true)
    public FechamentoPeriodoDTO.PreviaResponse consultarPreviaFechamento(String competencia) {
        return consultarPreviaFechamento(obterEmpresaAtualSegura(), competencia);
    }

    @Transactional(readOnly = true)
    public FechamentoPeriodoDTO.PreviaResponse consultarPreviaFechamento(UUID empresaId, String competencia) {
        YearMonth ym = parseCompetencia(competencia);
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        Optional<FechamentoPeriodo> fechamentoOpt = fechamentoPeriodoRepository
                .findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, ym.toString());

        StatusFechamentoPeriodo statusAtual = fechamentoOpt
                .map(FechamentoPeriodo::getStatus)
                .orElse(StatusFechamentoPeriodo.ABERTO);

        boolean periodoFechado = statusAtual == StatusFechamentoPeriodo.CONCLUIDO
                || statusAtual == StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS;

        List<Inconsistencia> abertas = inconsistenciaRepository
                .findByEmpresaIdAndSituacaoAndDataReferenciaBetweenOrderByDataHoraOrigemDesc(
                        empresaId,
                        StatusInconsistencia.ABERTA,
                        inicio,
                        fim
                );

        List<InconsistenciaDTO.Response> pendenciasDto = abertas.stream()
                .map(this::toInconsistenciaResponse)
                .toList();

        return new FechamentoPeriodoDTO.PreviaResponse(
                empresaId,
                ym.toString(),
                inicio,
                fim,
                statusAtual,
                periodoFechado,
                pendenciasDto.size(),
                pendenciasDto
        );
    }

    @Transactional
    public FechamentoPeriodoDTO.FechamentoDetalheResponse fecharPeriodo(FechamentoPeriodoDTO.FecharRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dados de fechamento inválidos.");
        }
        return fecharPeriodo(
                obterEmpresaAtualSegura(),
                req.competencia(),
                req.confirmarComPendencias(),
                req.observacao()
        );
    }

    @Transactional
    public FechamentoPeriodoDTO.FechamentoDetalheResponse fecharPeriodo(
            UUID empresaId,
            String competencia,
            Boolean confirmarComPendencias,
            String observacao
    ) {
        YearMonth ym = parseCompetencia(competencia);
        String mesRef = ym.toString();
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Inconsistencia> abertas = inconsistenciaRepository
                .findByEmpresaIdAndSituacaoAndDataReferenciaBetweenOrderByDataHoraOrigemDesc(
                        empresaId,
                        StatusInconsistencia.ABERTA,
                        inicio,
                        fim
                );

        int qtdAbertas = abertas.size();
        boolean confirmar = Boolean.TRUE.equals(confirmarComPendencias);

        String resumoPorTipo = abertas.stream()
                .collect(Collectors.groupingBy(Inconsistencia::getTipo, Collectors.counting()))
                .entrySet()
                .stream()
                .map(e -> e.getKey().name() + ": " + e.getValue())
                .collect(Collectors.joining(", "));

        if (qtdAbertas > 0 && !confirmar) {
            throw new FechamentoComPendenciasException(
                    "Não é possível fechar o período " + mesRef + " (" + inicio + " a " + fim + "): existem "
                            + qtdAbertas + " inconsistência(s) aberta(s) na fila [" + resumoPorTipo
                            + "]. Trate as pendências antes do fechamento ou confirme o fechamento com ressalva."
            );
        }

        Empresa empresa = obterEmpresaEntidade(empresaId);
        String autor = obterIdentificacaoUsuarioLogado();
        UUID autorId = obterIdUsuarioLogado();
        LocalDateTime agora = LocalDateTime.now();

        FechamentoPeriodo fechamento = fechamentoPeriodoRepository
                .findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, mesRef)
                .orElseGet(FechamentoPeriodo::new);

        fechamento.setEmpresa(empresa);
        fechamento.setMesReferencia(mesRef);
        fechamento.setDataInicio(inicio);
        fechamento.setDataFim(fim);
        fechamento.setDataFechamento(agora);
        fechamento.setFechadoPor(autor);
        fechamento.setFechadoPorId(autorId);
        fechamento.setPendenciasAbertasNoFechamento(qtdAbertas);

        AcaoFechamentoPeriodo acaoHistorico;
        if (qtdAbertas > 0) {
            fechamento.setStatus(StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS);
            String prefixoObs = (observacao != null && !observacao.isBlank()) ? observacao.trim() + " — " : "";
            fechamento.setAvisoPendencias(
                    prefixoObs + "Fechamento realizado com " + qtdAbertas
                            + " pendência(s) em aberto no período (" + resumoPorTipo + ")."
            );
            acaoHistorico = AcaoFechamentoPeriodo.FECHAMENTO_COM_PENDENCIAS;
        } else {
            fechamento.setStatus(StatusFechamentoPeriodo.CONCLUIDO);
            fechamento.setAvisoPendencias(
                    (observacao != null && !observacao.isBlank())
                            ? observacao.trim()
                            : "Período fechado sem pendências abertas."
            );
            acaoHistorico = AcaoFechamentoPeriodo.FECHAMENTO;
        }

        FechamentoPeriodo salvo = fechamentoPeriodoRepository.saveAndFlush(fechamento);

        // Congela saldos do Banco de Horas (T16)
        bancoHorasService.congelarPeriodoEmpresa(empresaId, mesRef, autor);

        // Registra trilha de auditoria em HistoricoFechamentoPeriodo
        HistoricoFechamentoPeriodo hist = new HistoricoFechamentoPeriodo();
        hist.setFechamentoPeriodo(salvo);
        hist.setAcao(acaoHistorico);
        hist.setAutor(autor);
        hist.setAutorId(autorId);
        hist.setDataHora(agora);
        hist.setMotivo(salvo.getAvisoPendencias());
        historicoRepository.saveAndFlush(hist);

        return toDetalheResponse(salvo);
    }

    @Transactional
    public FechamentoPeriodoDTO.FechamentoDetalheResponse reabrirPeriodo(FechamentoPeriodoDTO.ReabrirRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dados de reabertura inválidos.");
        }
        return reabrirPeriodo(obterEmpresaAtualSegura(), req.competencia(), req.motivo());
    }

    @Transactional
    public FechamentoPeriodoDTO.FechamentoDetalheResponse reabrirPeriodo(
            UUID empresaId,
            String competencia,
            String motivo
    ) {
        YearMonth ym = parseCompetencia(competencia);
        String mesRef = ym.toString();

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("O motivo da reabertura é obrigatório.");
        }

        FechamentoPeriodo fechamento = fechamentoPeriodoRepository
                .findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, mesRef)
                .orElseThrow(() -> new IllegalArgumentException("Fechamento não encontrado para a competência " + mesRef + "."));

        if (fechamento.getStatus() == StatusFechamentoPeriodo.ABERTO) {
            throw new IllegalArgumentException("A competência " + mesRef + " já se encontra aberta.");
        }

        String autor = obterIdentificacaoUsuarioLogado();
        UUID autorId = obterIdUsuarioLogado();
        LocalDateTime agora = LocalDateTime.now();

        fechamento.setStatus(StatusFechamentoPeriodo.ABERTO);
        fechamento.setAvisoPendencias("Período reaberto por " + autor + ". Motivo: " + motivo.trim());
        FechamentoPeriodo salvo = fechamentoPeriodoRepository.saveAndFlush(fechamento);

        // Descongela saldos do Banco de Horas (T16) para permitir recálculos e ajustes na competência reaberta
        bancoHorasService.descongelarPeriodoEmpresa(empresaId, mesRef);

        // Registra trilha de auditoria da reabertura
        HistoricoFechamentoPeriodo hist = new HistoricoFechamentoPeriodo();
        hist.setFechamentoPeriodo(salvo);
        hist.setAcao(AcaoFechamentoPeriodo.REABERTURA);
        hist.setAutor(autor);
        hist.setAutorId(autorId);
        hist.setDataHora(agora);
        hist.setMotivo(motivo.trim());
        historicoRepository.saveAndFlush(hist);

        return toDetalheResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<FechamentoPeriodoDTO.FechamentoDetalheResponse> listarFechamentos() {
        return listarFechamentos(obterEmpresaAtualSegura());
    }

    @Transactional(readOnly = true)
    public List<FechamentoPeriodoDTO.FechamentoDetalheResponse> listarFechamentos(UUID empresaId) {
        return fechamentoPeriodoRepository.findByEmpresaIdOrderByDataFechamentoDesc(empresaId)
                .stream()
                .map(this::toDetalheResponse)
                .toList();
    }

    private FechamentoPeriodoDTO.FechamentoDetalheResponse toDetalheResponse(FechamentoPeriodo f) {
        List<FechamentoPeriodoDTO.HistoricoItemResponse> historico = historicoRepository
                .findByFechamentoPeriodoIdOrderByDataHoraDesc(f.getId())
                .stream()
                .map(h -> new FechamentoPeriodoDTO.HistoricoItemResponse(
                        h.getId(),
                        f.getId(),
                        f.getMesReferencia(),
                        h.getAcao(),
                        h.getAutor(),
                        h.getAutorId(),
                        h.getDataHora(),
                        h.getMotivo()
                ))
                .toList();

        return new FechamentoPeriodoDTO.FechamentoDetalheResponse(
                f.getId(),
                f.getEmpresa() != null ? f.getEmpresa().getId() : null,
                f.getMesReferencia(),
                f.getDataInicio(),
                f.getDataFim(),
                f.getStatus(),
                f.getFechadoPor(),
                f.getFechadoPorId(),
                f.getDataFechamento(),
                f.getPendenciasAbertasNoFechamento() != null ? f.getPendenciasAbertasNoFechamento() : 0,
                f.getAvisoPendencias(),
                historico
        );
    }

    private InconsistenciaDTO.Response toInconsistenciaResponse(Inconsistencia i) {
        Funcionario f = i.getFuncionario();
        Equipamento eq = i.getEquipamento();
        return new InconsistenciaDTO.Response(
                i.getId(),
                i.getTipo(),
                i.getSituacao(),
                i.getDataReferencia(),
                i.getDataHoraOrigem(),
                i.getDescricaoOrigem(),
                f != null ? f.getId() : null,
                f != null ? f.getNomeCompleto() : null,
                f != null ? f.getMatricula() : null,
                eq != null ? eq.getId() : null,
                eq != null ? eq.getIdentificacao() : null,
                eq != null ? eq.getCodigo() : null,
                i.getIdentificadorRecebido(),
                i.getNomeArquivo(),
                i.getBatidaOrigemId(),
                i.getBatidaSecundariaId(),
                i.getAcaoTomada(),
                i.getTratadoPor(),
                i.getTratadoEm(),
                i.getMotivoTratamento(),
                i.getDetalheResolucao()
        );
    }

    private YearMonth parseCompetencia(String competencia) {
        if (competencia == null || competencia.trim().isEmpty()) {
            throw new IllegalArgumentException("A competência (YYYY-MM) é obrigatória.");
        }
        try {
            return YearMonth.parse(competencia.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato de competência inválido. Use YYYY-MM (ex.: 2026-10).");
        }
    }

    private UUID obterEmpresaAtualSegura() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails) {
            return securityTenantContext.empresaAtual();
        }
        return funcionarioRepository.findAll().stream()
                .map(f -> f.getEmpresa().getId())
                .findFirst()
                .orElseThrow(() -> new AcessoNegadoException("Usuário não autenticado!"));
    }

    private Empresa obterEmpresaEntidade(UUID empresaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            return userDetails.getFuncionario().getEmpresa();
        }
        return funcionarioRepository.findByEmpresaId(empresaId).stream()
                .map(Funcionario::getEmpresa)
                .findFirst()
                .orElseGet(() -> {
                    Empresa e = new Empresa();
                    e.setId(empresaId);
                    return e;
                });
    }

    private String obterIdentificacaoUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            Funcionario f = userDetails.getFuncionario();
            return f.getNomeCompleto() + " (" + f.getUsuario() + ")";
        }
        return auth != null ? auth.getName() : "RH";
    }

    private UUID obterIdUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            return userDetails.getFuncionario().getId();
        }
        return null;
    }
}
