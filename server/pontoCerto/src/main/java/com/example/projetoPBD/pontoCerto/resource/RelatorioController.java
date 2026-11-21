package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.RelatorioDTO;
import com.example.projetoPBD.pontoCerto.service.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/relatorios")
@PreAuthorize("hasAnyRole('RH', 'GESTOR')")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/horas-extras")
    public ResponseEntity<RelatorioDTO.HorasExtrasRelatorioResponse> horasExtras(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) UUID localTrabalhoId,
            @RequestParam(required = false) UUID gestorId
    ) {
        return ResponseEntity.ok(relatorioService.gerarRelatorioHorasExtras(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorId
        ));
    }

    @GetMapping("/horas-extras/csv")
    public ResponseEntity<byte[]> horasExtrasCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) UUID localTrabalhoId,
            @RequestParam(required = false) UUID gestorId
    ) {
        String csv = relatorioService.exportarHorasExtrasCsv(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorId
        );
        return responderCsv(csv, "relatorio-horas-extras.csv");
    }

    @GetMapping("/absenteismo")
    public ResponseEntity<RelatorioDTO.AbsenteismoRelatorioResponse> absenteismo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) UUID localTrabalhoId,
            @RequestParam(required = false) UUID gestorId
    ) {
        return ResponseEntity.ok(relatorioService.gerarRelatorioAbsenteismo(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorId
        ));
    }

    @GetMapping("/absenteismo/csv")
    public ResponseEntity<byte[]> absenteismoCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) UUID localTrabalhoId,
            @RequestParam(required = false) UUID gestorId
    ) {
        String csv = relatorioService.exportarAbsenteismoCsv(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorId
        );
        return responderCsv(csv, "relatorio-absenteismo.csv");
    }

    @GetMapping("/banco-horas")
    public ResponseEntity<RelatorioDTO.BancoHorasRelatorioResponse> bancoHoras(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) UUID localTrabalhoId,
            @RequestParam(required = false) UUID gestorId
    ) {
        return ResponseEntity.ok(relatorioService.gerarRelatorioBancoHoras(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorId
        ));
    }

    @GetMapping("/banco-horas/csv")
    public ResponseEntity<byte[]> bancoHorasCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) UUID localTrabalhoId,
            @RequestParam(required = false) UUID gestorId
    ) {
        String csv = relatorioService.exportarBancoHorasCsv(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorId
        );
        return responderCsv(csv, "relatorio-banco-horas.csv");
    }

    @GetMapping("/pendencias")
    public ResponseEntity<RelatorioDTO.PendenciasRelatorioResponse> pendencias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) UUID localTrabalhoId,
            @RequestParam(required = false) UUID gestorId
    ) {
        return ResponseEntity.ok(relatorioService.gerarRelatorioPendencias(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorId
        ));
    }

    @GetMapping("/pendencias/csv")
    public ResponseEntity<byte[]> pendenciasCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) UUID localTrabalhoId,
            @RequestParam(required = false) UUID gestorId
    ) {
        String csv = relatorioService.exportarPendenciasCsv(
                dataInicio, dataFim, competencia, localTrabalhoId, gestorId
        );
        return responderCsv(csv, "relatorio-pendencias.csv");
    }

    private ResponseEntity<byte[]> responderCsv(String conteudo, String filename) {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] bodyBytes = conteudo.getBytes(StandardCharsets.UTF_8);
        byte[] resultado = new byte[bom.length + bodyBytes.length];
        System.arraycopy(bom, 0, resultado, 0, bom.length);
        System.arraycopy(bodyBytes, 0, resultado, bom.length, bodyBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(resultado);
    }
}
