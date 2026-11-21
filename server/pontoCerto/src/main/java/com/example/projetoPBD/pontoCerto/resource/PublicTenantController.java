package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Estacao;
import com.example.projetoPBD.pontoCerto.domain.WhiteLabel;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.EstacaoRepository;
import com.example.projetoPBD.pontoCerto.security.RateLimiterService;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaNaoEncontradaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoint público somente-leitura para resolver identidade visual (White-Label) e estações ativas por subdomínio.
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicTenantController {

    private final EmpresaRepository empresaRepository;
    private final EstacaoRepository estacaoRepository;
    private final RateLimiterService rateLimiterService;

    public PublicTenantController(
            EmpresaRepository empresaRepository,
            EstacaoRepository estacaoRepository,
            RateLimiterService rateLimiterService
    ) {
        this.empresaRepository = empresaRepository;
        this.estacaoRepository = estacaoRepository;
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/tenant/{subdominio}")
    public ResponseEntity<Map<String, Object>> obterBrandingPorSubdominio(
            @PathVariable String subdominio,
            HttpServletRequest request
    ) {
        rateLimiterService.verificarLimite("public-tenant:" + request.getRemoteAddr(), 120, 60);

        Empresa empresa = empresaRepository.findBySubDominioIgnoreCase(subdominio.trim())
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada para o subdomínio informado."));

        WhiteLabel wl = empresa.getConfigWhiteLabel();
        String logo = (wl != null && wl.getUrlLogo() != null && !wl.getUrlLogo().isBlank())
                ? wl.getUrlLogo()
                : (empresa.getLogoUrl() != null ? empresa.getLogoUrl() : "");

        List<Map<String, Object>> estacoes = estacaoRepository.findByEmpresaId(empresa.getId()).stream()
                .filter(e -> e.getStatus() == StatusEquipamento.ATIVO)
                .map((Estacao e) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", e.getId());
                    item.put("identificacao", e.getIdentificacao());
                    item.put("local", e.getLocalTrabalho() != null ? e.getLocalTrabalho().getNome() : "");
                    return item;
                })
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subdominio", empresa.getSubDominio());
        body.put("razaoSocial", empresa.getRazaoSocial());
        body.put("logoUrl", logo);
        body.put("corPrimaria", wl != null && wl.getPrimaryColor() != null ? wl.getPrimaryColor() : "#2563eb");
        body.put("corSecundaria", wl != null && wl.getSecundaryColor() != null ? wl.getSecundaryColor() : "#1d4ed8");
        body.put("estacoes", estacoes);

        return ResponseEntity.ok(body);
    }
}
