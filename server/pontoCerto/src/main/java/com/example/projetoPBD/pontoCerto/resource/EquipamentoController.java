package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.dto.EquipamentoDTO;
import com.example.projetoPBD.pontoCerto.service.EquipamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/equipamentos", "/api/equipamentos"})
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<EquipamentoDTO.Response> cadastrar(@Valid @RequestBody EquipamentoDTO.Request dto) {
        EquipamentoDTO.Response response = equipamentoService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<List<EquipamentoDTO.Response>> listar(
            @RequestParam(required = false) UUID localTrabalhoId,
            @RequestParam(required = false) UUID empresaId) {
        List<EquipamentoDTO.Response> equipamentos = equipamentoService.listar(localTrabalhoId, empresaId);
        return ResponseEntity.ok(equipamentos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<EquipamentoDTO.Response> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(equipamentoService.buscarPorId(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<EquipamentoDTO.Response> alterarStatus(
            @PathVariable UUID id,
            @RequestParam StatusEquipamento status) {
        return ResponseEntity.ok(equipamentoService.alterarStatus(id, status));
    }
}
