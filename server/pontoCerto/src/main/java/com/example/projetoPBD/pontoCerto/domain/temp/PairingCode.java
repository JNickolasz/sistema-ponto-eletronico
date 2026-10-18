package com.example.projetoPBD.pontoCerto.domain.temp;

import com.example.projetoPBD.pontoCerto.domain.Estacao;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusPairingCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "pairing_codes")
public class PairingCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pairing_code_hash", nullable = false)
    private String pairingCodeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusPairingCode status = StatusPairingCode.VALIDO;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    private Estacao estacao;


    public PairingCode(String pairingCodeHash, Instant expiresAt, Estacao estacao) {
        this.pairingCodeHash = pairingCodeHash;
        this.expiresAt = expiresAt;
        this.estacao = estacao;
    }

    public PairingCode() {

    }
}
