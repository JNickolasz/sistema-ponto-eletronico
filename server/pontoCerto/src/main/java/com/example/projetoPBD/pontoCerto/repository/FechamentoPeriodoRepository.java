package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.FechamentoPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FechamentoPeriodoRepository extends JpaRepository<FechamentoPeriodo, UUID> {

    List<FechamentoPeriodo> findByEmpresaIdOrderByDataFechamentoDesc(
            UUID empresaId
    );

    Optional<FechamentoPeriodo> findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(
            UUID empresaId,
            String mesReferencia
    );
}
