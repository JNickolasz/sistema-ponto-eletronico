package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.FechamentoPeriodoDTO;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.FechamentoPeriodoService;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/fechamentos", "/api/fechamentos"})
public class FechamentoPeriodoController {

    private final FechamentoPeriodoService fechamentoPeriodoService;
    private final SecurityTenantContext securityTenantContext;

    public FechamentoPeriodoController(
            FechamentoPeriodoService fechamentoPeriodoService,
            SecurityTenantContext securityTenantContext
    ) {
        this.fechamentoPeriodoService = fechamentoPeriodoService;
        this.securityTenantContext = securityTenantContext;
    }

    @GetMapping("/previa")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'GESTOR')")
    public ResponseEntity<FechamentoPeriodoDTO.PreviaResponse> consultarPrevia(
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) String mesReferencia,
            @RequestParam(required = false) UUID empresaId
    ) {
        validarTenant(empresaId);
        String comp = competencia != null ? competencia : mesReferencia;
        return ResponseEntity.ok(fechamentoPeriodoService.consultarPreviaFechamento(comp));
    }

    @PostMapping("/fechar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<FechamentoPeriodoDTO.FechamentoDetalheResponse> fecharPeriodo(
            @RequestBody FechamentoPeriodoDTO.FecharRequest req
    ) {
        return ResponseEntity.ok(fechamentoPeriodoService.fecharPeriodo(req));
    }

    @PostMapping("/reabrir")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<FechamentoPeriodoDTO.FechamentoDetalheResponse> reabrirPeriodo(
            @RequestBody FechamentoPeriodoDTO.ReabrirRequest req
    ) {
        return ResponseEntity.ok(fechamentoPeriodoService.reabrirPeriodo(req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'GESTOR')")
    public ResponseEntity<List<FechamentoPeriodoDTO.FechamentoDetalheResponse>> listarFechamentos(
            @RequestParam(required = false) UUID empresaId
    ) {
        validarTenant(empresaId);
        return ResponseEntity.ok(fechamentoPeriodoService.listarFechamentos());
    }

    private void validarTenant(UUID empresaId) {
        if (empresaId != null && !empresaId.equals(securityTenantContext.empresaAtual())) {
            throw new AcessoNegadoException("Acesso negado: empresa informada não pertence ao usuário autenticado.");
        }
    }
}
