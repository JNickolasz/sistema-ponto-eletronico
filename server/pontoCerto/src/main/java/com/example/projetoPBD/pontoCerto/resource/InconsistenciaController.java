package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.Inconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoInconsistencia;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.InconsistenciaDTO;
import com.example.projetoPBD.pontoCerto.service.InconsistenciaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/inconsistencias", "/api/inconsistencias"})
@PreAuthorize("hasRole('RH')")
public class InconsistenciaController {

    private final InconsistenciaService inconsistenciaService;

    public InconsistenciaController(InconsistenciaService inconsistenciaService){
        this.inconsistenciaService = inconsistenciaService;
    }

    @GetMapping
    public ResponseEntity<List<InconsistenciaDTO.Response>>listar(
            @RequestParam(required = false) TipoInconsistencia tipo,
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false) UUID funcionarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false)String mesReferencia
            ) {
        return ResponseEntity.ok(
                inconsistenciaService.listar(tipo, situacao, funcionarioId, dataInicio, dataFim, mesReferencia)
        );
    }

    @GetMapping("/painel")
    public ResponseEntity<InconsistenciaDTO.PainelContagemResponse> obterPainel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false)String mesReferencia
            ) {
        return ResponseEntity.ok(
                inconsistenciaService.obterPainelContagem(dataInicio, dataFim, mesReferencia)
        );
    }

    @PostMapping("/{id}/tratar")
    public ResponseEntity<InconsistenciaDTO.Response> tratar(
            @PathVariable UUID id,
            @Valid @RequestBody InconsistenciaDTO.TratarRequest dto
    ) {
        return ResponseEntity.ok(inconsistenciaService.tratar(id, dto));
    }

    @PostMapping("/fechamento")
    public ResponseEntity<InconsistenciaDTO.FechamentoResponse> fecharPeriodo(
            @Valid @RequestBody InconsistenciaDTO.FechamentoRequest dto
    ) {
        return ResponseEntity.ok(inconsistenciaService.fecharPeriodo(dto));
    }

    @GetMapping("/fechamentos")
    public ResponseEntity<List<InconsistenciaDTO.FechamentoResponse>> listarFechamentos() {
        return ResponseEntity.ok(inconsistenciaService.listarFechamentos());
    }
}
