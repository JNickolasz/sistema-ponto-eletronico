package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Relogio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface RelogioRepository extends JpaRepository<Relogio, UUID>, JpaSpecificationExecutor<Relogio> {

    @Query("""
        SELECT COUNT(r) > 0
        FROM Relogio r
        WHERE r.numFabricante = :numeroFabricacao
          AND r.empresa.id = :empresaId
    """)
    boolean existsByNumeroFabricacaoAndEmpresaId(
            String numeroFabricacao,
            UUID empresaId
    );

}
