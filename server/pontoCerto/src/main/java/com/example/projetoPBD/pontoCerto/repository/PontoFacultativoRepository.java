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

    // Valida se a data repetida no mesmo alcance/local já existe
    boolean existsByDataAndAlcanceAndUfAndMunicipio(LocalDate data, Alcance alcance, String uf, String municipio);

    // Consulta do calendário anual
    @Query("SELECT p FROM PontoFacultativo p WHERE YEAR(p.data) = :ano " +
            "AND (p.alcance = 'NACIONAL' " +
            "OR (p.alcance = 'ESTADUAL' AND p.uf = :uf) " +
            "OR (p.alcance = 'MUNICIPAL' AND p.uf = :uf AND p.municipio = :municipio))")
    List<PontoFacultativo> buscarCalendarioAnual(@Param("ano") int ano, @Param("uf") String uf, @Param("municipio") String municipio);

}
