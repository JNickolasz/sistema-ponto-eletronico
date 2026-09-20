package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.PontoFacultativoDTO;
import com.example.projetoPBD.pontoCerto.service.PontoFacultativoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/ponto-facultativo", "/api/v1/pontos-facultativos"})
public class PontoFacultativoController {

    private final PontoFacultativoService pontoFacultativoService;

    public PontoFacultativoController(PontoFacultativoService pontoFacultativoService) {
        this.pontoFacultativoService = pontoFacultativoService;
    }

    @PostMapping
    public ResponseEntity<PontoFacultativoDTO.Response> cadastrar(@Valid @RequestBody PontoFacultativoDTO.Request dto) {
        PontoFacultativoDTO.Response response = pontoFacultativoService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/calendario")
    public ResponseEntity<List<PontoFacultativoDTO.Response>> buscarCalendario(
            @RequestParam("ano") int ano,
            @RequestParam(value = "uf", required = false) String uf,
            @RequestParam(value = "municipio", required = false) String municipio) {

        List<PontoFacultativoDTO.Response> calendario = pontoFacultativoService.buscarCalendarioAnual(ano, uf, municipio);
        return ResponseEntity.ok(calendario);
    }
}