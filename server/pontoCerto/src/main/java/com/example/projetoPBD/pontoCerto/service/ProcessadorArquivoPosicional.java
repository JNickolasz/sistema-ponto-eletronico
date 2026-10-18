package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.service.exceptions.ArquivoImportacaoInvalidoException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProcessadorArquivoPosicional {

    private static final DateTimeFormatter DATA_FORMATER = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter HORA_FORMATER = DateTimeFormatter.ofPattern("HHmm");

    public record RegistroBatidaLinha(int numeroLinha, String pis, LocalDateTime dataHora) {}
    public record ResultadoLeitura(List<RegistroBatidaLinha> registro, int totalLinhasBatidas) {}

    public List<String> lerEValidarEstrutura(InputStream inputStream) {
        List<String> linhas = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                String linhaTrim = linha.trim();
                if (!linhaTrim.isEmpty()) {
                    linhas.add(linhaTrim);
                }
            }
        } catch (IOException e) {
            throw new ArquivoImportacaoInvalidoException("Erro ao ler o arquivo de importação: " + e.getMessage());
        }

        if (linhas.isEmpty()) {
            throw new ArquivoImportacaoInvalidoException("O arquivo de importação está vazio.");
        }

        String primeiraLinha = linhas.get(0);
        if (primeiraLinha.isEmpty() || !"1".equals(primeiraLinha.substring(0, 1))) {
            throw new ArquivoImportacaoInvalidoException(
                    "Arquivo inválido: o cabeçalho (registro tipo 1) não foi encontrado na primeira linha."
            );
        }

        String ultimaLinha = linhas.get(linhas.size() - 1);
        if (ultimaLinha.isEmpty() || !"9".equals(ultimaLinha.substring(0, 1))) {
            throw new ArquivoImportacaoInvalidoException(
                    "Arquivo inválido: o trailer/totalizador (registro tipo 9) não foi encontrado na última linha."
            );
        }

        return linhas;
    }

    public RegistroBatidaLinha extrairRegistroBatida(String linha) {
        return extrairRegistroBatida(1, linha);
    }

    public RegistroBatidaLinha extrairRegistroBatida(int numeroLinha, String linha) {
        if (linha == null || linha.isEmpty()) {
            throw new ArquivoImportacaoInvalidoException(
                    String.format("Linha %d está vazia.", numeroLinha)
            );
        }

        String tipoRegistro = linha.substring(0, 1);
        if (!"3".equals(tipoRegistro)) {
            throw new ArquivoImportacaoInvalidoException(
                    String.format("Linha %d possui tipo de registro inválido ('%s'). Esperado registro tipo '3' (batida).", numeroLinha, tipoRegistro)
            );
        }

        if (linha.length() < 24) {
            throw new ArquivoImportacaoInvalidoException(
                    String.format("Linha %d possui formato inválido. Tamanho mínimo esperado: 24 caracteres.", numeroLinha)
            );
        }

        try {
            String dataStr = linha.substring(1, 9);
            String horaStr = linha.substring(9, 13);
            String pis = linha.substring(13, 24);

            LocalDate data = LocalDate.parse(dataStr, DATA_FORMATER);
            LocalTime hora = LocalTime.parse(horaStr, HORA_FORMATER);
            LocalDateTime dataHora = LocalDateTime.of(data, hora);
            return new RegistroBatidaLinha(numeroLinha, pis, dataHora);
        } catch (Exception e) {
            throw new ArquivoImportacaoInvalidoException(
                    String.format("Erro ao decodificar dados na linha %d: %s", numeroLinha, e.getMessage())
            );
        }
    }

    public ResultadoLeitura processar(InputStream inputStream) {
        List<String> linhas = lerEValidarEstrutura(inputStream);
        List<RegistroBatidaLinha> registros = new ArrayList<>();

        for (int i = 1; i < linhas.size() - 1; i++) {
            String linha = linhas.get(i);
            int numeroLinha = i + 1;
            registros.add(extrairRegistroBatida(numeroLinha, linha));
        }

        return new ResultadoLeitura(registros, registros.size());
    }
}
