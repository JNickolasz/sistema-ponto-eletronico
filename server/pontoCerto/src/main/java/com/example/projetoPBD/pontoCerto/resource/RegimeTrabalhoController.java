package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.HorarioPrevistoDTO;
import com.example.projetoPBD.pontoCerto.dto.RegimeTrabalhoDTO;
import com.example.projetoPBD.pontoCerto.service.RegimeTrabalhoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/regimes-trabalho", "/api/v1/regime-trabalho"})
public class RegimeTrabalhoController {

    private final RegimeTrabalhoService regimeTrabalhoService;

    public RegimeTrabalhoController(RegimeTrabalhoService regimeTrabalhoService) {
        this.regimeTrabalhoService = regimeTrabalhoService;
    }

    @PostMapping({"", "/vincular"})
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<RegimeTrabalhoDTO.Response> vincular(@Valid @RequestBody RegimeTrabalhoDTO.Vincular dto) {
        RegimeTrabalhoDTO.Response response = regimeTrabalhoService.vincular(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/funcionario/{funcionarioId}")
    @PreAuthorize("hasAnyRole('RH', 'GESTOR', 'COLABORADOR')")
    public ResponseEntity<List<RegimeTrabalhoDTO.Response>> listarPorFuncionario(@PathVariable UUID funcionarioId) {
        List<RegimeTrabalhoDTO.Response> lista = regimeTrabalhoService.listarPorFuncionario(funcionarioId);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/horario-previsto")
    @PreAuthorize("hasAnyRole('RH', 'GESTOR', 'COLABORADOR')")
    public ResponseEntity<HorarioPrevistoDTO.Response> obterHorarioPrevisto(
            @RequestParam UUID funcionarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        LocalDate dataConsulta = data != null ? data : LocalDate.now();
        HorarioPrevistoDTO.Response response = regimeTrabalhoService.obterHorarioPrevisto(funcionarioId, dataConsulta);
        return ResponseEntity.ok(response);
    }
}
