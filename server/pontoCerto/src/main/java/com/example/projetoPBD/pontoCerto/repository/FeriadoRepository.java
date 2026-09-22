package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Alcance;
import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeriadoRepository extends JpaRepository<Feriado, UUID> {

    @Query("""
            SELECT COUNT(f) > 0
            FROM Feriado f
            WHERE f.ativo = true
              AND f.data = :data
              AND (:idExcluir IS NULL OR f.id != :idExcluir)
              AND (
                  (f.alcance = 'NACIONAL' AND f.empresa IS NULL)
                  OR
                  (
                      f.alcance = :alcance
                      AND (:uf IS NULL OR UPPER(f.uf) = UPPER(:uf))
                      AND (:municipio IS NULL OR LOWER(TRIM(f.municipio)) = LOWER(TRIM(:municipio)))
                      AND (
                          (:empresa IS NULL AND f.empresa IS NULL)
                          OR (f.empresa = :empresa)
                      )
                  )
              )
            """)
    boolean existsFeriadoConflitante(
            @Param("data") LocalDate data,
            @Param("alcance") Alcance alcance,
            @Param("uf") String uf,
            @Param("municipio") String municipio,
            @Param("empresa") Empresa empresa,
            @Param("idExcluir") UUID idExcluir
    );

    @Query("""
            SELECT f
            FROM Feriado f
            WHERE f.ativo = true
              AND YEAR(f.data) = :ano
              AND (f.empresa IS NULL OR (:empresaId IS NOT NULL AND f.empresa.id = :empresaId))
              AND (
                  f.alcance = 'NACIONAL'
                  OR (f.alcance = 'ESTADUAL' AND UPPER(f.uf) = UPPER(:uf))
                  OR (f.alcance = 'MUNICIPAL' AND UPPER(f.uf) = UPPER(:uf) AND LOWER(TRIM(f.municipio)) = LOWER(TRIM(:municipio)))
              )
            """)
    List<Feriado> buscarCalendarioAnual(
            @Param("ano") int ano,
            @Param("uf") String uf,
            @Param("municipio") String municipio,
            @Param("empresaId") UUID empresaId
    );
}