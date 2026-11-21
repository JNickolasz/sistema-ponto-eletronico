package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.EspelhoPontoDTO;
import com.example.projetoPBD.pontoCerto.service.EspelhoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/espelho-ponto")
public class EspelhoController {

    private final EspelhoService espelhoService;

    public EspelhoController(EspelhoService espelhoService) {
        this.espelhoService = espelhoService;
    }

    @GetMapping
    public ResponseEntity<EspelhoPontoDTO.EspelhoMensalResponse> consultarEspelho(
            @RequestParam(required = false) UUID funcionarioId,
            @RequestParam(required = false) String competencia
    ) {
        return ResponseEntity.ok(espelhoService.consultarEspelhoMensal(funcionarioId, competencia));
    }

    @GetMapping(value = "/imprimir", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> imprimirEspelho(
            @RequestParam(required = false) UUID funcionarioId,
            @RequestParam(required = false) String competencia
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(espelhoService.gerarHtmlImpressao(funcionarioId, competencia));
    }
}
