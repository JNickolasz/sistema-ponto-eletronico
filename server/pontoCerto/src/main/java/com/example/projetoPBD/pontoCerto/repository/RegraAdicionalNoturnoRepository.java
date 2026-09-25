package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.RegraAdicionalNoturno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegraAdicionalNoturnoRepository extends JpaRepository<RegraAdicionalNoturno, UUID> {

    Optional<RegraAdicionalNoturno> findByRegraApuracaoId(UUID regraApuracaoId);
}
