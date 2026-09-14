package com.example.projetoPBD.pontoCerto.service;


import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.dto.EmpresaDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaExistenteException;
import com.example.projetoPBD.pontoCerto.service.exceptions.ErroCadastroEmpresaException;
import org.springframework.stereotype.Service;

@Service
public class EmpresaService {
    private final EmpresaRepository empresaRepository;


    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;

    }

    public EmpresaDTO.Response cadastrarEmpresa(EmpresaDTO.Criar empresaDTO) {

        if (empresaRepository.existsByCnpj(empresaDTO.cnpj())) {
            throw new EmpresaExistenteException("CNPJ já cadastrado");
        }

        if (empresaRepository.existsBySubDominio(empresaDTO.subdominio())){
            throw new EmpresaExistenteException("Subdomínio já cadastrado");
        }


        Empresa empresa = new Empresa();
        empresa.setCnpj(empresaDTO.cnpj());
        empresa.setRazaoSocial(empresaDTO.razaoSocial());
        empresa.setEndereco(empresaDTO.endereco());
        empresa.setSubDominio(empresaDTO.subdominio());

        empresa.setLogoUrl(
                empresaDTO.logoUrl() != null ? empresaDTO.logoUrl() : ""
        );

        Empresa empresaSalva = empresaRepository.save(empresa);

        return new EmpresaDTO.Response(
                empresaSalva.getId(),
                empresaSalva.getRazaoSocial()
        );

    }
}
