package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Inconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusInconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoInconsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InconsistenciaRepository extends JpaRepository<Inconsistencia, UUID> {

    boolean existsByEmpresaIdAndChaveDeduplicacaoAndSituacao(
            UUID empresaId,
            String chaveDuplicacao,
            StatusInconsistencia situacao
    );

    boolean existsByEmpresaIdAndChaveDeduplicacao(
            UUID empresaId,
            String chaveDuplicacao
    );

    Optional<Inconsistencia> findFirstByBatidaOrigemIdAndTipoAndSituacao(
            UUID batidaOrigemId,
            TipoInconsistencia tipo,
            StatusInconsistencia situacao
    );

    List<Inconsistencia> findByEmpresaIdAndFuncionarioIdAndDataReferenciaAndTipoAndSituacao(
            UUID empresaId,
            UUID funcionarioId,
            LocalDate dataReferencia,
            TipoInconsistencia tipo,
            StatusInconsistencia situacao
    );

    @Query("SELECT i FROM Inconsistencia i " +
            "LEFT JOIN FETCH i.funcionario f " +
            "LEFT JOIN FETCH i.equipamento e " +
            "WHERE i.empresa.id = :empresaId " +
            "ORDER BY i.dataHoraOrigem DESC"
    )
    List<Inconsistencia> findByEmpresaIdOrderByDataHoraOrigemDesc(
            @Param("empresaId")
            UUID empresaId
    );

    @Query("SELECT i FROM Inconsistencia i " +
            "LEFT JOIN FETCH i.funcionario f " +
            "LEFT JOIN FETCH i.equipamento e " +
            "WHERE i.empresa.id = :empresaId " +
            "AND i.situacao = :situacao " +
            "AND i.dataReferencia BETWEEN :dataInicio AND :dataFim " +
            "ORDER BY i.dataHoraOrigem DESC"
    )
    List<Inconsistencia> findByEmpresaIdAndSituacaoAndDataReferenciaBetweenOrderByDataHoraOrigemDesc(
            @Param("empresaId") UUID empresaId,
            @Param("situacao") StatusInconsistencia situacao,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );

    long countByEmpresaIdAndSituacao(
            UUID empresaId,
            StatusInconsistencia situacao
    );

    long countByEmpresaIdAndSituacaoAndTipo(
            UUID empresaId,
            StatusInconsistencia situacao,
            TipoInconsistencia tipo
    );
}
