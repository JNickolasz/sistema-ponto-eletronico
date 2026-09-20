package com.example.projetoPBD.pontoCerto.security;

import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityTenantContext implements TenantContext {

    @Override
    public UUID empresaAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AcessoNegadoException("Usuário não autenticado!");
        }

        if (!(authentication.getPrincipal() instanceof FuncionarioUserDetails funcionarioUserDetails)) {
            throw new AcessoNegadoException("Usuário não autenticado!");
        }

        return funcionarioUserDetails.getFuncionario().getEmpresa().getId();
    }
}
