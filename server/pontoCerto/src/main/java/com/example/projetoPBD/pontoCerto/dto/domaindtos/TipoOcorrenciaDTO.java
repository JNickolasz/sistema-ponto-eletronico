package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.TipoOcorrencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoAbonoOcorrencia;

import java.util.UUID;

public class TipoOcorrenciaDTO {

    public record CriarRequest(
            String nome,
            String descricao,
            TipoAbonoOcorrencia tipoAbono,
            Integer minutosAbonoPadrao,
            Boolean exigeAnexo
    ) {}

    public record AtualizarRequest(
            String nome,
            String descricao,
            TipoAbonoOcorrencia tipoAbono,
            Integer minutosAbonoPadrao,
            Boolean exigeAnexo,
            Boolean ativo
    ) {}

    public record Response(
            UUID id,
            UUID empresaId,
            String nome,
            String descricao,
            TipoAbonoOcorrencia tipoAbono,
            Integer minutosAbonoPadrao,
            boolean exigeAnexo,
            boolean ativo
    ) {
        public Response(TipoOcorrencia t) {
            this(
                    t.getId(),
                    t.getEmpresa() != null ? t.getEmpresa().getId() : null,
                    t.getNome(),
                    t.getDescricao(),
                    t.getTipoAbono(),
                    t.getMinutosAbonoPadrao(),
                    t.isExigeAnexo(),
                    t.isAtivo()
            );
        }
    }
}
