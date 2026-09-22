package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.JornadaDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JornadaDiaRepository extends JpaRepository<JornadaDia, UUID> {
}
