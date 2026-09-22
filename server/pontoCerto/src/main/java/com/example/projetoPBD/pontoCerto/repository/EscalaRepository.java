package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Escala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EscalaRepository extends JpaRepository<Escala, UUID> {
    List<Escala> findByEmpresaId(UUID empresaId);
    Optional<Escala> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
