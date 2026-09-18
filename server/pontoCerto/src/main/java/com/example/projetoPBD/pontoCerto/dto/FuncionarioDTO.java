package com.example.projetoPBD.pontoCerto.dto;


import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.PerfilAcesso;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;
import java.util.UUID;

public class FuncionarioDTO {

    public record Criar(
            UUID empresaId,
            String nomeCompleto,
            String usuario,
            String senha,
            String pis_pasep,
            @CPF
            String cpf,
            String matricula,
            LocalDate dataAdmissao,
            UUID localTrabalhoId,
            UUID gestorId,
            PerfilAcesso perfilAcesso
    ) {}

    public record Desligamento(
            LocalDate dataDesligamento
    ) {}

    public record Response(
            UUID id,
            UUID empresaId,
            String nomeCompleto,
            String usuario,
            String pis_pasep,
            String cpf,
            String matricula,
            LocalDate dataAdmissao,
            LocalDate dataDesligamento,
            UUID localTrabalhoId,
            UUID gestorId,
            PerfilAcesso perfilAcesso
    ) {
        public Response(Funcionario f) {
            this(
                    f.getId(),
                    f.getEmpresa() != null ? f.getEmpresa().getId() : null,
                    f.getNomeCompleto(),
                    f.getUsuario(),
                    f.getPisPasep(),
                    f.getCpf(),
                    f.getMatricula(),
                    f.getDataAdmissao(),
                    f.getDataDesligamento(),
                    f.getLocalDeTrabalho() != null ? f.getLocalDeTrabalho().getId() : null,
                    f.getGestor() != null ? f.getGestor().getId() : null,
                    f.getPerfilAcesso()
            );
        }
    }
}
