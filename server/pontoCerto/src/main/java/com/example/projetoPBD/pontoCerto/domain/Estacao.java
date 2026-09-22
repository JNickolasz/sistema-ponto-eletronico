package com.example.projetoPBD.pontoCerto.domain;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
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
        super(TipoEquipamento.ESTACAO, StatusEquipamento.INATIVO);
    }

    @Column(name = "credential_id")
    private String credentialId;

    @Column(name = "public_key")
    private String publicKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_estacao", nullable = false)
    private StatusEstacao statusEstacao = StatusEstacao.PENDENTE;

    @Column(name = "revoked_at")
    private Timestamp revokedAt;

    @ManyToOne
    @JoinColumn(name = "revoked_by_id")
    private Funcionario revokedBy;

}
