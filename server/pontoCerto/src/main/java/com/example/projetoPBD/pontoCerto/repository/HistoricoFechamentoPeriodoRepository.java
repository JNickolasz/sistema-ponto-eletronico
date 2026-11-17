package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.HistoricoFechamentoPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistoricoFechamentoPeriodoRepository extends JpaRepository<HistoricoFechamentoPeriodo, UUID> {

    List<HistoricoFechamentoPeriodo> findByFechamentoPeriodoIdOrderByDataHoraDesc(UUID fechamentoPeriodoId);

    List<HistoricoFechamentoPeriodo> findByFechamentoPeriodoEmpresaIdOrderByDataHoraDesc(UUID empresaId);
}
