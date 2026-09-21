package com.example.projetoPBD.pontoCerto.repository.specification;

import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class EquipamentoSpecification {

    private EquipamentoSpecification() {}

    public static Specification<Equipamento> pertenceaEmpresa(UUID empresaId){
        return (root, query, cb) -> cb.equal(root.get("empresa").get("id"), empresaId);
    }

    public static Specification<Equipamento> pertenceAoLocalDeTrabalho(UUID localTrabalhoId) {
        return (root, query, cb) ->
                cb.equal(root.get("localTrabalho"), localTrabalhoId);
    }

    public static Specification<Equipamento> possuiTipoEquipamento(TipoEquipamento tipoEquipamento) {
        return (root, query, cb) ->
                cb.equal(root.get("tipo"), tipoEquipamento);
    }

    public static Specification<Equipamento> possuiStatusEquipamento(StatusEquipamento statusEquipamento) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), statusEquipamento);
    }

}
