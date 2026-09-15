package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.LocalDeTrabalho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LocalDeTrabalhoRepository extends JpaRepository<LocalDeTrabalho, UUID> {
    List<LocalDeTrabalho> findByEmpresaId(UUID empresaId);
}
