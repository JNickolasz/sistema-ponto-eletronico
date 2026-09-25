package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import com.example.projetoPBD.pontoCerto.dto.projection.RegraApuracaoProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegraApuracaoRepository extends JpaRepository<RegraApuracao, UUID> {

    Optional<RegraApuracao> findFirstByEmpresaIdAndInicioVigenciaLessThanEqualOrderByInicioVigenciaDesc(UUID empresa, LocalDate inicioVigencia);

    @Query(value = """ 
                SELECT r.*,
                       LEAD(inicio_vigencia) OVER (ORDER BY inicio_vigencia) - 1 AS fim_vigencia,
                       CASE
                         WHEN inicio_vigencia > :dataReferencia THEN 'AGENDADA'
                         WHEN LEAD(inicio_vigencia) OVER (ORDER BY inicio_vigencia) <= :hoje THEN 'ENCERRADA'
                         ELSE 'VIGENTE'
                       END AS situacao
                FROM regras_apuracao r
                WHERE empresa_id = :empresaId
                ORDER BY inicio_vigencia;
""", nativeQuery = true)
    List<RegraApuracaoProjection> listAllRulesByEmpresaAndDiaRefeencia(UUID empresaId, LocalDate dataReferencia);

}