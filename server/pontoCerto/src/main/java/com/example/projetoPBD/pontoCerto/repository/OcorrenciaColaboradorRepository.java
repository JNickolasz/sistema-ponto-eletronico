package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.OcorrenciaColaborador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OcorrenciaColaboradorRepository extends JpaRepository<OcorrenciaColaborador, UUID> {

    @Query("SELECT o FROM OcorrenciaColaborador o " +
            "WHERE o.funcionario.id = :funcionarioId " +
            "AND o.dataInicio <= :dataFim " +
            "AND o.dataFim >= :dataInicio")
    List<OcorrenciaColaborador> findSobrepostas(
            @Param("funcionarioId") UUID funcionarioId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );

    @Query("SELECT o FROM OcorrenciaColaborador o " +
            "JOIN FETCH o.tipoOcorrencia t " +
            "WHERE o.funcionario.id = :funcionarioId " +
            "AND :dataReferencia BETWEEN o.dataInicio AND o.dataFim")
    Optional<OcorrenciaColaborador> findVigenteNaData(
            @Param("funcionarioId") UUID funcionarioId,
            @Param("dataReferencia") LocalDate dataReferencia
    );

    @Query("SELECT o FROM OcorrenciaColaborador o " +
            "JOIN FETCH o.tipoOcorrencia t " +
            "JOIN FETCH o.funcionario f " +
            "WHERE f.empresa.id = :empresaId " +
            "AND (:funcionarioId IS NULL OR f.id = :funcionarioId) " +
            "AND (cast(:dataInicio as date) IS NULL OR o.dataFim >= :dataInicio) " +
            "AND (cast(:dataFim as date) IS NULL OR o.dataInicio <= :dataFim) " +
            "ORDER BY o.dataInicio DESC")
    List<OcorrenciaColaborador> findFiltradas(
            @Param("empresaId") UUID empresaId,
            @Param("funcionarioId") UUID funcionarioId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );
}
