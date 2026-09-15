package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EquipamentoRepository extends JpaRepository<Equipamento, UUID> {

    boolean existsByEmpresaIdAndNumFabricacao(UUID empresaId, String numFabricacao);

    List<Equipamento> findByEmpresaId(UUID empresaId);

    List<Equipamento> findByLocalTrabalhoId(UUID localTrabalhoId);

}
