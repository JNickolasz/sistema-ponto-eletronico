package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Batida;
import com.example.projetoPBD.pontoCerto.domain.FechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.domain.SolicitacaoAjuste;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusBatida;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusSolicitacaoAjuste;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.SolicitacaoAjusteDTO;
import com.example.projetoPBD.pontoCerto.repository.BatidaRepository;
import com.example.projetoPBD.pontoCerto.repository.FechamentoPeriodoRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.SolicitacaoAjusteRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.PeriodoFechadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SolicitacaoAjusteService {

    private final SolicitacaoAjusteRepository solicitacaoAjusteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;
    private final BatidaRepository batidaRepository;
    private final InconsistenciaService inconsistenciaService;
    private final SecurityTenantContext securityTenantContext;

    public SolicitacaoAjusteService(
            SolicitacaoAjusteRepository solicitacaoAjusteRepository,
            FuncionarioRepository funcionarioRepository,
            FechamentoPeriodoRepository fechamentoPeriodoRepository,
            BatidaRepository batidaRepository,
            InconsistenciaService inconsistenciaService,
            SecurityTenantContext securityTenantContext
    ) {
        this.solicitacaoAjusteRepository = solicitacaoAjusteRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
        this.batidaRepository = batidaRepository;
        this.inconsistenciaService = inconsistenciaService;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional
    public SolicitacaoAjusteDTO.Response abrirSolicitacao(SolicitacaoAjusteDTO.CriarRequest dto) {
        return abrirSolicitacao(obterUsuarioLogado(), dto);
    }

    @Transactional
    public SolicitacaoAjusteDTO.Response abrirSolicitacao(Funcionario colaborador, SolicitacaoAjusteDTO.CriarRequest dto) {
        if (dto.dataAlvo().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data do ajuste não pode ser futura.");
        }

        validarPeriodoFechado(colaborador.getEmpresa().getId(), dto.dataAlvo());

        SolicitacaoAjuste solicitacao = new SolicitacaoAjuste();
        solicitacao.setColaborador(colaborador);
        solicitacao.setDataAlvo(dto.dataAlvo());
        solicitacao.setHorarioPretendido(dto.horarioPretendido());
        solicitacao.setTipoRegistro(dto.tipoRegistro());
        solicitacao.setMotivo(dto.motivo().trim());
        solicitacao.setStatus(StatusSolicitacaoAjuste.PENDENTE);
        solicitacao.setSolicitadoEm(LocalDateTime.now());

        solicitacao = solicitacaoAjusteRepository.save(solicitacao);
        return new SolicitacaoAjusteDTO.Response(solicitacao);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoAjusteDTO.Response> listarMinhas() {
        return listarMinhas(obterUsuarioLogado());
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoAjusteDTO.Response> listarMinhas(Funcionario colaborador) {
        return solicitacaoAjusteRepository.findByColaboradorIdOrderBySolicitadoEmDesc(colaborador.getId())
                .stream()
                .map(SolicitacaoAjusteDTO.Response::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoAjusteDTO.Response> listarEquipe(StatusSolicitacaoAjuste status) {
        return listarEquipe(obterUsuarioLogado(), status);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoAjusteDTO.Response> listarEquipe(Funcionario gestor, StatusSolicitacaoAjuste status) {
        List<SolicitacaoAjuste> lista = (status != null)
                ? solicitacaoAjusteRepository.findByColaboradorGestorIdAndStatusOrderBySolicitadoEmDesc(gestor.getId(), status)
                : solicitacaoAjusteRepository.findByColaboradorGestorIdOrderBySolicitadoEmDesc(gestor.getId());

        return lista.stream()
                .map(SolicitacaoAjusteDTO.Response::new)
                .toList();
    }

    @Transactional
    public SolicitacaoAjusteDTO.Response analisarSolicitacao(UUID id, SolicitacaoAjusteDTO.AnalisarRequest dto) {
        return analisarSolicitacao(id, obterUsuarioLogado(), dto);
    }

    @Transactional
    public SolicitacaoAjusteDTO.Response analisarSolicitacao(UUID id, Funcionario gestor, SolicitacaoAjusteDTO.AnalisarRequest dto) {
        SolicitacaoAjuste solicitacao = solicitacaoAjusteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação de ajuste não encontrada."));

        if (solicitacao.getStatus() != StatusSolicitacaoAjuste.PENDENTE) {
            throw new IllegalStateException("Esta solicitação já foi analisada anteriormente.");
        }

        if (dto.status() == StatusSolicitacaoAjuste.PENDENTE) {
            throw new IllegalArgumentException("O status da análise deve ser DEFERIDO ou INDEFERIDO.");
        }

        validarPermissaoGestao(gestor, solicitacao.getColaborador());

        solicitacao.setGestor(gestor);
        solicitacao.setAnalisadoEm(LocalDateTime.now());
        solicitacao.setJustificativaGestor(dto.justificativaGestor().trim());
        solicitacao.setStatus(dto.status());

        if (dto.status() == StatusSolicitacaoAjuste.DEFERIDO) {
            Batida batida = new Batida();
            batida.setFuncionario(solicitacao.getColaborador());
            batida.setDataHora(LocalDateTime.of(solicitacao.getDataAlvo(), solicitacao.getHorarioPretendido()));
            batida.setTipoRegistro(solicitacao.getTipoRegistro());
            batida.setStatusBatida(StatusBatida.NORMAL);
            batida.setTipoOrigem(TipoOrigem.AJUSTE_MANUAL);
            batida.setSolicitacaoAjusteId(solicitacao.getId());
            batida.setPedidoAjusteId(solicitacao.getId());
            batida.setAutorAjuste(gestor.getNomeCompleto() + " (" + gestor.getUsuario() + ")");
            batida.setMotivoAjuste(solicitacao.getMotivo());

            Long nsr = batidaRepository.proximoNsrSequence();
            batida.setNsr(nsr);

            batida = batidaRepository.saveAndFlush(batida);
            solicitacao.setBatidaCriada(batida);

            try {
                inconsistenciaService.analisarDiaColaborador(solicitacao.getColaborador(), solicitacao.getDataAlvo());
            } catch (Exception ignored) {}
        }

        solicitacao = solicitacaoAjusteRepository.saveAndFlush(solicitacao);
        return new SolicitacaoAjusteDTO.Response(solicitacao);
    }

    private void validarPeriodoFechado(UUID empresaId, LocalDate dataAlvo) {
        String mesReferencia = dataAlvo.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Optional<FechamentoPeriodo> fechamentoOpt = fechamentoPeriodoRepository
                .findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, mesReferencia);

        if (fechamentoOpt.isPresent()) {
            StatusFechamentoPeriodo status = fechamentoOpt.get().getStatus();
            if (status == StatusFechamentoPeriodo.CONCLUIDO || status == StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS) {
                throw new PeriodoFechadoException("Não é permitido solicitar ajuste para período já fechado (" + mesReferencia + ").");
            }
        }
    }

    private void validarPermissaoGestao(Funcionario gestor, Funcionario colaborador) {
        if (gestor.getPerfilAcesso() == PerfilAcesso.RH) {
            return; // RH possui autoridade administrativa global
        }

        boolean ehLiderDireto = colaborador.getGestor() != null && colaborador.getGestor().getId().equals(gestor.getId());
        if (!ehLiderDireto) {
            throw new AcessoNegadoException("Você não tem permissão para analisar solicitações deste colaborador.");
        }
    }

    private Funcionario obterUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            return funcionarioRepository.findById(userDetails.getFuncionario().getId())
                    .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário logado não encontrado no banco."));
        }
        throw new AcessoNegadoException("Usuário não autenticado.");
    }
}
