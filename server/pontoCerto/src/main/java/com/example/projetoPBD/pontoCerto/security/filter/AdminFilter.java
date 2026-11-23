package com.example.projetoPBD.pontoCerto.security.filter;

import com.example.projetoPBD.pontoCerto.security.AdminBruteForceService;
import com.example.projetoPBD.pontoCerto.security.AdminBruteForceService.StatusBloqueio;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AdminFilter extends OncePerRequestFilter {

    private final AdminBruteForceService bruteForceService;

    @Value("${api.security.admin-key:admin123}")
    private String adminKey;

    public AdminFilter(AdminBruteForceService bruteForceService) {
        this.bruteForceService = bruteForceService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/admin") || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String ip = request.getRemoteAddr() != null && !request.getRemoteAddr().isBlank()
                ? request.getRemoteAddr()
                : "unknown";

        StatusBloqueio statusAtual = bruteForceService.verificarSeBloqueado(ip);
        if (statusAtual.bloqueado()) {
            escreverRespostaBloqueio(response, statusAtual);
            return;
        }

        String key = request.getHeader("X-Admin-Key");
        boolean valido = key != null && adminKey != null && MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8),
                adminKey.getBytes(StandardCharsets.UTF_8)
        );

        if (!valido) {
            StatusBloqueio posFalha = bruteForceService.registrarFalha(ip);
            if (posFalha.bloqueado()) {
                escreverRespostaBloqueio(response, posFalha);
                return;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"codigo\":\"ADMIN_KEY_INVALIDA\",\"tentativasRestantes\":"
                            + posFalha.tentativasRestantes()
                            + ",\"message\":\""
                            + escaparJson(posFalha.mensagem())
                            + "\"}"
            );
            return;
        }

        bruteForceService.registrarSucesso(ip);
        filterChain.doFilter(request, response);
    }

    private void escreverRespostaBloqueio(HttpServletResponse response, StatusBloqueio status) throws IOException {
        if (status.permanente()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"codigo\":\"IP_BLOQUEADO_PERMANENTE\",\"nivelPenalidade\":"
                            + status.nivelPenalidade()
                            + ",\"message\":\""
                            + escaparJson(status.mensagem())
                            + "\"}"
            );
            return;
        }

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(status.segundosRestantes()));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"codigo\":\"IP_BLOQUEADO_TEMPORARIO\",\"nivelPenalidade\":"
                        + status.nivelPenalidade()
                        + ",\"segundosRestantes\":"
                        + status.segundosRestantes()
                        + ",\"message\":\""
                        + escaparJson(status.mensagem())
                        + "\"}"
        );
    }

    private String escaparJson(String texto) {
        if (texto == null) return "Acesso bloqueado.";
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
