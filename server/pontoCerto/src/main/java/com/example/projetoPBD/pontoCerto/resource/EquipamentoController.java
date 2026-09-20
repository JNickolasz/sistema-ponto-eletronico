package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EquipamentoDTO;
import com.example.projetoPBD.pontoCerto.service.EquipamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('RH')")
// Pra n precisar botar em cada endpoint
@RequestMapping({"/api/v1/equipamento", "/api/v1/equipamentos"})
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    @PostMapping
    public ResponseEntity<EquipamentoDTO.Response> cadastrar(@Valid @RequestBody EquipamentoDTO.Request dto) {
        EquipamentoDTO.Response response = equipamentoService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EquipamentoDTO.Response>> listar(
            @RequestParam(required = false) UUID localTrabalhoId,
            @RequestParam(required = false) UUID empresaId) {
        List<EquipamentoDTO.Response> equipamentos = equipamentoService.listar(localTrabalhoId, empresaId);
        return ResponseEntity.ok(equipamentos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipamentoDTO.Response> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(equipamentoService.buscarPorId(id));
    }

    // exemplo de uso "/api/v1/equipamentos/{id}/status?status=INATIVO"
    @PatchMapping("/{id}/status")
    public ResponseEntity<EquipamentoDTO.Response> alterarStatus(
            @PathVariable UUID id,
            @RequestParam StatusEquipamento status) {
        EquipamentoDTO.Response response = equipamentoService.alterarStatus(id, status);
        // dessa forma a gnt tbm já mata o critério 5, pq ele apenas muda o status do equipamento sem desativar
        return ResponseEntity.ok(response);
    }
}
