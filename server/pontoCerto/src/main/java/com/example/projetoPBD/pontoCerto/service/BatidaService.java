package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Batida;
import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.domain.HistoricoImportacao;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusBatida;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BatidaDTO;
import com.example.projetoPBD.pontoCerto.repository.BatidaRepository;
import com.example.projetoPBD.pontoCerto.repository.EquipamentoRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.HistoricoImportacaoRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoNaoEncontradoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class BatidaService {
    private final BatidaRepository batidaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final EquipamentoRepository equipamentoReposity;
    private final HistoricoImportacaoRepository historicoImportacaoRepository;
    private final SecurityTenantContext securityTenantContext;

    @PersistenceContext
    private EntityManager entityManager;

    public BatidaService(
            BatidaRepository batidaRepository,
            FuncionarioRepository funcionarioRepository,
            EquipamentoRepository equipamentoReposity,
            HistoricoImportacaoRepository historicoImportacaoRepository,
            SecurityTenantContext securityTenantContext
    ) {
        this.batidaRepository = batidaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.equipamentoReposity = equipamentoReposity;
        this.historicoImportacaoRepository = historicoImportacaoRepository;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional
    public BatidaDTO.ComprovanteResponse registrarBatida(BatidaDTO.Request dto) {
        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Funcionário não encontrado"));

        Equipamento equipamento = null;
        if (dto.equipamentoId() != null) {
            equipamento = equipamentoReposity.findById(dto.equipamentoId())
                    .orElseThrow(() -> new EquipamentoNaoEncontradoException("Equipamento não encontrado"));
        }

        Batida batida = new Batida();
        batida.setFuncionario(funcionario);
        batida.setEquipamento(equipamento);
        batida.setDataHora(LocalDateTime.now());
        batida.setTipoRegistro(dto.tipoRegistro());
        batida.setStatusBatida(StatusBatida.NORMAL);
        batida.setTipoOrigem(equipamento != null ? TipoOrigem.ESTACAO : TipoOrigem.APLICATIVO_WEB);
        batida.setLatitudeRecebida(dto.latitude());
        batida.setLongitudeRecebida(dto.longitude());
        batida.setIpRecebido(dto.ip());

        batida = batidaRepository.saveAndFlush(batida);

        // só por segurança, antes de dar o return faz uma verificação
        // e caso o nsr seja == null ele tenta criar novamente
        if (batida.getNsr() == null) {
            entityManager.refresh(batida);
        }

        return new BatidaDTO.ComprovanteResponse(batida);
    }

    @Transactional(readOnly = true)
    public List<BatidaDTO.ComprovanteResponse> listarPorFuncionario(UUID funcionarioId) {
        if (!funcionarioRepository.existsById(funcionarioId)) {
            throw new UsuarioNaoEncontradoException("Funcionário não encontrado");
        }

        return batidaRepository.findByFuncionarioIdOrderByDataHoraDesc(funcionarioId)
                .stream()
                .map(BatidaDTO.ComprovanteResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BatidaDTO.BatidaOrfaResponse> listarOrfas(StatusBatida statusFiltro) {
        UUID empresaId = securityTenantContext.empresaAtual();
        StatusBatida status = statusFiltro != null ? statusFiltro : StatusBatida.ORFA_PENDENTE;

        List<Batida> orfas = batidaRepository.findByEmpresaIdAndStatusBatidaOrderByDataHoraDesc(empresaId, status);

        Map<UUID, HistoricoImportacao> historicosMap = new HashMap<>();
        orfas.stream()
                .map(Batida::getArquivoImportadoId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(arqId -> historicoImportacaoRepository.findById(arqId)
                        .ifPresent(h -> historicosMap.put(arqId, h)));

        return orfas.stream()
                .map(b -> {
                    HistoricoImportacao h = b.getArquivoImportadoId() != null ? historicosMap.get(b.getArquivoImportadoId()) : null;
                    String nomeArq = h != null ? h.getNomeArquivo() : (b.getArquivoImportadoId() != null ? "Arquivo importado" : "Registro direto");
                    String conteudoArq = h != null ? h.getConteudoArquivo() : null;
                    return new BatidaDTO.BatidaOrfaResponse(b, nomeArq, conteudoArq);
                })
                .toList();
    }

    @Transactional
    public BatidaDTO.BatidaOrfaResponse vincularOrfa(UUID batidaId, BatidaDTO.VincularOrfaRequest dto) {
        Batida batida = batidaRepository.findById(batidaId)
                .orElseThrow(() -> new IllegalArgumentException("Batida não encontrada."));

        UUID empresaId = obterEmpresaContextoOuEquipamento(batida);

        if (batida.getEquipamento() != null && !batida.getEquipamento().getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("Esta batida não pertence à sua empresa.");
        }

        if (batida.getStatusBatida() != StatusBatida.ORFA_PENDENTE) {
            throw new IllegalArgumentException("Apenas batidas com status ORFA_PENDENTE podem ser vinculadas.");
        }

        if (dto.motivo() == null || dto.motivo().trim().isEmpty()) {
            throw new IllegalArgumentException("O motivo do vínculo é obrigatório.");
        }

        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));

        if (empresaId != null && !funcionario.getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("O colaborador selecionado não pertence à sua empresa.");
        }

        if (batidaRepository.existsByFuncionarioIdAndDataHora(funcionario.getId(), batida.getDataHora())) {
            throw new IllegalArgumentException("Este colaborador já possui uma batida registrada nesta mesma data e hora.");
        }

        String autorVinculo = obterIdentificacaoUsuarioLogado();

        batida.setFuncionario(funcionario);
        batida.setStatusBatida(StatusBatida.VINCULADA);
        batida.setVinculadoPor(autorVinculo);
        batida.setDataVinculo(LocalDateTime.now());
        batida.setMotivoVinculo(dto.motivo().trim());

        batida = batidaRepository.saveAndFlush(batida);

        HistoricoImportacao historico = batida.getArquivoImportadoId() != null
                ? historicoImportacaoRepository.findById(batida.getArquivoImportadoId()).orElse(null)
                : null;
        String nomeArquivo = historico != null
                ? historico.getNomeArquivo()
                : (batida.getArquivoImportadoId() != null ? "Arquivo importado" : "Registro direto");
        String conteudoArquivo = historico != null ? historico.getConteudoArquivo() : null;

        return new BatidaDTO.BatidaOrfaResponse(batida, nomeArquivo, conteudoArquivo);
    }

    private UUID obterEmpresaContextoOuEquipamento(Batida batida) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails) {
            return securityTenantContext.empresaAtual();
        }
        return batida.getEquipamento() != null ? batida.getEquipamento().getEmpresa().getId() : null;
    }

    private String obterIdentificacaoUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            Funcionario f = userDetails.getFuncionario();
            return f.getNomeCompleto() + " (" + f.getUsuario() + ")";
        }
        return auth != null ? auth.getName() : "RH";
    }
}
