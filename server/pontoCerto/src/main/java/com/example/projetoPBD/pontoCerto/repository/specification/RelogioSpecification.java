package com.example.projetoPBD.pontoCerto.repository.specification;

import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Relogio;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

public final class RelogioSpecification {

    private RelogioSpecification() {}

    public static Specification<Equipamento> possuiNumFabricante(String numFabricante){
        return (root, query, cb) -> {

            Root<Relogio> relogio = cb.treat(root, Relogio.class);

            return cb.and(cb.equal(root.type(), Relogio.class), cb.equal(relogio.get("numFabricante"), numFabricante));
        };
    }

    public static Specification<Equipamento> possuiLinhasImportadas(Long quantidade){
        return (root, query, cb)  -> {

            Root<Relogio> relogio = cb.treat(root, Relogio.class);

            return cb.and(cb.equal(root.type(), Relogio.class), cb.equal(relogio.get("linhasImportadas"), quantidade));

        };
    }

}
