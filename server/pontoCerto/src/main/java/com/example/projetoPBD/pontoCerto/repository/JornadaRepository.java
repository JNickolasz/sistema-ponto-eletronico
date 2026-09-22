package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Jornada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JornadaRepository extends JpaRepository<Jornada, UUID> {
    List<Jornada> findByEmpresaId(UUID empresaId);
    Optional<Jornada> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
