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

        jdbcTemplate.execute("""                                                                                                                          
                    DO $$                                                                                                                                         
                    DECLARE                                                                                                                                       
                        r RECORD;                                                                                                                                 
                    BEGIN                                                                                                                                         
                        FOR r IN (                                                                                                                                
                            SELECT tc.constraint_name                                                                                                             
                            FROM information_schema.table_constraints tc                                                                                          
                            JOIN information_schema.key_column_usage kcu                                                                                          
                              ON tc.constraint_name = kcu.constraint_name                                                                                         
                            WHERE tc.table_name = 'batida'                                                                                                        
                              AND tc.constraint_type = 'UNIQUE'                                                                                                   
                              AND tc.constraint_name <> 'uk_batida_equipamento_nsr'                                                                               
                        ) LOOP                                                                                                                                    
                            EXECUTE 'ALTER TABLE batida DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);                                            
                        END LOOP;                                                                                                                                 
                    END $$;                                                                                                                                       
                """);

        // constraint composta (equipamento_id, nsr)
        jdbcTemplate.execute("""                                                                                                                          
                    DO $$                                                                                                                                         
                    BEGIN                                                                                                                                         
                        IF NOT EXISTS (                                                                                                                           
                            SELECT 1 FROM information_schema.table_constraints                                                                                    
                            WHERE table_name = 'batida' AND constraint_name = 'uk_batida_equipamento_nsr'                                                         
                        ) THEN                                                                                                                                    
                            ALTER TABLE batida ADD CONSTRAINT uk_batida_equipamento_nsr UNIQUE (equipamento_id, nsr);                                             
                        END IF;                                                                                                                                   
                    END $$;                                                                                                                                       
                """);
    }
}