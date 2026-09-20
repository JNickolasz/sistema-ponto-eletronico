package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusEstacao;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "estacoes")
public class Estacao extends Equipamento{

    public Estacao(){
        super(TipoEquipamento.ESTACAO);
    }

    @Column(name = "credential_id")
    private String credential_id;

    @Column(name = "public_key")
    private String public_key;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusEstacao status =  StatusEstacao.PENDENTE;

    @Column(name = "revoked_at")
    private Timestamp revoked_at;

    @ManyToOne
    @JoinColumn(name = "revoked_by_id")
    private Funcionario revoked_by;

}
