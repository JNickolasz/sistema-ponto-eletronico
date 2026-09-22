package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Alcance;
import com.example.projetoPBD.pontoCerto.domain.PontoFacultativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PontoFacultativoRepository extends JpaRepository<PontoFacultativo, UUID> {

    @Query("""
            SELECT COUNT(p) > 0
            FROM PontoFacultativo p
            WHERE p.ativo = true
              AND p.data = :data
              AND (:idExcluir IS NULL OR p.id != :idExcluir)
              AND p.alcance = :alcance
              AND (:uf IS NULL OR UPPER(p.uf) = UPPER(:uf))
              AND (:municipio IS NULL OR LOWER(TRIM(p.municipio)) = LOWER(TRIM(:municipio)))
            """)

        // Valida se a data repetida no mesmo alcance/local já existe
    boolean existsPontoFacultativoConflitante(
            @Param("data") LocalDate data,
            @Param("alcance") Alcance alcance,
            @Param("uf") String uf,
            @Param("municipio") String municipio,
            @Param("idExcluir") UUID idExcluir
    );

    @Query("""
            SELECT p
            FROM PontoFacultativo p
            WHERE p.ativo = true
              AND YEAR(p.data) = :ano
              AND (
                  p.alcance = 'NACIONAL'
                  OR (p.alcance = 'ESTADUAL' AND UPPER(p.uf) = UPPER(:uf))
                  OR (p.alcance = 'MUNICIPAL' AND UPPER(p.uf) = UPPER(:uf) AND LOWER(TRIM(p.municipio)) = LOWER(TRIM(:municipio)))
              )
            """)

        // Consulta do calendário anual
    List<PontoFacultativo> buscarCalendarioAnual(
            @Param("ano") int ano,
            @Param("uf") String uf,
            @Param("municipio") String municipio
    );
}