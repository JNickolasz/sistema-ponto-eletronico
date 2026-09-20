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

    boolean existsByDataAndAlcanceAndUfAndMunicipio(LocalDate data, Alcance alcance, String uf, String Municipio);
    // Valida duplicidade na mesma data, alcance e localidade

    boolean existsByDataAndAlcanceAndUfAndMunicipioAndEmpresa(
            LocalDate data, Alcance alcance, String uf, String municipio, Empresa empresa);


    //Consulta do calendário anual pelo que foi cadastrado

    @Query("SELECT f FROM Feriado f WHERE YEAR(f.data) = :ano " +
            "AND (f.alcance = 'NACIONAL' " +
            "OR (f.alcance = 'ESTADUAL' AND f.uf = :uf) " +
            "OR (f.alcance = 'MUNICIPAL' AND f.uf = :uf AND f.municipio = :municipio))")
    List<Feriado> buscarCalendarioAnual(@Param("ano") int ano, @Param("uf") String uf, @Param("municipio") String municipio);

}
