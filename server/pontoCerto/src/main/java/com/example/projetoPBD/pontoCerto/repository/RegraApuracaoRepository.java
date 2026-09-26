package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import com.example.projetoPBD.pontoCerto.dto.RegraApuracaoConsulta;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RegraApuracaoDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegraApuracaoRepository extends JpaRepository<RegraApuracao, UUID> {

    Optional<RegraApuracao> findFirstByEmpresaIdAndInicioVigenciaLessThanEqualOrderByInicioVigenciaDesc(UUID empresa, LocalDate inicioVigencia);

    @Query("""
    SELECT new com.example.projetoPBD.pontoCerto.dto.RegraApuracaoConsulta(
        r,

        LEAD(r.inicioVigencia) OVER (
            ORDER BY r.inicioVigencia
        ),

        CASE
            WHEN r.inicioVigencia > :dataReferencia
                THEN 'AGENDADA'

            WHEN LEAD(r.inicioVigencia) OVER (
                ORDER BY r.inicioVigencia
            ) <= :dataReferencia
                THEN 'ENCERRADA'

            ELSE 'VIGENTE'
        END
    )
    FROM RegraApuracao r
    WHERE r.empresa.id = :empresaId
    ORDER BY r.inicioVigencia
""")
    List<RegraApuracaoConsulta> listVigenciasAndRegraApuracaoStatus(
            UUID empresaId,
            LocalDate dataReferencia
    );





    @Query("""
    SELECT new com.example.projetoPBD.pontoCerto.dto.RegraApuracaoConsulta(
        r,

        (
            SELECT MIN(r2.inicioVigencia)
            FROM RegraApuracao r2
            WHERE r2.empresa.id = r.empresa.id
              AND r2.inicioVigencia > r.inicioVigencia
        ),

        CASE
            WHEN r.inicioVigencia > :dataReferencia
                THEN 'AGENDADA'

            WHEN (
                SELECT MIN(r2.inicioVigencia)
                FROM RegraApuracao r2
                WHERE r2.empresa.id = r.empresa.id
                  AND r2.inicioVigencia > r.inicioVigencia
            ) <= :dataReferencia
                THEN 'ENCERRADA'

            ELSE 'VIGENTE'
        END
    )
    FROM RegraApuracao r
    WHERE r.id = :regraApuracaoId
      AND r.empresa.id = :empresaId
""")
    Optional<RegraApuracaoConsulta> findConsultaById(
            UUID regraApuracaoId,
            UUID empresaId,
            LocalDate dataReferencia
    );


    boolean existsByEmpresaIdAndInicioVigencia(UUID empresaId, LocalDate diaReferencia);
}