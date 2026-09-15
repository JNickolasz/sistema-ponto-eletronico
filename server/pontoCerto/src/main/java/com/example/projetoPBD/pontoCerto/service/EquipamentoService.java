package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.LocalDeTrabalho;
import com.example.projetoPBD.pontoCerto.domain.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.domain.TipoEquipamento;
import com.example.projetoPBD.pontoCerto.dto.EquipamentoDTO;
import com.example.projetoPBD.pontoCerto.repository.EquipamentoRepository;
import com.example.projetoPBD.pontoCerto.repository.LocalDeTrabalhoRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoExistenteException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoInvalidoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;
    private final LocalDeTrabalhoRepository localDeTrabalhoRepository;

    public EquipamentoService(EquipamentoRepository equipamentoRepository, LocalDeTrabalhoRepository localDeTrabalhoRepository) {
        this.equipamentoRepository = equipamentoRepository;
        this.localDeTrabalhoRepository = localDeTrabalhoRepository;
    }

    @Transactional
    public EquipamentoDTO.Response cadastrar(EquipamentoDTO.Request dto) {
        if (dto.localTrabalhoId() == null) {
            throw new EquipamentoInvalidoException("O local de trabalho é obrigatório");
        }

        LocalDeTrabalho local = localDeTrabalhoRepository.findById(dto.localTrabalhoId())
                .orElseThrow(() -> new EquipamentoInvalidoException("Local de trabalho não encontrado com id: " + dto.localTrabalhoId()));

        Empresa empresa = local.getEmpresa();
        if (empresa == null) {
            throw new EquipamentoInvalidoException("O local de trabalho não possui empresa vinculada");
        }

        if (dto.tipo() == null) {
            throw new EquipamentoInvalidoException("O tipo do equipamento é obrigatório");
        }

        if (dto.identificacao() == null || dto.identificacao().isBlank()) {
            throw new EquipamentoInvalidoException("A identificação do equipamento é obrigatória");
        }

        String numFabricacao = dto.numFabricacao() != null ? dto.numFabricacao().trim() : null;

        if (dto.tipo() == TipoEquipamento.RELOGIO) {
            if (numFabricacao == null || numFabricacao.isBlank()) {
                throw new EquipamentoInvalidoException("O número de fabricação é obrigatório para equipamentos do tipo RELOGIO");
            }
            if (equipamentoRepository.existsByEmpresaIdAndNumFabricacao(empresa.getId(), numFabricacao)) {
                throw new EquipamentoExistenteException("Já existe um relógio cadastrado com o número de fabricação '" + numFabricacao + "' para esta empresa");
            }
        } else {
            // Estação web não exige número de fabricação, portanto vai forcar o valor ser null
            numFabricacao = null;
        }

        Equipamento eq = new Equipamento();
        eq.setEmpresa(empresa);
        eq.setLocalTrabalho(local);
        eq.setTipo(dto.tipo());
        eq.setIdentificacao(dto.identificacao().trim());
        eq.setNumFabricacao(numFabricacao);
        eq.setStatus(StatusEquipamento.ATIVO);

        eq = equipamentoRepository.save(eq);
        return new EquipamentoDTO.Response(eq);
    }

    @Transactional(readOnly = true)
    public List<EquipamentoDTO.Response> listar(UUID localTrabalhoId, UUID empresaId) {
        List<Equipamento> lista;
        if (localTrabalhoId != null) {
            lista = equipamentoRepository.findByLocalTrabalhoId(localTrabalhoId);
        } else if (empresaId != null) {
            lista = equipamentoRepository.findByEmpresaId(empresaId);
        } else {
            lista = equipamentoRepository.findAll();
        }

        return lista.stream()
                .map(EquipamentoDTO.Response::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipamentoDTO.Response buscarPorId(UUID id) {
        Equipamento eq = equipamentoRepository.findById(id)
                .orElseThrow(() -> new EquipamentoNaoEncontradoException("Equipamento não encontrado com id: " + id));
        return new EquipamentoDTO.Response(eq);
    }

    @Transactional
    public EquipamentoDTO.Response alterarStatus(UUID id, StatusEquipamento novoStatus) {
        Equipamento eq = equipamentoRepository.findById(id)
                .orElseThrow(() -> new EquipamentoNaoEncontradoException("Equipamento não encontrado com id: " + id));
        eq.setStatus(novoStatus);
        eq = equipamentoRepository.save(eq);
        return new EquipamentoDTO.Response(eq);
    }
}
