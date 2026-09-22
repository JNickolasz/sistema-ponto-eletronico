package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.JornadaDTO;
import com.example.projetoPBD.pontoCerto.service.JornadaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('RH')")
@RequestMapping({"/api/v1/jornadas", "/api/v1/jornada"})
public class JornadaController {

    private final JornadaService jornadaService;

    public JornadaController(JornadaService jornadaService) {
        this.jornadaService = jornadaService;
    }

    @PostMapping
    public ResponseEntity<JornadaDTO.Response> cadastrar(@Valid @RequestBody JornadaDTO.Criar dto) {
        JornadaDTO.Response response = jornadaService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<JornadaDTO.Response>> listar() {
        List<JornadaDTO.Response> jornadas = jornadaService.listar();
        return ResponseEntity.ok(jornadas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JornadaDTO.Response> buscarPorId(@PathVariable UUID id) {
        JornadaDTO.Response response = jornadaService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }
}
