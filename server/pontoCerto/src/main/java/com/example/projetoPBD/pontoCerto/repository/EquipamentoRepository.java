package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EquipamentoRepository extends JpaRepository<Equipamento, UUID>, JpaSpecificationExecutor<Equipamento> {

    boolean existsByEmpresaIdAndCodigo(UUID empresaId, String codigo);

    Optional<Equipamento> findByEmpresaIdAndCodigo(UUID empresaId, String codigo);

    List<Equipamento> findByEmpresaIdAndTipo(UUID empresaId, TipoEquipamento tipo);

    List<Equipamento> findByEmpresaId(UUID empresaId);

    List<Equipamento> findByLocalTrabalhoId(UUID localTrabalhoId);

}
