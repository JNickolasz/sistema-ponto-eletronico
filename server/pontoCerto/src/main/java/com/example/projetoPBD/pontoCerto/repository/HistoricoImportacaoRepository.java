package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.HistoricoImportacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HistoricoImportacaoRepository extends JpaRepository<HistoricoImportacao, UUID> {

    @Query("SELECT h FROM HistoricoImportacao h " +
            "JOIN FETCH h.equipamento " +
            "LEFT JOIN FETCH h.autor " +
            "WHERE h.equipamento.empresa.id = :empresaId " +
            "ORDER BY h.dataImportacao DESC")
    List<HistoricoImportacao> findByEmpresaIdOrderByDAtaImportacaoDesc(@Param("empresaId") UUID empresaId);
}
