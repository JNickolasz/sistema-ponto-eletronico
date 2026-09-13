package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.FuncionarioDTO;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.service.FuncionarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FuncionarioController {

    private final FuncionarioService service;
    private final FuncionarioRepository repository;

    public FuncionarioController(FuncionarioService service, FuncionarioRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping("/rh/adicionar")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<FuncionarioDTO.Response> criarFuncionario(@RequestBody FuncionarioDTO.Criar dto) {

        // Só por hora enquanto ainda n temos o objeto de Empresa definido
        FuncionarioDTO.Response response = service.criar(dto, dto.empresaCnpj());
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
    public ResponseEntity<Map<String, Object>> verEspelhoColaborador(@PathVariable UUID id, Authentication authentication){
        var funcionarioLogado = repository.findByUsuario(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (!funcionarioLogado.getId().equals(id)) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("message", "Acesso negado: Você só pode acessar o seu próprio espelho de ponto.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("mensagem", "Acesso liberado ao seu espelho de ponto!");
        response.put("espelhoId", id);
        response.put("donoDoEspelho", funcionarioLogado.getUsuario());

        return ResponseEntity.ok(response);

    }

    @GetMapping("/gestor/painel")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<Map<String, Object>> rotaGestor(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("mensagem", "Acesso liberado para GESTOR");
        response.put("usuarioLogado", authentication.getName());
        response.put("permissoes", authentication.getAuthorities());

        return ResponseEntity.ok(response);
    }

}
