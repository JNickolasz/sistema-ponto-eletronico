package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.MovimentacaoBancoHoras;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoMovimentacaoBancoHoras;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovimentacaoBancoHorasRepository extends JpaRepository<MovimentacaoBancoHoras, UUID> {

    List<MovimentacaoBancoHoras> findByApuracaoDiariaId(UUID apuracaoDiariaId);

    Optional<MovimentacaoBancoHoras> findByApuracaoDiariaIdAndTipo(UUID apuracaoDiariaId, TipoMovimentacaoBancoHoras tipo);

    void deleteByApuracaoDiariaId(UUID apuracaoDiariaId);

    List<MovimentacaoBancoHoras> findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAscRegistradoEmAsc(
            UUID funcionarioId,
            LocalDate dataInicio,
            LocalDate dataFim
    );

    List<MovimentacaoBancoHoras> findByFuncionarioIdAndDataReferenciaLessThanOrderByDataReferenciaAscRegistradoEmAsc(
            UUID funcionarioId,
            LocalDate dataLimite
    );

    @Query("""
        SELECT COALESCE(SUM(m.minutos), 0)
        FROM MovimentacaoBancoHoras m
        WHERE m.funcionario.id = :funcionarioId
          AND m.dataReferencia < :dataLimite
    """)
    Integer somarSaldoAnterior(
            @Param("funcionarioId") UUID funcionarioId,
            @Param("dataLimite") LocalDate dataLimite
    );

    @Query("""
        SELECT COALESCE(SUM(m.minutos), 0)
        FROM MovimentacaoBancoHoras m
        WHERE m.funcionario.id = :funcionarioId
          AND m.dataReferencia >= :dataInicio
          AND m.dataReferencia < :dataLimite
    """)
    Integer somarSaldoEntre(
            @Param("funcionarioId") UUID funcionarioId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataLimite") LocalDate dataLimite
    );
}
