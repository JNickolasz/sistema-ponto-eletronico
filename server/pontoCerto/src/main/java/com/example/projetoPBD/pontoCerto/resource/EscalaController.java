package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.EscalaDTO;
import com.example.projetoPBD.pontoCerto.service.EscalaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('RH')")
@RequestMapping({"/api/v1/escalas", "/api/v1/escala"})
public class EscalaController {

    private final EscalaService escalaService;

    public EscalaController(EscalaService escalaService) {
        this.escalaService = escalaService;
    }

    @PostMapping
    public ResponseEntity<EscalaDTO.Response> cadastrar(@Valid @RequestBody EscalaDTO.Criar dto) {
        EscalaDTO.Response response = escalaService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EscalaDTO.Response>> listar() {
        List<EscalaDTO.Response> escalas = escalaService.listar();
        return ResponseEntity.ok(escalas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EscalaDTO.Response> buscarPorId(@PathVariable UUID id) {
        EscalaDTO.Response response = escalaService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }
}
