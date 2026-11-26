package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.enums.Alcance;
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
              AND (
                  (p.alcance = 'NACIONAL' AND p.empresa IS NULL)
                  OR (
                      p.alcance = :alcance
                      AND (:uf IS NULL OR UPPER(p.uf) = UPPER(:uf))
                      AND (:municipio IS NULL OR LOWER(TRIM(p.municipio)) = LOWER(TRIM(:municipio)))
                      AND (
                          (:empresa IS NULL AND p.empresa IS NULL)
                          OR (p.empresa = :empresa)
                      )
                  )
              )
            """)
    boolean existsPontoFacultativoConflitante(
            @Param("data") LocalDate data,
            @Param("alcance") Alcance alcance,
            @Param("uf") String uf,
            @Param("municipio") String municipio,
            @Param("empresa") Empresa empresa,
            @Param("idExcluir") UUID idExcluir
    );

    @Query("""
            SELECT p
            FROM PontoFacultativo p
            WHERE p.ativo = true
              AND YEAR(p.data) = :ano
              AND (p.empresa IS NULL OR (:empresaId IS NOT NULL AND p.empresa.id = :empresaId))
              AND (
                  p.alcance = 'NACIONAL'
                  OR (p.alcance = 'ESTADUAL' AND (:uf IS NULL OR UPPER(p.uf) = UPPER(:uf)))
                  OR (
                      p.alcance = 'MUNICIPAL'
                      AND (:uf IS NULL OR UPPER(p.uf) = UPPER(:uf))
                      AND (:municipio IS NULL OR LOWER(TRIM(p.municipio)) = LOWER(TRIM(:municipio)))
                  )
              )
            ORDER BY p.data ASC
            """)
    List<PontoFacultativo> buscarCalendarioAnual(
            @Param("ano") int ano,
            @Param("uf") String uf,
            @Param("municipio") String municipio,
            @Param("empresaId") UUID empresaId
    );

    @Query("""
            SELECT p
            FROM PontoFacultativo p
            WHERE p.ativo = true
              AND (p.empresa IS NULL OR (:empresaId IS NOT NULL AND p.empresa.id = :empresaId))
            ORDER BY p.data ASC
            """)
    List<PontoFacultativo> listarAtivosPorEmpresaOuGlobais(@Param("empresaId") UUID empresaId);
}