package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Batida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BatidaRepository extends JpaRepository<Batida, UUID> {

    List<Batida> findByFuncionarioIdOrderByDataHoraDesc(UUID funcionarioId);

    @Query("SELECT COUNT(DISTINCT b.nsr) FROM Batida b")
    long countDistinctNsr();

    boolean existsByFuncionarioIdAndDataHora(UUID funcionarioId, LocalDateTime dataHora);
}
