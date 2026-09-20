package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.FeriadoDTO;
import com.example.projetoPBD.pontoCerto.service.FeriadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/feriado", "/api/v1/feriados"})
public class FeriadoController {

    private final FeriadoService feriadoService;

    public FeriadoController(FeriadoService feriadoService) {
        this.feriadoService = feriadoService;
    }

    @PostMapping
    public ResponseEntity<FeriadoDTO.Response> cadastrar(@Valid @RequestBody FeriadoDTO.Request dto) {
        FeriadoDTO.Response response = feriadoService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/calendario")
    public ResponseEntity<List<FeriadoDTO.Response>> buscarCalendario(
            @RequestParam("ano") int ano,
            @RequestParam(value = "uf", required = false) String uf,
            @RequestParam(value = "municipio", required = false) String municipio) {

        List<FeriadoDTO.Response> calendario = feriadoService.buscarCalendarioAnual(ano, uf, municipio);
        return ResponseEntity.ok(calendario);
    }
}