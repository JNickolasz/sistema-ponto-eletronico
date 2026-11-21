package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExportacaoFolhaDTO {

    public record LinhaColaboradorFolha(
            UUID funcionarioId,
            String matricula,
            String cpf,
            String nomeCompleto,
            String cargo,
            int horasNormaisMinutos,
            int horasExtras50Minutos,
            int horasExtras100Minutos,
            Map<String, Integer> extrasPorFaixa,
            int adicionalNoturnoMinutos,
            int faltasMinutos,
            int atrasosMinutos,
            int saldoBancoHorasMinutos
    ) {}

    public record TotaisArquivoFolha(
            int totalHorasNormaisMinutos,
            int totalHorasExtras50Minutos,
            int totalHorasExtras100Minutos,
            int totalAdicionalNoturnoMinutos,
            int totalFaltasMinutos,
            int totalAtrasosMinutos,
            int totalSaldoBancoHorasMinutos
    ) {}

    public record ResumoExportacaoResponse(
            String competencia,
            String nomeArquivo,
            int totalColaboradores,
            String hashSha256,
            TotaisArquivoFolha totais,
            List<LinhaColaboradorFolha> linhas
    ) {}

    public record HistoricoItemResponse(
            UUID id,
            String competencia,
            String autor,
            UUID autorId,
            LocalDateTime dataHora,
            String nomeArquivo,
            Integer quantidadeColaboradores,
            String hashSha256
    ) {}
}
