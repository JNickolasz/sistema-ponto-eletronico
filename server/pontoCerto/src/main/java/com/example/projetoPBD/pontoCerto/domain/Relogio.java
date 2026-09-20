package com.example.projetoPBD.pontoCerto.domain;


import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "relogios", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"numeros_fabricante"})
})
public class Relogio extends Equipamento{

    public Relogio() {
        super(TipoEquipamento.RELOGIO);
    }

    @Column(length = 100,  nullable = false, name = "numeros_fabricante")
    private String num_fabricante;

    @Column(name = "linhas_importadas")
    private Long linhas_importadas;


}
