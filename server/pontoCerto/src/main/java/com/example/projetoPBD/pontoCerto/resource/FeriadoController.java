package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.FeriadoDTO;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.FeriadoService;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/feriado", "/api/v1/feriados"})
public class FeriadoController {

    private final FeriadoService feriadoService;
    private final SecurityTenantContext securityTenantContext;

    public FeriadoController(FeriadoService feriadoService, SecurityTenantContext securityTenantContext) {
        this.feriadoService = feriadoService;
        this.securityTenantContext = securityTenantContext;
    }

    @PostMapping
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<FeriadoDTO.Response> cadastrar(@Valid @RequestBody FeriadoDTO.Request dto) {
        FeriadoDTO.Response response = feriadoService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

//    @PutMapping("/{id}")
//    public ResponseEntity<FeriadoDTO.Response> atualizar(
//            @PathVariable UUID id,
//            @Valid @RequestBody FeriadoDTO.Request dto) {
//        return ResponseEntity.ok(feriadoService.atualizar(id, dto));
//    }

    @DeleteMapping("/{id}") // Ou @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        feriadoService.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/calendario")
    public ResponseEntity<List<FeriadoDTO.Response>> buscarCalendario(
            @RequestParam(value = "ano", required = false) Integer ano,
            @RequestParam(value = "uf", required = false) String uf,
            @RequestParam(value = "municipio", required = false) String municipio,
            @RequestParam(value = "empresaId", required = false) UUID empresaId) {

        UUID empresaAtual = securityTenantContext.empresaAtual();
        if (empresaId != null && !empresaId.equals(empresaAtual)) {
            throw new AcessoNegadoException("Acesso negado: não é permitido consultar calendário de outra empresa.");
        }

        List<FeriadoDTO.Response> calendario = feriadoService.listarCalendarioFiltrado(ano, uf, municipio, empresaAtual);
        return ResponseEntity.ok(calendario);
    }
}