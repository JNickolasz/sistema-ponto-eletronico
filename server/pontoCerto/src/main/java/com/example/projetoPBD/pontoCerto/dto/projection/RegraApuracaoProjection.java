package com.example.projetoPBD.pontoCerto.dto.projection;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.enums.RegraApuracaoStatus;
import org.springframework.cglib.core.Local;


import java.time.LocalDate;
import java.util.UUID;

public interface RegraApuracaoProjection {
    
    UUID getId();
    Empresa getEmpresa();
    LocalDate getInicioVigencia();
    LocalDate getFimVigencia();
    Integer getLimiteDiarioExtraMinutos();
    RegraApuracaoStatus getStatus();

}
