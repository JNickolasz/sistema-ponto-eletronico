package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.domain.OcorrenciaColaborador;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.OcorrenciaColaboradorDTO;
import com.example.projetoPBD.pontoCerto.service.OcorrenciaColaboradorService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/ocorrencias", "/api/ocorrencias"})
public class OcorrenciaController {

    private final OcorrenciaColaboradorService ocorrenciaService;

    public OcorrenciaController(OcorrenciaColaboradorService ocorrenciaService) {
        this.ocorrenciaService = ocorrenciaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<OcorrenciaColaboradorDTO.Response> lancarMultipart(
            @RequestParam("funcionarioId") UUID funcionarioId,
            @RequestParam("tipoOcorrenciaId") UUID tipoOcorrenciaId,
            @RequestParam("dataInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam("dataFim") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(value = "minutosAbonoPadrao", required = false) Integer minutosAbonoPadrao,
            @RequestParam(value = "observacao", required = false) String observacao,
            @RequestParam(value = "arquivo", required = false) MultipartFile arquivo
    ) {
        OcorrenciaColaboradorDTO.CriarRequest req = new OcorrenciaColaboradorDTO.CriarRequest(
                funcionarioId,
                tipoOcorrenciaId,
                dataInicio,
                dataFim,
                minutosAbonoPadrao,
                observacao
        );
        OcorrenciaColaboradorDTO.Response resp = ocorrenciaService.lancarOcorrencia(req, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<OcorrenciaColaboradorDTO.Response> lancarJson(
            @Valid @RequestBody OcorrenciaColaboradorDTO.CriarRequest req
    ) {
        OcorrenciaColaboradorDTO.Response resp = ocorrenciaService.lancarOcorrencia(req, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<OcorrenciaColaboradorDTO.Response>> listar(
            @RequestParam(required = false) UUID funcionarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        Funcionario logado = obterFuncionarioLogado();
        UUID alvoId = funcionarioId;
        if (logado != null && logado.getPerfilAcesso() == PerfilAcesso.COLABORADOR) {
            alvoId = logado.getId();
        }

        return ResponseEntity.ok(ocorrenciaService.listar(alvoId, dataInicio, dataFim));
    }

    @GetMapping("/{id}/anexo")
    public ResponseEntity<Resource> downloadAnexo(@PathVariable UUID id) {
        OcorrenciaColaborador o = ocorrenciaService.buscarPorId(id);
        Resource recurso = ocorrenciaService.carregarAnexo(id);

        String contentType = o.getContentTypeAnexo() != null ? o.getContentTypeAnexo() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String nomeOriginal = o.getNomeArquivoAnexo() != null ? o.getNomeArquivoAnexo() : "anexo";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomeOriginal + "\"")
                .body(recurso);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        ocorrenciaService.cancelarOcorrencia(id);
        return ResponseEntity.noContent().build();
    }

    private Funcionario obterFuncionarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails ud) {
            return ud.getFuncionario();
        }
        return null;
    }
}
