package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.FechamentoPeriodoDTO;
import com.example.projetoPBD.pontoCerto.service.FechamentoPeriodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/fechamentos", "/api/fechamentos"})
public class FechamentoPeriodoController {

    private final FechamentoPeriodoService fechamentoPeriodoService;

    public FechamentoPeriodoController(FechamentoPeriodoService fechamentoPeriodoService) {
        this.fechamentoPeriodoService = fechamentoPeriodoService;
    }

    @GetMapping("/previa")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'GESTOR')")
    public ResponseEntity<FechamentoPeriodoDTO.PreviaResponse> consultarPrevia(
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) String mesReferencia,
            @RequestParam(required = false) UUID empresaId
    ) {
        String comp = competencia != null ? competencia : mesReferencia;
        FechamentoPeriodoDTO.PreviaResponse resp = empresaId != null
                ? fechamentoPeriodoService.consultarPreviaFechamento(empresaId, comp)
                : fechamentoPeriodoService.consultarPreviaFechamento(comp);
        return ResponseEntity.ok(resp);
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
    public ResponseEntity<List<FechamentoPeriodoDTO.FechamentoDetalheResponse>> listarFechamentos(
            @RequestParam(required = false) UUID empresaId
    ) {
        List<FechamentoPeriodoDTO.FechamentoDetalheResponse> lista = empresaId != null
                ? fechamentoPeriodoService.listarFechamentos(empresaId)
                : fechamentoPeriodoService.listarFechamentos();
        return ResponseEntity.ok(lista);
    }
}
