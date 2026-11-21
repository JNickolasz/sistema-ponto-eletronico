package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.HistoricoExportacaoFolha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistoricoExportacaoFolhaRepository extends JpaRepository<HistoricoExportacaoFolha, UUID> {

    List<HistoricoExportacaoFolha> findByEmpresaIdOrderByDataHoraDesc(UUID empresaId);

    List<HistoricoExportacaoFolha> findByEmpresaIdAndCompetenciaOrderByDataHoraDesc(UUID empresaId, String competencia);
}
