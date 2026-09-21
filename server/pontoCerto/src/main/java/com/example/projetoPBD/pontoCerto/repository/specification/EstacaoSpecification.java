package com.example.projetoPBD.pontoCerto.repository.specification;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Estacao;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusEstacao;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

public final class EstacaoSpecification {

    private EstacaoSpecification() {}

    public static Specification<Equipamento> statusEstacao(StatusEstacao statusEstacao) {
        return (root, query, cb) -> {

            Root<Estacao> estacao = cb.treat(root, Estacao.class);

            return cb.and(cb.equal(root.type(), Estacao.class), cb.equal(estacao.get("statusEstacao"), statusEstacao));
        };
    }

}
