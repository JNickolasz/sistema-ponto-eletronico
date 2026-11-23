package com.example.projetoPBD.pontoCerto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "admin_ip_bloqueio",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_admin_ip_bloqueio_ip", columnNames = {"ip"})
        }
)
public class AdminIpBloqueio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ip", nullable = false, length = 64)
    private String ip;

    @Column(name = "falhas_consecutivas", nullable = false)
    private Integer falhasConsecutivas = 0;

    /**
     * Nível de escalonamento da penalidade:
     * 0 = Sem bloqueio prévio (5 tentativas permitidas)
     * 1 = Bloqueado por minutos (5 min)
     * 2 = Bloqueado por horas (2 h)
     * 3 = Bloqueado por dias (3 dias)
     * 4 = Bloqueado por mês (30 dias)
     * 5 = Bloqueio permanente (desbloqueio apenas via SQL no banco)
     */
    @Column(name = "nivel_penalidade", nullable = false)
    private Integer nivelPenalidade = 0;

    @Column(name = "total_bloqueios", nullable = false)
    private Integer totalBloqueios = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @Column(name = "bloqueado_permanente", nullable = false)
    private boolean bloqueadoPermanente = false;

    /**
     * Flag de controle administrativo: definir ativo = false no banco libera imediatamente o IP.
     */
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "ultima_tentativa_em")
    private LocalDateTime ultimaTentativaEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
