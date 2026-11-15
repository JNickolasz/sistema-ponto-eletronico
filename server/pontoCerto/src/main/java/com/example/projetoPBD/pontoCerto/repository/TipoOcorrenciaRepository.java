package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.TipoOcorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipoOcorrenciaRepository extends JpaRepository<TipoOcorrencia, UUID> {

    List<TipoOcorrencia> findByEmpresaIdAndAtivoTrueOrderByNomeAsc(UUID empresaId);

    List<TipoOcorrencia> findByEmpresaIdOrderByNomeAsc(UUID empresaId);

    Optional<TipoOcorrencia> findByIdAndEmpresaId(UUID id, UUID empresaId);

    boolean existsByEmpresaIdAndNomeIgnoreCase(UUID empresaId, String nome);

    boolean existsByEmpresaIdAndNomeIgnoreCaseAndIdNot(UUID empresaId, String nome, UUID id);
}