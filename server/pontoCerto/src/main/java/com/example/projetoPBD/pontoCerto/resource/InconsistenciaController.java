package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.enums.TipoInconsistencia;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FechamentoPeriodoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.InconsistenciaDTO;
import com.example.projetoPBD.pontoCerto.service.FechamentoPeriodoService;
import com.example.projetoPBD.pontoCerto.service.InconsistenciaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/inconsistencias", "/api/inconsistencias"})
@PreAuthorize("hasRole('RH')")
public class InconsistenciaController {

    private final InconsistenciaService inconsistenciaService;
    private final FechamentoPeriodoService fechamentoPeriodoService;

    public InconsistenciaController(
            InconsistenciaService inconsistenciaService,
            FechamentoPeriodoService fechamentoPeriodoService
    ){
        this.inconsistenciaService = inconsistenciaService;
        this.fechamentoPeriodoService = fechamentoPeriodoService;
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
        // Delega para FechamentoPeriodoService (T17) garantindo trilha de auditoria e congelamento do Banco de Horas
        FechamentoPeriodoDTO.FechamentoDetalheResponse det = fechamentoPeriodoService.fecharPeriodo(
                new FechamentoPeriodoDTO.FecharRequest(
                        dto.mesReferencia(),
                        dto.confirmarComPendencias(),
                        null
                )
        );
        return ResponseEntity.ok(new InconsistenciaDTO.FechamentoResponse(
                det.id(),
                det.competencia(),
                det.dataInicio(),
                det.dataFim(),
                det.fechadoEm(),
                det.fechadoPor(),
                det.status(),
                det.quantidadePendenciasAbertas(),
                det.ressalvaOuAviso(),
                det.quantidadePendenciasAbertas() > 0
        ));
    }

    @GetMapping("/fechamentos")
    public ResponseEntity<List<InconsistenciaDTO.FechamentoResponse>> listarFechamentos() {
        return ResponseEntity.ok(inconsistenciaService.listarFechamentos());
    }
}
