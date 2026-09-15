package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FuncionarioDTO;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioDuplicadoException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

@Service
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public FuncionarioService(FuncionarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;

    }

    public FuncionarioDTO.Response criar(FuncionarioDTO.Criar dto) {


        if (repository.existsByUsuario(dto.usuario())) {
            throw new UsuarioDuplicadoException("Usuário já cadastrado!");
        }

        Funcionario funcionario = new Funcionario();
        funcionario.setNomeCompleto(dto.nomeCompleto());
        funcionario.setUsuario(dto.usuario());
        funcionario.setSenhaHash(passwordEncoder.encode(dto.senha()));
        funcionario.setPerfilAcesso(dto.perfilAcesso());

        Funcionario salvo = repository.save(funcionario);

        return new FuncionarioDTO.Response(
                salvo.getId(),
                salvo.getNomeCompleto(),
                salvo.getUsuario(),
                salvo.getPerfilAcesso()
        );
    }
}
