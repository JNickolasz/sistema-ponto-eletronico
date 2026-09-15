package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.LocalDeTrabalhoDTO;
import com.example.projetoPBD.pontoCerto.service.LocalDeTrabalhoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('RH')")
@RequestMapping({"/api/v1/locais-trabalho", "/api/locais-de-trabalho", "/api/locais-trabalho"})
public class LocalDeTrabalhoController {

    private final LocalDeTrabalhoService localDeTrabalhoService;

    public LocalDeTrabalhoController(LocalDeTrabalhoService localDeTrabalhoService) {
        this.localDeTrabalhoService = localDeTrabalhoService;
    }

    @PostMapping
    public ResponseEntity<LocalDeTrabalhoDTO.Response> cadastrar(@RequestBody LocalDeTrabalhoDTO.Request dto) {
        LocalDeTrabalhoDTO.Response response = localDeTrabalhoService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LocalDeTrabalhoDTO.Response>> listar(@RequestParam(required = false) UUID empresaId) {
        List<LocalDeTrabalhoDTO.Response> locais = localDeTrabalhoService.listar(empresaId);
        return ResponseEntity.ok(locais);
    }

}
