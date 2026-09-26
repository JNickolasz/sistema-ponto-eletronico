package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import com.example.projetoPBD.pontoCerto.dto.RegraApuracaoConsultaDTO;
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
    SELECT new com.example.projetoPBD.pontoCerto.dto.RegraApuracaoConsultaDTO(
        r,
        LEAD(r.inicioVigencia) OVER (
            ORDER BY r.inicioVigencia
        ),
        CASE
            WHEN r.inicioVigencia > :dataReferencia
                THEN 'AGENDADA'

            WHEN LEAD(r.inicioVigencia) OVER (
                ORDER BY r.inicioVigencia
            ) <= :hoje
                THEN 'ENCERRADA'

            ELSE 'VIGENTE'
        END
    )
    FROM RegraApuracao r
    WHERE r.empresa.id = :empresaId
    ORDER BY r.inicioVigencia
""")
    List<RegraApuracaoConsultaDTO> listVigenciasAndRegraApuracaoStatus(UUID empresaId, LocalDate dataReferencia);


    List<RegraApuracao> findAllByEmpresaId(UUID empresaId);
}