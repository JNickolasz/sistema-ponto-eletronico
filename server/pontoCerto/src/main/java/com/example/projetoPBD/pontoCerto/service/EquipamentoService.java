package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.LocalDeTrabalho;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.dto.EquipamentoFiltroDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EquipamentoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EquipamentoResponse;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.EquipamentoRepository;
import com.example.projetoPBD.pontoCerto.repository.LocalDeTrabalhoRepository;
import com.example.projetoPBD.pontoCerto.repository.specification.EquipamentoSpecification;
import com.example.projetoPBD.pontoCerto.repository.specification.EstacaoSpecification;
import com.example.projetoPBD.pontoCerto.repository.specification.RelogioSpecification;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.*;
import com.example.projetoPBD.pontoCerto.service.mapper.EquipamentoResponseMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;
    private final LocalDeTrabalhoRepository localDeTrabalhoRepository;
    private final EmpresaRepository empresaRepository;
    private final SecurityTenantContext securityTenantContext;
    private final RelogioService relogioService;
    private final EstacaoService estacaoService;

    public EquipamentoService(EquipamentoRepository equipamentoRepository, LocalDeTrabalhoRepository localDeTrabalhoRepository, EmpresaRepository empresaRepository, SecurityTenantContext securityTenantContext, RelogioService relogioService, EstacaoService estacaoService) {
        this.equipamentoRepository = equipamentoRepository;
        this.localDeTrabalhoRepository = localDeTrabalhoRepository;
        this.empresaRepository = empresaRepository;
        this.securityTenantContext = securityTenantContext;
        this.relogioService = relogioService;
        this.estacaoService = estacaoService;
    }

    @Transactional
    public EquipamentoResponse cadastrar(EquipamentoDTO.Criar criarDto) {

        LocalDeTrabalho local = localDeTrabalhoRepository.findById(criarDto.localTrabalhoId())
                .orElseThrow(() -> new EquipamentoInvalidoException("Local de trabalho não encontrado com id: " + criarDto.localTrabalhoId()));

        Empresa empresa = empresaRepository.findById(securityTenantContext.empresaAtual())
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada!"));


        if (equipamentoRepository.existsByEmpresaIdAndCodigo(empresa.getId(), criarDto.codigo())) {
            throw new EquipamentoExistenteException("Esse equipamento já existe!");
        }

        Equipamento equipamento = new Equipamento();

        switch (criarDto.tipo()){
            case RELOGIO -> equipamento = relogioService.criar(criarDto, empresa, local);
            case ESTACAO -> equipamento = estacaoService.criar(criarDto, empresa, local);
        }

        equipamentoRepository.save(equipamento);
        return EquipamentoResponseMapper.from(equipamento);
    }

    @Transactional(readOnly = true)
    public List<EquipamentoResponse> listar(EquipamentoFiltroDTO filtro) {

        Specification<Equipamento> spec = Specification.where(EquipamentoSpecification.pertenceaEmpresa(securityTenantContext.empresaAtual()));

        if (filtro.localDeTrabalhoId() != null) {
            spec = spec.and(EquipamentoSpecification.pertenceAoLocalDeTrabalho(filtro.localDeTrabalhoId()));
        }

        if (filtro.status() != null) {
            spec = spec.and(EquipamentoSpecification.possuiStatusEquipamento(filtro.status()));
        }

        if (filtro.tipoEquipamento() != null) {
            spec = spec.and(EquipamentoSpecification.possuiTipoEquipamento(filtro.tipoEquipamento()));
        }

        if (filtro.numFabricacao() != null) {
            spec = spec.and(RelogioSpecification.possuiNumFabricante(filtro.numFabricacao()));
        }

        if (filtro.linhasImportadas() != null) {
            spec = spec.and(RelogioSpecification.possuiLinhasImportadas(filtro.linhasImportadas()));
        }

        if(filtro.statusEstacao() != null) {
            spec = spec.and(EstacaoSpecification.statusEstacao(filtro.statusEstacao()));
        }

        return equipamentoRepository.findAll(spec)
                .stream()
                .map(EquipamentoResponseMapper::from)
                .toList();
    }

    @Transactional
    public EquipamentoResponse alterarStatus(String codigo, StatusEquipamento novoStatus) {

        Equipamento equipamento = equipamentoRepository.findByEmpresaIdAndCodigo(securityTenantContext.empresaAtual(), codigo)
                .orElseThrow(() -> new EquipamentoNaoEncontradoException("Equipamento não encontrado com codigo: " + codigo));

        equipamento.setStatus(novoStatus);
        equipamento = equipamentoRepository.save(equipamento);

        return EquipamentoResponseMapper.from(equipamento);
    }

}
