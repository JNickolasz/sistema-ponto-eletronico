package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.dto.FuncionarioDTO;
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

    public FuncionarioDTO.Response criar(FuncionarioDTO.Criar dto, Long empresaId) {
        if (repository.existsByUsuarioAndEmpresaId(dto.usuario(), empresaId)) {
            throw new UsuarioDuplicadoException("Usuário já cadastrado nessa empresa!");
        }

        /*
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada."));
        funcionario.setEmpresa(empresa);
         ^
         | Tem q ver como prosseguir aqui, já q no T1 n pede nada sobre a Empresa
         L pode por alguns campos como nullable = true só pra funcionar
           por enquanto. Depois a gnt conserta.
         */

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
