package com.example.projetoPBD.pontoCerto.resource;


import com.example.projetoPBD.pontoCerto.dto.domaindtos.EmpresaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FeriadoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.PontoFacultativoDTO;
import com.example.projetoPBD.pontoCerto.service.AdminDashboardService;
import com.example.projetoPBD.pontoCerto.service.EmpresaService;
import com.example.projetoPBD.pontoCerto.service.FeriadoService;
import com.example.projetoPBD.pontoCerto.service.PontoFacultativoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.projetoPBD.pontoCerto.dto.EntityResponse;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final EmpresaService empresaService;
    private final FeriadoService feriadoService;
    private final PontoFacultativoService pontoFacultativoService;
    private final AdminDashboardService adminDashboardService;

    public AdminController(
            EmpresaService empresaService,
            FeriadoService feriadoService,
            PontoFacultativoService pontoFacultativoService,
            AdminDashboardService adminDashboardService
    ) {
        this.empresaService = empresaService;
        this.feriadoService = feriadoService;
        this.pontoFacultativoService = pontoFacultativoService;
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/estatisticas")
    public ResponseEntity<Map<String, Object>> obterEstatisticas() {
        return ResponseEntity.ok(adminDashboardService.obterEstatisticasGlobais());
    }

    @GetMapping("/empresas/{id}/detalhes")
    public ResponseEntity<Map<String, Object>> obterDetalhesEmpresa(@PathVariable UUID id) {
        return ResponseEntity.ok(adminDashboardService.obterDetalhesEmpresa(id));
    }

    @PatchMapping("/empresas/{id}/branding")
    public ResponseEntity<Map<String, Object>> atualizarBranding(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(adminDashboardService.atualizarBranding(
                id,
                body.get("logoUrl"),
                body.get("corPrimaria"),
                body.get("corSecundaria")
        ));
    }

    @PostMapping("/empresas")
    public ResponseEntity<EntityResponse<EmpresaDTO.Response>> cadastrarEmpresa(@Valid @RequestBody EmpresaDTO.Criar empresaDTO){
        EmpresaDTO.Response empresa = empresaService.cadastrarEmpresa(empresaDTO);

        EntityResponse<EmpresaDTO.Response> response = new EntityResponse<>(
                empresa,
                "Empresa Criada Com Sucesso!"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/empresas")
    public ResponseEntity<java.util.List<EmpresaDTO.Response>> listarEmpresas() {
        java.util.List<EmpresaDTO.Response> empresas = empresaService.listarEmpresas();
        return ResponseEntity.ok(empresas);
    }

    @PostMapping("/feriados")
    public ResponseEntity<FeriadoDTO.Response> cadastrarFeriadoGlobal(@Valid @RequestBody FeriadoDTO.Request dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feriadoService.cadastrarGlobal(dto));
    }

    @DeleteMapping("/feriados/{id}")
    public ResponseEntity<Void> inativarFeriadoGlobal(@PathVariable UUID id) {
        feriadoService.inativarGlobal(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pontos-facultativos")
    public ResponseEntity<PontoFacultativoDTO.Response> cadastrarPontoFacultativoGlobal(@Valid @RequestBody PontoFacultativoDTO.Request dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pontoFacultativoService.cadastrarGlobal(dto));
    }

    @DeleteMapping("/pontos-facultativos/{id}")
    public ResponseEntity<Void> inativarPontoFacultativoGlobal(@PathVariable UUID id) {
        pontoFacultativoService.inativarGlobal(id);
        return ResponseEntity.noContent().build();
    }

}
