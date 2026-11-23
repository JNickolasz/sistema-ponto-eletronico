package com.example.projetoPBD.pontoCerto.security.filter;

import com.example.projetoPBD.pontoCerto.security.RateLimiterService;
import com.example.projetoPBD.pontoCerto.service.exceptions.TooManyRequestsException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Proteção global de camada de aplicação contra picos de carga e floods (HTTP 429 Too Many Requests).
 * Atua antes dos controllers e filtros de autenticação/banco para preservar o pool de conexões.
 */
@Component
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private static final long JANELA_SEGUNDOS = 60L;

    private final RateLimiterService rateLimiterService;
    private final boolean enabled;
    private final int maxGeneralPerMin;
    private final int maxHeavyPerMin;
    private final int maxAdminPerMin;

    public GlobalRateLimitFilter(
            RateLimiterService rateLimiterService,
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.rate-limit.general-per-min:180}") int maxGeneralPerMin,
            @Value("${app.security.rate-limit.heavy-per-min:30}") int maxHeavyPerMin,
            @Value("${app.security.rate-limit.admin-per-min:40}") int maxAdminPerMin
    ) {
        this.rateLimiterService = rateLimiterService;
        this.enabled = enabled;
        this.maxGeneralPerMin = maxGeneralPerMin;
        this.maxHeavyPerMin = maxHeavyPerMin;
        this.maxAdminPerMin = maxAdminPerMin;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri == null || (!uri.startsWith("/api/") && !uri.startsWith("/v1/api/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String ip = extrairIpCliente(request);
        String uri = request.getRequestURI();

        try {
            if (uri.startsWith("/api/v1/admin")) {
                rateLimiterService.verificarLimite("global-admin:" + ip, maxAdminPerMin, JANELA_SEGUNDOS);
            } else if (ehRotaPesada(uri)) {
                rateLimiterService.verificarLimite("global-heavy:" + ip, maxHeavyPerMin, JANELA_SEGUNDOS);
            } else {
                rateLimiterService.verificarLimite("global-api:" + ip, maxGeneralPerMin, JANELA_SEGUNDOS);
            }
        } catch (TooManyRequestsException ex) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(JANELA_SEGUNDOS));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"codigo\":\"TOO_MANY_REQUESTS\",\"message\":\"" + escaparJson(ex.getMessage()) + "\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean ehRotaPesada(String uri) {
        return uri.startsWith("/api/v1/importacao")
                || uri.startsWith("/api/importacao")
                || uri.startsWith("/api/folha")
                || uri.startsWith("/api/relatorios")
                || uri.startsWith("/api/v1/apuracao/recalcular")
                || uri.startsWith("/api/apuracao/recalcular");
    }

    private String extrairIpCliente(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "unknown" : remote;
    }

    private String escaparJson(String valor) {
        if (valor == null) return "Muitas requisições em sequência.";
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
