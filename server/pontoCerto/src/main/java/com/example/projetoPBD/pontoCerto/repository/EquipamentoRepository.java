package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EquipamentoRepository extends JpaRepository<Equipamento, UUID>, JpaSpecificationExecutor<Equipamento> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Equipamento e WHERE e.id = :id")
    Optional<Equipamento> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByEmpresaIdAndCodigo(UUID empresaId, String codigo);

    Optional<Equipamento> findByEmpresaIdAndCodigo(UUID empresaId, String codigo);

    List<Equipamento> findByEmpresaIdAndTipo(UUID empresaId, TipoEquipamento tipo);

    List<Equipamento> findByEmpresaId(UUID empresaId);

    List<Equipamento> findByLocalTrabalhoId(UUID localTrabalhoId);

}
