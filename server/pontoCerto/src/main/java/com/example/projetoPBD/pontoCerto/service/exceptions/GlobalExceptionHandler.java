package com.example.projetoPBD.pontoCerto.service.exceptions;

import jakarta.persistence.PersistenceException;
import org.hibernate.JDBCException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Pattern PARAM_TYPE_PATTERN =
            Pattern.compile("could not determine data type of parameter \\$(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NOT_NULL_COLUMN_PATTERN =
            Pattern.compile("null value in column \"([^\"]+)\"(?: of relation \"([^\"]+)\")?", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE_NAME_PATTERN =
            Pattern.compile("\\b(?:from|into|update)\\s+([a-zA-Z0-9_.\"]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_BRACKET_PATTERN =
            Pattern.compile("\\[((?:select|insert|update|delete)\\b[^\\]]+)\\]", Pattern.CASE_INSENSITIVE);

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<String> handleException(AcessoNegadoException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<String> handleSpringAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Acesso negado: você não possui permissão para realizar esta operação.");
    }

    @ExceptionHandler(AlcanceInvalidoException.class)
    public ResponseEntity<String> handleException(AlcanceInvalidoException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<String> handleException(CredenciaisInvalidasException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(EmpresaCampoInvalidoException.class)
    public ResponseEntity<String> handleException(EmpresaCampoInvalidoException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(EmpresaExistenteException.class)
    public ResponseEntity<String> handleException(EmpresaExistenteException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(EmpresaNaoEncontradaException.class)
    public ResponseEntity<String> handleException(EmpresaNaoEncontradaException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(EquipamentoExistenteException.class)
    public ResponseEntity<String> handleException(EquipamentoExistenteException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(EquipamentoInvalidoException.class)
    public ResponseEntity<String> handleException(EquipamentoInvalidoException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(EquipamentoNaoEncontradoException.class)
    public ResponseEntity<String> handleException(EquipamentoNaoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(FeriadoExistenteException.class)
    public ResponseEntity<String> handleException(FeriadoExistenteException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(FeriadoNacionalEmpresaInvalidaException.class)
    public ResponseEntity<String> handleException(FeriadoNacionalEmpresaInvalidaException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(MunicipioCampoInvalidoException.class)
    public ResponseEntity<String> handleException(MunicipioCampoInvalidoException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(UfCampoInvalidoException.class)
    public ResponseEntity<String> handleException(UfCampoInvalidoException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(UsuarioDuplicadoException.class)
    public ResponseEntity<String> handleException(UsuarioDuplicadoException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<String> handleException(UsuarioNaoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }
    @ExceptionHandler(PontoFacultativoExistenteException.class)
    public ResponseEntity<String> handleException(PontoFacultativoExistenteException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(JornadaNaoEncontradaException.class)
    public ResponseEntity<String> handleException(JornadaNaoEncontradaException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(EscalaNaoEncontradaException.class)
    public ResponseEntity<String> handleException(EscalaNaoEncontradaException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(RegimeTrabalhoConflitoException.class)
    public ResponseEntity<String> handleException(RegimeTrabalhoConflitoException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(RegimeTrabalhoInvalidoException.class)
    public ResponseEntity<String> handleException(RegimeTrabalhoInvalidoException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleException(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(FiltroDeBuscaException.class)
    public ResponseEntity<String> handleException(FiltroDeBuscaException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(CadastroRegraApuracaoInvalidoException.class)
    public ResponseEntity<String> handleException(CadastroRegraApuracaoInvalidoException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(RegraApuracaoNaoEncontradaException.class)
    public ResponseEntity<String> handleException(RegraApuracaoNaoEncontradaException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ArquivoImportacaoInvalidoException.class)
    public ResponseEntity<String> handleException(ArquivoImportacaoInvalidoException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(message);
    }

    @ExceptionHandler(FechamentoComPendenciasException.class)
    public ResponseEntity<String> handleException(FechamentoComPendenciasException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(PeriodoFechadoException.class)
    public ResponseEntity<String> handleException(PeriodoFechadoException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(PeriodoNaoFechadoException.class)
    public ResponseEntity<String> handleException(PeriodoNaoFechadoException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<String> handleException(TooManyRequestsException ex) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex);
    }

    @ExceptionHandler(OcorrenciaConflitoException.class)
    public ResponseEntity<String> handleException(OcorrenciaConflitoException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Erro de integridade de dados no banco", ex);

        String detalheCausa = extrairMensagensDaCausa(ex);
        Matcher notNullMatcher = NOT_NULL_COLUMN_PATTERN.matcher(detalheCausa);
        if (notNullMatcher.find()) {
            String coluna = notNullMatcher.group(1);
            String tabela = notNullMatcher.group(2);
            String mensagem = tabela != null
                    ? String.format("Não foi possível concluir a operação: o campo obrigatório '%s' (tabela '%s') está nulo (null).", coluna, tabela)
                    : String.format("Não foi possível concluir a operação: o campo obrigatório '%s' está nulo (null).", coluna);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mensagem);
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body("Não foi possível concluir a operação: os dados informados violam uma restrição do banco de dados (registro duplicado, vínculo inválido ou campo obrigatório ausente).");
    }

    @ExceptionHandler({
            DataAccessException.class,
            PersistenceException.class,
            JDBCException.class,
            SQLException.class
    })
    public ResponseEntity<String> handleDatabaseException(Exception ex) {
        log.error("Erro ao executar operação no banco de dados", ex);

        String mensagem = "Erro interno ao acessar o banco de dados. Não foi possível concluir a operação no momento.";
        String detalheCausa = extrairMensagensDaCausa(ex);

        Matcher paramMatcher = PARAM_TYPE_PATTERN.matcher(detalheCausa);
        if (paramMatcher.find()) {
            int numeroParametro = Integer.parseInt(paramMatcher.group(1));
            String sql = extrairSql(ex, detalheCausa);
            String tabela = extrairTabela(sql);
            String trecho = extrairTrechoParametro(sql, numeroParametro);

            StringBuilder sb = new StringBuilder("Erro ao consultar o banco de dados");
            if (tabela != null) {
                sb.append(" na tabela '").append(tabela).append("'");
            }
            sb.append(": o parâmetro $").append(numeroParametro)
              .append(" foi enviado como nulo (null) sem tipo definido");
            if (trecho != null) {
                sb.append(" no trecho '").append(trecho).append("'");
            }
            sb.append(".");
            mensagem = sb.toString();
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mensagem);
    }

    private String extrairMensagensDaCausa(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable atual = ex;
        while (atual != null) {
            if (atual.getMessage() != null) {
                sb.append(atual.getMessage()).append(" ");
            }
            atual = atual.getCause();
        }
        return sb.toString();
    }

    private String extrairSql(Throwable ex, String detalheCausa) {
        Throwable atual = ex;
        while (atual != null) {
            if (atual instanceof JDBCException jdbcEx && jdbcEx.getSQL() != null && !jdbcEx.getSQL().isBlank()) {
                return jdbcEx.getSQL();
            }
            atual = atual.getCause();
        }
        Matcher matcher = SQL_BRACKET_PATTERN.matcher(detalheCausa);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extrairTabela(String sql) {
        if (sql == null) {
            return null;
        }
        Matcher matcher = TABLE_NAME_PATTERN.matcher(sql);
        return matcher.find() ? matcher.group(1).replace("\"", "") : null;
    }

    private String extrairTrechoParametro(String sql, int numeroParametro) {
        if (sql == null || numeroParametro <= 0) {
            return null;
        }
        int pos = -1;
        int encontrados = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                encontrados++;
                if (encontrados == numeroParametro) {
                    pos = i;
                    break;
                }
            }
        }
        if (pos == -1) {
            return null;
        }

        int abre = sql.lastIndexOf('(', pos);
        int fecha = sql.indexOf(')', pos);
        if (abre != -1 && fecha != -1 && (fecha - abre) <= 120) {
            return sql.substring(abre, fecha + 1).trim();
        }

        int inicio = Math.max(0, pos - 40);
        int fim = Math.min(sql.length(), pos + 40);
        return sql.substring(inicio, fim).trim();
    }

    private ResponseEntity<String> buildResponse(
            HttpStatus status,
            RuntimeException ex
    ) {
        String msg = (ex != null && ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Erro inesperado ao processar a requisição.";
        return ResponseEntity
                .status(status)
                .body(msg);
    }
}

