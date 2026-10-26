package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Batida;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusBatida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BatidaRepository extends JpaRepository<Batida, UUID> {

    @Query("SELECT b FROM Batida b " +
            "WHERE b.funcionario.id = :funcionarioId " +
            "AND (b.statusBatida IS NULL OR b.statusBatida <> com.example.projetoPBD.pontoCerto.domain.enums.StatusBatida.ORFA_PENDENTE) " +
            "ORDER BY b.dataHora DESC")
    List<Batida> findByFuncionarioIdOrderByDataHoraDesc(@Param("funcionarioId") UUID funcionarioId);

    @Query("SELECT COUNT(DISTINCT b.nsr) FROM Batida b")
    long countDistinctNsr();

    boolean existsByFuncionarioIdAndDataHora(UUID funcionarioId, LocalDateTime dataHora);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Batida b " +
            "WHERE b.identificadorOriginal = :identificadorOriginal " +
            "AND b.dataHora = :dataHora " +
            "AND b.equipamento.empresa.id = :empresaId")
    boolean existsOrfaByIdentificadorAndDataHoraAndEmpresaId(
            @Param("identificadorOriginal") String identificadorOriginal,
            @Param("dataHora") LocalDateTime dataHora,
            @Param("empresaId") UUID empresaId
    );

    @Query("SELECT b FROM Batida b " +
            "LEFT JOIN FETCH b.equipamento e " +
            "LEFT JOIN FETCH b.funcionario f " +
            "WHERE e.empresa.id = :empresaId " +
            "AND b.statusBatida = :status " +
            "ORDER BY b.dataHora DESC")
    List<Batida> findByEmpresaIdAndStatusBatidaOrderByDataHoraDesc(
            @Param("empresaId") UUID empresaId,
            @Param("status") StatusBatida status
    );
}
