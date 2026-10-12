package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.BatidaDTO;
import com.example.projetoPBD.pontoCerto.service.BatidaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController

@RequestMapping({"/api", "/v1/api"})
public class BatidaController {
    private final BatidaService batidaService;

    public BatidaController(BatidaService batidaService) {
        this.batidaService = batidaService;
    }

    @PostMapping("/batidas")
    public ResponseEntity<BatidaDTO.ComprovanteResponse> registrarBatida(
            @Valid @RequestBody BatidaDTO.Request dto
    ) {
        BatidaDTO.ComprovanteResponse comprovante = batidaService.registrarBatida(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(comprovante);
    }

    @GetMapping("/funcionarios/{id}/batidas")
    public ResponseEntity<List<BatidaDTO.ComprovanteResponse>> listarBatidasPorFuncionario(
            @PathVariable UUID id
    ) {
        List<BatidaDTO.ComprovanteResponse> batidas = batidaService.listarPorFuncionario(id);
        return ResponseEntity.ok(batidas);
    }
}
