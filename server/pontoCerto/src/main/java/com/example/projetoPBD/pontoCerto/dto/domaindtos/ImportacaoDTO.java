package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.HistoricoImportacao;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusImportacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ImportacaoDTO {

    public record ItemPreview(
            int numeroLinha,
            String pis,
            String nomeFuncionario,
            LocalDateTime dataHora,
            String motivo
    ) {}

    public record PreviewResponse(
            String nomeArquivo,
            UUID equipamentoId,
            String identificacaoEquipamento,
            int totalLidas,
            int totalValidas,
            int totalRepetidas,
            int totalRecusadas,
            List<ItemPreview> validas,
            List<ItemPreview> repetidas,
            List<ItemPreview> recusadas
    ) {}

    public record HistoricoResponse(
            UUID id,
            LocalDateTime dataImportacao,
            String nomeArquivo,
            String conteudoArquivo,
            String nomeAutor,
            String identificacaoEquipamento,
            int totalLidas,
            int totalValidas,
            int totalRepetidas,
            int totalRecusadas,
            StatusImportacao status
    ) {
        public HistoricoResponse(HistoricoImportacao h) {
            this(
                    h.getId(),
                    h.getDataImportacao(),
                    h.getNomeArquivo(),
                    h.getConteudoArquivo(),
                    h.getAutor() != null ? h.getAutor().getNomeCompleto() : "Sistema",
                    h.getEquipamento() != null ? h.getEquipamento().getIdentificacao() : "N/A",
                    h.getTotalLidas(),
                    h.getTotalValidas(),
                    h.getTotalRepetidas(),
                    h.getTotalRecusadas(),
                    h.getStatus()
            );
        }
    }
}
