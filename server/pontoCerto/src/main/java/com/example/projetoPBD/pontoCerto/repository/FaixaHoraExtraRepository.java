package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.FaixaHoraExtra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaixaHoraExtraRepository extends JpaRepository<FaixaHoraExtra, UUID> {

    List<FaixaHoraExtra> findAllBy();
}
