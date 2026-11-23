package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.repository.RegimeTrabalhoRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.GlobalExceptionHandler;
import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Deve identificar tabela, parametro e trecho SQL quando parametro nulo nao tem tipo no PostgreSQL")
    void deveIdentificarParametroNuloEmErroJdbc() {
        String sql = "select rt1_0.id,rt1_0.data_fim_vigencia,rt1_0.data_inicio_vigencia " +
                "from regime_trabalho rt1_0 " +
                "where rt1_0.funcionario_id=? and (? is null or rt1_0.data_inicio_vigencia<=?) " +
                "and (rt1_0.data_fim_vigencia is null or rt1_0.data_fim_vigencia>=?)";

        SQLException sqlEx = new SQLException("ERROR: could not determine data type of parameter $2");
        SQLGrammarException jdbcEx = new SQLGrammarException("JDBC exception executing SQL", sqlEx, sql);
        InvalidDataAccessResourceUsageException ex = new InvalidDataAccessResourceUsageException("Erro JDBC", jdbcEx);

        ResponseEntity<String> response = handler.handleDatabaseException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("tabela 'regime_trabalho'"));
        assertTrue(response.getBody().contains("parâmetro $2"));
        assertTrue(response.getBody().contains("(? is null or rt1_0.data_inicio_vigencia<=?)"));
    }

    @Test
    @DisplayName("Deve identificar coluna e tabela em violacao de constraint NOT NULL")
    void deveIdentificarColunaEmViolacaoNotNull() {
        SQLException sqlEx = new SQLException(
                "ERROR: null value in column \"data_inicio_vigencia\" of relation \"regime_trabalho\" violates not-null constraint"
        );
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Violacao", sqlEx);

        ResponseEntity<String> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "Não foi possível concluir a operação: o campo obrigatório 'data_inicio_vigencia' (tabela 'regime_trabalho') está nulo (null).",
                response.getBody()
        );
    }

    @Test
    @DisplayName("findSobrepostos deve delegar para query sem dataFim quando dataFim for nulo")
    void deveDelegarFindSobrepostosCorretamente() {
        RegimeTrabalhoRepository repo = mock(RegimeTrabalhoRepository.class);
        UUID funcId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 12, 31);

        when(repo.findSobrepostos(any(), any(), any())).thenCallRealMethod();
        when(repo.findSobrepostosSemDataFim(funcId, inicio)).thenReturn(List.of());
        when(repo.findSobrepostosComDataFim(funcId, inicio, fim)).thenReturn(List.of());

        repo.findSobrepostos(funcId, inicio, null);
        verify(repo).findSobrepostosSemDataFim(funcId, inicio);
        verify(repo, never()).findSobrepostosComDataFim(any(), any(), any());

        repo.findSobrepostos(funcId, inicio, fim);
        verify(repo).findSobrepostosComDataFim(funcId, inicio, fim);
    }
}
