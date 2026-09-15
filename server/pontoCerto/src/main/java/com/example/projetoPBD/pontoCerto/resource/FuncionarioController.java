package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.EntityResponse;
import com.example.projetoPBD.pontoCerto.dto.PainelDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FuncionarioDTO;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.service.FuncionarioService;
import com.example.projetoPBD.pontoCerto.service.PainelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FuncionarioController {

    private final FuncionarioService service;
    private final PainelService painelService;

    public FuncionarioController(FuncionarioService service, PainelService painelService) {
        this.service = service;
        this.painelService = painelService;
    }

    @PostMapping("/rh/adicionar")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<FuncionarioDTO.Response> criarFuncionario(@RequestBody FuncionarioDTO.Criar dto) {

        // Só por hora enquanto ainda n temos o objeto de Empresa definido
        FuncionarioDTO.Response response = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rh/painel")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Map<String, Object>> rotaRh(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("mensagem", "Acesso liberado para RH");
        response.put("usuarioLogado", authentication.getName());
        response.put("permissoes", authentication.getAuthorities());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/colaborador/painel")
    @PreAuthorize("hasRole('COLABORADOR')")
    public ResponseEntity<Map<String, Object>> rotaColaborador(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("mensagem", "Acesso liberado para COLABORADOR");
        response.put("usuarioLogado", authentication.getName());
        response.put("permissoes", authentication.getAuthorities());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/colaborador/{id}/espelho")
    @PreAuthorize("hasRole('COLABORADOR')")
    public ResponseEntity<EntityResponse<PainelDTO.AcessoEspelho>> verEspelhoColaborador(@PathVariable UUID id, Authentication authentication){

        var data = painelService.verEspelhoColaborador(id, authentication.getName());

        EntityResponse<PainelDTO.AcessoEspelho> response = new EntityResponse<>(
                data,
                "Acesso liberado ao seu espelho de ponto!");


        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @GetMapping("/gestor/painel")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<EntityResponse<PainelDTO.AcessoLiberado>> rotaGestor(Authentication authentication) {


        String usuarioLogado =  authentication.getName();
        String permissoes = authentication.getAuthorities().toString();
        PainelDTO.AcessoLiberado data = new PainelDTO.AcessoLiberado(usuarioLogado, permissoes);

        EntityResponse<PainelDTO.AcessoLiberado> response = new EntityResponse<>(
                data,
                "Acesso liberado para GESTOR");


        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
