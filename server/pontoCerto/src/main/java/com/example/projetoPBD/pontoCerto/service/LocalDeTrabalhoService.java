package com.example.projetoPBD.pontoCerto.service;


import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.LocalDeTrabalho;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.LocalDeTrabalhoDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.LocalDeTrabalhoRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaCampoInvalidoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaNaoEncontradaException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LocalDeTrabalhoService {
    private final LocalDeTrabalhoRepository localDeTrabalhoRepository;
    private final EmpresaRepository empresaRepository;
    private final SecurityTenantContext securityTenantContext;

    public LocalDeTrabalhoService(
            LocalDeTrabalhoRepository localDeTrabalhoRepository,
            EmpresaRepository empresaRepository,
            SecurityTenantContext securityTenantContext
    ) {
        this.localDeTrabalhoRepository = localDeTrabalhoRepository;
        this.empresaRepository = empresaRepository;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional
    public LocalDeTrabalhoDTO.Response cadastrar(LocalDeTrabalhoDTO.Request dto){
        UUID empresaAtual = securityTenantContext.empresaAtual();
        if (dto.empresaId() != null && !dto.empresaId().equals(empresaAtual)) {
            throw new AcessoNegadoException("Acesso negado: não é permitido cadastrar local de trabalho em outra empresa.");
        }

        Empresa empresa = empresaRepository.findById(empresaAtual)
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Não encontrada"));

        LocalDeTrabalho local = new LocalDeTrabalho();
        local.setEmpresa(empresa);
        local.setNome(dto.nome() != null ? dto.nome().trim() : "");
        local.setLatitude(dto.latitude());
        local.setLongitude(dto.longitude());
        local.setRaioMetros(dto.raioMetros() != null ? dto.raioMetros() : 0);
        local.setIpEsperado(dto.ipEsperado() != null && !dto.ipEsperado().isBlank() ? dto.ipEsperado().trim() : null);
        local.setEndereco(dto.endereco() != null ? dto.endereco().trim() : "");
        local.setMunicipio(dto.municipio() != null ? dto.municipio().trim() : "");
        local.setUf(dto.uf() != null ? dto.uf().trim().toUpperCase() : "");

        local = localDeTrabalhoRepository.save(local);
        return new LocalDeTrabalhoDTO.Response(local);
    }

    @Transactional
    public List<LocalDeTrabalhoDTO.Response> listar(UUID empresaId) {
        UUID empresaAtual = securityTenantContext.empresaAtual();
        if (empresaId != null && !empresaId.equals(empresaAtual)) {
            throw new AcessoNegadoException("Acesso negado: você só pode listar locais de trabalho da sua própria empresa.");
        }

        List<LocalDeTrabalho> lista = localDeTrabalhoRepository.findByEmpresaId(empresaAtual);

        return lista.stream()
                .map(LocalDeTrabalhoDTO.Response::new)
                .toList();
    }
}
