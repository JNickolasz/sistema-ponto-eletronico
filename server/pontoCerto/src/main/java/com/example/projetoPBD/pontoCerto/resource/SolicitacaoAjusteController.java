package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.enums.StatusSolicitacaoAjuste;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.SolicitacaoAjusteDTO;
import com.example.projetoPBD.pontoCerto.service.SolicitacaoAjusteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/solicitacoes-ajuste", "/api/solicitacoes-ajuste"})
public class SolicitacaoAjusteController {

    private final SolicitacaoAjusteService solicitacaoAjusteService;

    public SolicitacaoAjusteController(SolicitacaoAjusteService solicitacaoAjusteService) {
        this.solicitacaoAjusteService = solicitacaoAjusteService;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoAjusteDTO.Response> abrirSolicitacao(
            @Valid @RequestBody SolicitacaoAjusteDTO.CriarRequest dto
    ) {
        SolicitacaoAjusteDTO.Response response = solicitacaoAjusteService.abrirSolicitacao(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<SolicitacaoAjusteDTO.Response>> listarMinhas() {
        return ResponseEntity.ok(solicitacaoAjusteService.listarMinhas());
    }

    @PreAuthorize("hasAnyRole('GESTOR', 'RH')")
    @GetMapping("/equipe")
    public ResponseEntity<List<SolicitacaoAjusteDTO.Response>> listarEquipe(
            @RequestParam(required = false) StatusSolicitacaoAjuste status
    ) {
        return ResponseEntity.ok(solicitacaoAjusteService.listarEquipe(status));
    }

    @PreAuthorize("hasAnyRole('GESTOR', 'RH')")
    @PostMapping("/{id}/analisar")
    public ResponseEntity<SolicitacaoAjusteDTO.Response> analisarSolicitacao(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitacaoAjusteDTO.AnalisarRequest dto
    ) {
        return ResponseEntity.ok(solicitacaoAjusteService.analisarSolicitacao(id, dto));
    }
}
