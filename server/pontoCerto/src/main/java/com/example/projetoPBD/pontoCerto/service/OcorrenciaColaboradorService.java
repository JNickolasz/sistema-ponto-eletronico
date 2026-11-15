package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoAbonoOcorrencia;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.OcorrenciaColaboradorDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.TipoOcorrenciaDTO;
import com.example.projetoPBD.pontoCerto.repository.*;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.OcorrenciaConflitoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.PeriodoFechadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OcorrenciaColaboradorService {

    private final OcorrenciaColaboradorRepository ocorrenciaRepository;
    private final TipoOcorrenciaRepository tipoOcorrenciaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;
    private final ApuracaoDiariaService apuracaoDiariaService;
    private final AnexoStorageService anexoStorageService;
    private final SecurityTenantContext securityTenantContext;

    public OcorrenciaColaboradorService(
            OcorrenciaColaboradorRepository ocorrenciaRepository,
            TipoOcorrenciaRepository tipoOcorrenciaRepository,
            FuncionarioRepository funcionarioRepository,
            FechamentoPeriodoRepository fechamentoPeriodoRepository,
            ApuracaoDiariaService apuracaoDiariaService,
            AnexoStorageService anexoStorageService,
            SecurityTenantContext securityTenantContext
    ) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.tipoOcorrenciaRepository = tipoOcorrenciaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
        this.apuracaoDiariaService = apuracaoDiariaService;
        this.anexoStorageService = anexoStorageService;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional
    public OcorrenciaColaboradorDTO.Response lancarOcorrencia(
            OcorrenciaColaboradorDTO.CriarRequest req,
            MultipartFile arquivo
    ) {
        UUID empresaId = securityTenantContext.empresaAtual();

        if (req.funcionarioId() == null) {
            throw new IllegalArgumentException("O colaborador é obrigatório.");
        }
        if (req.tipoOcorrenciaId() == null) {
            throw new IllegalArgumentException("O tipo de ocorrência é obrigatório.");
        }
        if (req.dataInicio() == null || req.dataFim() == null) {
            throw new IllegalArgumentException("As datas de início e fim são obrigatórias.");
        }
        if (req.dataFim().isBefore(req.dataInicio())) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
        }
        Funcionario funcionario = funcionarioRepository.findById(req.funcionarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));
        if (!funcionario.getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("Colaborador não pertence à sua empresa.");
        }

        TipoOcorrencia tipo = tipoOcorrenciaRepository.findByIdAndEmpresaId(req.tipoOcorrenciaId(), empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de ocorrência não encontrado."));
        if (!tipo.isAtivo()) {
            throw new IllegalArgumentException("Este tipo de ocorrência está inativo.");
        }

        // --Bloqueio de periodo fechado--
        validarPeriodoNaoFechado(empresaId, req.dataInicio());
        validarPeriodoNaoFechado(empresaId, req.dataFim());

        // --Bloqueio de sobreposição de datas--
        List<OcorrenciaColaborador> sobrepostas = ocorrenciaRepository.findSobrepostas(
                funcionario.getId(),
                req.dataInicio(),
                req.dataFim()
        );
        if (!sobrepostas.isEmpty()) {
            OcorrenciaColaborador conflito = sobrepostas.get(0);
            throw new OcorrenciaConflitoException(
                    "O colaborador já possui ocorrência '" + conflito.getTipoOcorrencia().getNome() + "' lançada no período de " + conflito.getDataInicio() + " a " + conflito.getDataFim() + "."
            );
        }

        // --validação de anexo obrigatório--
        if (tipo.isExigeAnexo() && (arquivo == null || arquivo.isEmpty())) {
            throw new IllegalArgumentException("Este tipo de ocorrência exige comprovante em anexo.");
        }

        // --tratamento de minutos abonados--
        Integer minutosAbonorPorDia = null;
        if (tipo.getTipoAbono() == TipoAbonoOcorrencia.PARCIAL) {
            minutosAbonorPorDia = req.minutosAbonoPadrao() != null ? req.minutosAbonoPadrao() : tipo.getMinutosAbonoPadrao();

            if (minutosAbonorPorDia == null || minutosAbonorPorDia <= 0) {
                throw new IllegalArgumentException("Para abono parcial, informe a quantidade de minutos abonados por dia.");
            }
        }

        // --upload do anexo (se houver)--
        String caminhoAnexo = anexoStorageService.salvarAnexo(arquivo);
        String nomeOriginal = (arquivo != null && !arquivo.isEmpty()) ? arquivo.getOriginalFilename() : null;
        String contentType = (arquivo != null && !arquivo.isEmpty()) ? arquivo.getContentType() : null;
        OcorrenciaColaborador ocorrencia = new OcorrenciaColaborador();
        ocorrencia.setFuncionario(funcionario);
        ocorrencia.setTipoOcorrencia(tipo);
        ocorrencia.setDataInicio(req.dataInicio());
        ocorrencia.setDataFim(req.dataFim());
        ocorrencia.setMinutosAbonoPorDia(minutosAbonorPorDia);
        ocorrencia.setObservacao(req.observacao());
        ocorrencia.setNomeArquivoAnexo(nomeOriginal);
        ocorrencia.setCaminhoAnexo(caminhoAnexo);
        ocorrencia.setContentTypeAnexo(contentType);
        ocorrencia.setLancadoPor(obterIdentificacaoUsuarioLogado());
        ocorrencia.setLancadoEm(LocalDateTime.now());
        OcorrenciaColaborador salva = ocorrenciaRepository.saveAndFlush(ocorrencia);

        // --recálculo em cascata dos dias do período da ocorrência--
        recalcularDiasAfetados(funcionario, req.dataInicio(), req.dataFim());

        return new OcorrenciaColaboradorDTO.Response(salva);
    }

    @Transactional
    public void cancelarOcorrencia(UUID ocorrenciaId) {
        UUID empresaId = securityTenantContext.empresaAtual();
        OcorrenciaColaborador o = ocorrenciaRepository.findById(ocorrenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Ocorrência não encontrada."));
        if (!o.getFuncionario().getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("Esta ocorrência não pertence à sua empresa.");
        }
        validarPeriodoNaoFechado(empresaId, o.getDataInicio());
        validarPeriodoNaoFechado(empresaId, o.getDataFim());
        Funcionario funcionario = o.getFuncionario();
        LocalDate inicio = o.getDataInicio();
        LocalDate fim = o.getDataFim();
        if (o.getCaminhoAnexo() != null) {
            anexoStorageService.excluirAnexo(o.getCaminhoAnexo());
        }
        ocorrenciaRepository.delete(o);
        ocorrenciaRepository.flush();
        // Recalcula o espelho para remover o abono dos dias que haviam sido justificados
        recalcularDiasAfetados(funcionario, inicio, fim);
    }
    @Transactional(readOnly = true)
    public List<OcorrenciaColaboradorDTO.Response> listar(
            UUID funcionarioId,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        UUID empresaId = securityTenantContext.empresaAtual();
        return ocorrenciaRepository.findFiltradas(empresaId, funcionarioId, dataInicio, dataFim)
                .stream()
                .map(OcorrenciaColaboradorDTO.Response::new)
                .toList();
    }
    @Transactional(readOnly = true)
    public Resource carregarAnexo(UUID ocorrenciaId) {
        UUID empresaId = securityTenantContext.empresaAtual();
        OcorrenciaColaborador o = ocorrenciaRepository.findById(ocorrenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Ocorrência não encontrada."));
        if (!o.getFuncionario().getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("Acesso negado ao anexo da ocorrência.");
        }
        if (o.getCaminhoAnexo() == null || o.getCaminhoAnexo().isBlank()) {
            throw new IllegalArgumentException("Esta ocorrência não possui arquivo anexado.");
        }
        Resource recurso = anexoStorageService.carregarComoRecurso(o.getCaminhoAnexo());
        if (recurso == null) {
            throw new IllegalArgumentException("Arquivo do anexo não foi encontrado no servidor.");
        }
        return recurso;
    }
    @Transactional(readOnly = true)
    public OcorrenciaColaborador buscarPorId(UUID ocorrenciaId) {
        UUID empresaId = securityTenantContext.empresaAtual();
        OcorrenciaColaborador o = ocorrenciaRepository.findById(ocorrenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Ocorrência não encontrada."));
        if (!o.getFuncionario().getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("Acesso negado.");
        }
        return o;
    }
    private void recalcularDiasAfetados(Funcionario funcionario, LocalDate inicio, LocalDate fim) {
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fim)) {
            try {
                apuracaoDiariaService.apurarDia(funcionario, cursor);
            } catch (Exception ignored) {}
            cursor = cursor.plusDays(1);
        }
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
    private String obterIdentificacaoUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            Funcionario f = userDetails.getFuncionario();
            return f.getNomeCompleto() + " (" + f.getMatricula() + ")";
        }
        return auth != null ? auth.getName() : "RH";
    }
}
