package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {

    Optional<Funcionario> findByUsuarioAndEmpresaId(String usuario, Empresa empresaId);
    boolean existsByUsuarioAndEmpresaId(String usuario, Empresa empresaId);

    Optional<Funcionario> findByUsuario(String usuario);
    boolean existsByUsuario(String usuario);
}
