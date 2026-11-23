package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.AdminIpBloqueio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminIpBloqueioRepository extends JpaRepository<AdminIpBloqueio, UUID> {

    Optional<AdminIpBloqueio> findByIp(String ip);
}
