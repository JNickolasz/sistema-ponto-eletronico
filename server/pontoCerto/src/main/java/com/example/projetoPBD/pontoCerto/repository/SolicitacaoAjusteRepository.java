package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.SolicitacaoAjuste;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusSolicitacaoAjuste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SolicitacaoAjusteRepository extends JpaRepository<SolicitacaoAjuste, UUID> {

    List<SolicitacaoAjuste> findByColaboradorIdOrderBySolicitadoEmDesc(UUID colaboradorId);

    List<SolicitacaoAjuste> findByColaboradorGestorIdAndStatusOrderBySolicitadoEmDesc(UUID gestorId, StatusSolicitacaoAjuste status);

    List<SolicitacaoAjuste> findByColaboradorGestorIdOrderBySolicitadoEmDesc(UUID gestorId);

}