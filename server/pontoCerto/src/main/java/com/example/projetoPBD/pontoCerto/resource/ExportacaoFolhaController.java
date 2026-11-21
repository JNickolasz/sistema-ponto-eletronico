package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.ExportacaoFolhaDTO;
import com.example.projetoPBD.pontoCerto.service.ExportacaoFolhaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/folha")
@PreAuthorize("hasRole('RH')")
public class ExportacaoFolhaController {

    private final ExportacaoFolhaService exportacaoFolhaService;

    public ExportacaoFolhaController(ExportacaoFolhaService exportacaoFolhaService) {
        this.exportacaoFolhaService = exportacaoFolhaService;
    }

    @GetMapping("/previa")
    public ResponseEntity<ExportacaoFolhaDTO.ResumoExportacaoResponse> consultarPrevia(
            @RequestParam String competencia
    ) {
        return ResponseEntity.ok(exportacaoFolhaService.consultarPreviaExportacao(competencia));
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam String competencia
    ) {
        ExportacaoFolhaDTO.ResumoExportacaoResponse resumo = exportacaoFolhaService.exportarFolha(competencia);
        String csv = exportacaoFolhaService.montarConteudoCsv(resumo.competencia(), resumo.linhas());

        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] bodyBytes = csv.getBytes(StandardCharsets.UTF_8);
        byte[] resultado = new byte[bom.length + bodyBytes.length];
        System.arraycopy(bom, 0, resultado, 0, bom.length);
        System.arraycopy(bodyBytes, 0, resultado, bom.length, bodyBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resumo.nomeArquivo() + "\"")
                .header("X-File-SHA256", resumo.hashSha256())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(resultado);
    }

    @GetMapping("/historico")
    public ResponseEntity<List<ExportacaoFolhaDTO.HistoricoItemResponse>> listarHistorico(
            @RequestParam(required = false) String competencia
    ) {
        return ResponseEntity.ok(exportacaoFolhaService.listarHistorico(competencia));
    }
}
