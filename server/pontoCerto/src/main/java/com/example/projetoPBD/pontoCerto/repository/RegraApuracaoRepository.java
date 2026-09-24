package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RegraApuracaoRepository extends JpaRepository<RegraApuracao, UUID> {

    Optional<RegraApuracao> findFirstByEmpresaIdAndInicioVigenciaLessThanEqualOrderByInicioVigenciaDesc(UUID empresa, LocalDate inicioVigencia);

}