package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Estacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface EstacaoRepository extends JpaRepository<Estacao, UUID>, JpaSpecificationExecutor<Estacao> {

    Optional<Estacao> findByCredentialId(String credentialId);
}
