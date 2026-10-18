package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.temp.PairingCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PairingCodeRepository extends JpaRepository<PairingCode, UUID> {
}
