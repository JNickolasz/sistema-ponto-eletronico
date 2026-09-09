package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    Optional<Funcionario> findByUsuarioAndEmpresaId(String usuario, Long empresaId);

    boolean existsByUsuarioAndEmpresaId(String usuario, Long empresaId);

    Optional<Funcionario> findByUsuario(String usuario);
}
