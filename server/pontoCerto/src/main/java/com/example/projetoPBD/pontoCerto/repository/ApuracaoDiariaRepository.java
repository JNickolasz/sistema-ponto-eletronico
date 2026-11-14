package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.ApuracaoDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApuracaoDiariaRepository extends JpaRepository<ApuracaoDiaria, UUID> {

    Optional<ApuracaoDiaria> findByFuncionarioIdAndDataReferencia(UUID funcionarioId, LocalDate dataReferencia);

    List<ApuracaoDiaria> findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAsc(
            UUID funcionarioId,
            LocalDate dataInicio,
            LocalDate dataFim
    );

    @Query("SELECT a FROM ApuracaoDiaria a " +
            "JOIN FETCH a.funcionario f " +
            "WHERE f.empresa.id = :empresaId " +
            "AND a.dataReferencia BETWEEN :dataInicio AND :dataFim " +
            "ORDER BY a.dataReferencia ASC, f.nomeCompleto ASC")
    List<ApuracaoDiaria> findByEmpresaIdAndDataReferenciaBetweenOrderByDataReferenciaAsc(
            @Param("empresaId") UUID empresaId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );

    void deleteByFuncionarioIdAndDataReferencia(UUID funcionarioId, LocalDate dataReferencia);
}
