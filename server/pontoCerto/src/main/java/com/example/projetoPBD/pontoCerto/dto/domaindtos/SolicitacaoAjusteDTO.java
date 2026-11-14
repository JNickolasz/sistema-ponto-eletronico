package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.SolicitacaoAjuste;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusSolicitacaoAjuste;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegistro;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class SolicitacaoAjusteDTO {

    public record CriarRequest(
            @NotNull(message = "A data alvo é obrigatória!")
            LocalDate dataAlvo,

            @NotNull(message = "O horário pretndido é obrigatório!")
            LocalTime horarioPretendido,

            @NotNull(message = "O tipo de registro (ENTREDA/SAÍDA) é obrigatório!")
            TipoRegistro tipoRegistro,

            @NotNull(message = "O motivo da abertura da solicitação é obrigatório")
            String motivo
            ) {}

    public record AnalisarRequest(
            @NotNull(message = "O status da análise 'obrigatório (DEFERIDO ou INDEFERIDO")
            StatusSolicitacaoAjuste status,

            @NotNull(message = "A justificativa do gestor é obrigatória!")
            String justificativaGestor
    ) {}

    public record Response (
            UUID id,
            UUID colaboradorId,
            String colaboradorNome,
            String colaboradorMatricula,
            LocalDate dataAlvo,
            LocalTime horarioPretendido,
            TipoRegistro tipoRegistro,
            String motivo,
            StatusSolicitacaoAjuste status,
            LocalDateTime solicitadoEm,
            UUID gestorId,
            String gestorNome,
            LocalDateTime analisadoEm,
            String justificativaGestor,
            UUID batidaCriadaId
    ) {
        public Response(SolicitacaoAjuste s) {
            this(
                    s.getId(),
                    s.getColaborador() != null ? s.getColaborador().getId() : null,
                    s.getColaborador() != null ? s.getColaborador().getNomeCompleto() : null,
                    s.getColaborador() != null ? s.getColaborador().getMatricula() : null,
                    s.getDataAlvo(),
                    s.getHorarioPretendido(),
                    s.getTipoRegistro(),
                    s.getMotivo(),
                    s.getStatus(),
                    s.getSolicitadoEm(),
                    s.getGestor() != null ? s.getGestor().getId() : null,
                    s.getGestor() != null ? s.getGestor().getNomeCompleto() : null,
                    s.getAnalisadoEm(),
                    s.getJustificativaGestor(),
                    s.getBatidaCriada() != null ? s.getBatidaCriada().getId() : null
            );
        }
    }
}
