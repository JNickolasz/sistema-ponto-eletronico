package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.FeriadoDTO;
import com.example.projetoPBD.pontoCerto.service.FeriadoService;
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

    public FeriadoController(FeriadoService feriadoService) {
        this.feriadoService = feriadoService;
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
            @RequestParam("ano") int ano,
            @RequestParam(value = "uf", required = false) String uf,
            @RequestParam(value = "municipio", required = false) String municipio,
            @RequestParam(value = "empresaId", required = false) UUID empresaId) {

        List<FeriadoDTO.Response> calendario = feriadoService.buscarCalendarioAnual(ano, uf, municipio, empresaId);
        return ResponseEntity.ok(calendario);
    }
}