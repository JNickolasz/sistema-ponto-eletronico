package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.FechamentoBancoHoras;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FechamentoBancoHorasRepository extends JpaRepository<FechamentoBancoHoras, UUID> {

    Optional<FechamentoBancoHoras> findByFuncionarioIdAndMesReferencia(UUID funcionarioId, String mesReferencia);

    List<FechamentoBancoHoras> findByFuncionarioIdOrderByMesReferenciaAsc(UUID funcionarioId);

    Optional<FechamentoBancoHoras> findFirstByFuncionarioIdAndMesReferenciaLessThanOrderByMesReferenciaDesc(
            UUID funcionarioId,
            String mesReferenciaLimite
    );
}
