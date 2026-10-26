package com.example.projetoPBD.pontoCerto.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaAdjuster implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaAdjuster(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE batida ALTER COLUMN funcionario_id DROP NOT NULL");
        jdbcTemplate.execute("UPDATE batida SET status_batida = 'NORMAL' WHERE status_batida IS NULL");
    }
}