package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.ImportacaoDTO;
import com.example.projetoPBD.pontoCerto.service.ImportacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('RH')")
@RequestMapping({"/api/v1/importacao", "/api/importacao"})
public class HistoricoImportacaoController {

    private final ImportacaoService importacaoService;

    public HistoricoImportacaoController(ImportacaoService importacaoService) {
        this.importacaoService = importacaoService;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacaoDTO.PreviewResponse> gerarPreview(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("equipamentoId") UUID equipamentoId
    ) {
        ImportacaoDTO.PreviewResponse response = importacaoService.gerarPreview(arquivo, equipamentoId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/confirmar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacaoDTO.HistoricoResponse> confirmar(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("equipamentoId") UUID equipamentoId
    ) {
        ImportacaoDTO.HistoricoResponse response = importacaoService.confirmarImportacao(arquivo, equipamentoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/historico")
    public ResponseEntity<List<ImportacaoDTO.HistoricoResponse>> listarHistorico() {
        List<ImportacaoDTO.HistoricoResponse> historico = importacaoService.listarHistorico();
        return ResponseEntity.ok(historico);
    }
}