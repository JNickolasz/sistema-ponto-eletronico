package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.dto.EmpresaDTO;
import com.example.projetoPBD.pontoCerto.dto.FuncionarioDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaNaoEncontradaException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioDuplicadoException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmpresaRepository empresaRepository;

    public FuncionarioService(FuncionarioRepository repository, PasswordEncoder passwordEncoder, EmpresaRepository empresaRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.empresaRepository = empresaRepository;
    }

    public FuncionarioDTO.Response criar(FuncionarioDTO.Criar dto, String empresaCnpj) {
        // depois voltamos para o (repository.existsByUsuarioAndEmpresaId(dto.usuario(), empresaId))
        // por hora será apenas para cumprir o primeiro critério da T1
        if (repository.existsByUsuario(dto.usuario())) {
            throw new UsuarioDuplicadoException("Usuário já cadastrado!");
        }


        Empresa empresa = empresaRepository.findByCnpj(empresaCnpj)
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada."));


        Funcionario funcionario = new Funcionario();
        funcionario.setNomeCompleto(dto.nomeCompleto());
        funcionario.setUsuario(dto.usuario());
        funcionario.setSenhaHash(passwordEncoder.encode(dto.senha()));
        funcionario.setPerfilAcesso(dto.perfilAcesso());
        funcionario.setEmpresaId(empresa);

        Funcionario salvo = repository.save(funcionario);

        return new FuncionarioDTO.Response(
                salvo.getId(),
                salvo.getNomeCompleto(),
                salvo.getUsuario(),
                salvo.getPerfilAcesso()
        );
    }
}
