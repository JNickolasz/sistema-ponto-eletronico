package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.dto.EquipamentoFiltroDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EquipamentoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EquipamentoResponse;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ttl.PairingCodeDTO;
import com.example.projetoPBD.pontoCerto.service.EquipamentoService;
import com.example.projetoPBD.pontoCerto.service.EstacaoService;
import com.example.projetoPBD.pontoCerto.service.EstacaoSetupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('RH')")
@RequestMapping({"/api/v1/equipamento", "/api/v1/equipamentos"})
public class EquipamentoController {

    private final EquipamentoService equipamentoService;
    private final EstacaoService estacaoService;
    private final EstacaoSetupService estacaoSetupService;

    public EquipamentoController(EquipamentoService equipamentoService, EstacaoService estacaoService, EstacaoSetupService estacaoSetupService) {
        this.equipamentoService = equipamentoService;
        this.estacaoService = estacaoService;
        this.estacaoSetupService = estacaoSetupService;
    }

    @PostMapping
    public ResponseEntity<EquipamentoResponse> cadastrar(@Valid @RequestBody EquipamentoDTO.Criar criarDto) {
        EquipamentoResponse response = equipamentoService.cadastrar(criarDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EquipamentoResponse>> listar(EquipamentoFiltroDTO filtro) {
        List<EquipamentoResponse> equipamentos = equipamentoService.listar(filtro);
        return ResponseEntity.ok(equipamentos);
    }

    @PatchMapping("/{codigo}/status")
    public ResponseEntity<EquipamentoResponse> alterarStatus(
            @PathVariable String codigo,
            @RequestParam StatusEquipamento status) {
        EquipamentoResponse response = equipamentoService.alterarStatus(codigo, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{codigo}/autorizar")
    public ResponseEntity<PairingCodeDTO.Response> parearEstacao(
            @PathVariable String codigo
    ){
        PairingCodeDTO.Response response = estacaoSetupService.autorizarEstacao(codigo);
        return ResponseEntity.ok(response);
    }

}
