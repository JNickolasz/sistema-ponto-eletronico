package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.RegimeTrabalho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegimeTrabalhoRepository extends JpaRepository<RegimeTrabalho, UUID> {

    List<RegimeTrabalho> findByFuncionarioIdOrderByDataInicioVigenciaDesc(UUID funcionarioId);

    @Query("""
            SELECT r FROM RegimeTrabalho r
            WHERE r.funcionario.id = :funcionarioId
              AND r.dataInicioVigencia <= :data
              AND (r.dataFimVigencia IS NULL OR r.dataFimVigencia >= :data)
        """)
    Optional<RegimeTrabalho> findVigenteNaData(
            @Param("funcionarioId") UUID funcionarioId,
            @Param("data") LocalDate data
    );

    @Query("""
            SELECT r FROM RegimeTrabalho r
            WHERE r.funcionario.id = :funcionarioId
              AND (:dataFim IS NULL OR r.dataInicioVigencia <= :dataFim)
              AND (r.dataFimVigencia IS NULL OR r.dataFimVigencia >= :dataInicio)
        """)
    List<RegimeTrabalho> findSobrepostos(
            @Param("funcionarioId") UUID funcionarioId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );
}