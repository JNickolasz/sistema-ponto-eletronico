package com.example.projetoPBD.pontoCerto.service;


import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.dto.EmpresaDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaExistenteException;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmpresaService {
    private final EmpresaRepository empresaRepository;


    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;

    }

    public EmpresaDTO.Response cadastrarEmpresa(EmpresaDTO.Request dto) {

        if (empresaRepository.existsByCnpj(dto.cnpj())) {
            throw new EmpresaExistenteException("Empresa já existente");
        }

        Empresa empresa = new Empresa();
        empresa.setCnpj(dto.cnpj());
        empresa.setRazaoSocial(dto.razaoSocial());

        Empresa empresaSalva = empresaRepository.save(empresa);

        return new EmpresaDTO.Response(
                empresaSalva.getId(),
                empresaSalva.getRazaoSocial()
        );
    }
}
