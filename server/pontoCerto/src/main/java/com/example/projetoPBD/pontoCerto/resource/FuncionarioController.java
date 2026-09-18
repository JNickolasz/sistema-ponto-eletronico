package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.dto.FuncionarioDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.service.FuncionarioService;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaNaoEncontradaException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoNaEmpresa;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FuncionarioController {

    private final FuncionarioService service;
    private final FuncionarioRepository repository;
    private final EmpresaRepository empresaRepository;

    public FuncionarioController(FuncionarioService service, FuncionarioRepository repository, EmpresaRepository empresaRepository) {
        this.service = service;
        this.repository = repository;
        this.empresaRepository = empresaRepository;
    }

    @PostMapping("/rh/adicionar")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<FuncionarioDTO.Response> criarFuncionario(@Valid @RequestBody FuncionarioDTO.Criar dto) {

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
    public ResponseEntity<Map<String, Object>> verEspelhoColaborador(@PathVariable UUID id, Authentication authentication){
        // Por o ID ser do tipo UUID é impossível ter dois usuários com o mesmo UUID no mesmo
        // banco. Por isso n é preciso usar o metodo de findByIdAndEmpresaId()
        FuncionarioUserDetails userDetails = (FuncionarioUserDetails) authentication.getPrincipal();
        String usuarioLogado = userDetails.getUsername();
        UUID empresaId = userDetails.getFuncionario().getEmpresa().getId();

        Funcionario funcionario = repository.findByUsuarioAndEmpresaId(usuarioLogado, empresaId)
                .orElseThrow(() -> new UsuarioNaoEncontradoNaEmpresa("Usuário não encontrado na empresa"));

        if(!funcionario.getId().equals(id)) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("message", "Acesso negado: Você só pode acessar seu próprio espselho de ponto");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Acesso libetado ao seu espelho de ponto");
        response.put("espelhoId", id);
        response.put("donoDoESpelho", funcionario.getUsuario());

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

    @PatchMapping("/rh/funcionarios/{id}/desligar")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<FuncionarioDTO.Response> desligarFuncionario(
            @PathVariable UUID id,
            @RequestBody FuncionarioDTO.Desligamento dto) {
        FuncionarioDTO.Response response = service.desligar(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rh/funcionarios/{id}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Void> excluirFuncionario(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rh/funcionarios")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<List<FuncionarioDTO.Response>> buscarFuncionarios(
            @RequestParam UUID empresaId,
            @RequestParam(required = false) String termo) {
        List<FuncionarioDTO.Response> lista = service.buscar(empresaId, termo);
        return ResponseEntity.ok(lista);
    }

}
