package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Batida;
import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BatidaDTO;
import com.example.projetoPBD.pontoCerto.repository.BatidaRepository;
import com.example.projetoPBD.pontoCerto.repository.EquipamentoRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoNaoEncontradoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BatidaService {
    private final BatidaRepository batidaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final EquipamentoRepository equipamentoReposity;

    @PersistenceContext
    private EntityManager entityManager;

    public BatidaService(
            BatidaRepository batidaRepository,
            FuncionarioRepository funcionarioRepository,
            EquipamentoRepository equipamentoReposity
    ) {
        this.batidaRepository = batidaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.equipamentoReposity = equipamentoReposity;
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
}
