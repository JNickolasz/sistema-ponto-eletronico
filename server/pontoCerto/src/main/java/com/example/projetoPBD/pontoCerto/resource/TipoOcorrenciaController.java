package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.TipoOcorrenciaDTO;
import com.example.projetoPBD.pontoCerto.service.TipoOcorrenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/tipos-ocorrencia", "/api/tipos-ocorrencia"})
public class TipoOcorrenciaController {

    private final TipoOcorrenciaService tipoOcorrenciaService;

    public TipoOcorrenciaController(TipoOcorrenciaService tipoOcorrenciaService) {
        this.tipoOcorrenciaService = tipoOcorrenciaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<TipoOcorrenciaDTO.Response> criar(
            @Valid @RequestBody TipoOcorrenciaDTO.CriarRequest req
    ) {
        TipoOcorrenciaDTO.Response resp = tipoOcorrenciaService.criar(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<TipoOcorrenciaDTO.Response>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean todos
    ) {
        boolean apenasAtivos = !todos;
        return ResponseEntity.ok(tipoOcorrenciaService.listar(apenasAtivos));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<TipoOcorrenciaDTO.Response> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody TipoOcorrenciaDTO.AtualizarRequest req
    ) {
        return ResponseEntity.ok(tipoOcorrenciaService.atualizar(id, req));
    }
}
