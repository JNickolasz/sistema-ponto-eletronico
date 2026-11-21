package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.dto.HorarioPrevistoDTO;
import com.example.projetoPBD.pontoCerto.dto.RegimeTrabalhoDTO;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.service.RegimeTrabalhoService;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/regimes-trabalho", "/api/v1/regime-trabalho"})
public class RegimeTrabalhoController {

    private final RegimeTrabalhoService regimeTrabalhoService;
    private final FuncionarioRepository funcionarioRepository;

    public RegimeTrabalhoController(
            RegimeTrabalhoService regimeTrabalhoService,
            FuncionarioRepository funcionarioRepository
    ) {
        this.regimeTrabalhoService = regimeTrabalhoService;
        this.funcionarioRepository = funcionarioRepository;
    }

    @PostMapping({"", "/vincular"})
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<RegimeTrabalhoDTO.Response> vincular(@Valid @RequestBody RegimeTrabalhoDTO.Vincular dto) {
        RegimeTrabalhoDTO.Response response = regimeTrabalhoService.vincular(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/funcionario/{funcionarioId}")
    @PreAuthorize("hasAnyRole('RH', 'GESTOR', 'COLABORADOR')")
    public ResponseEntity<List<RegimeTrabalhoDTO.Response>> listarPorFuncionario(@PathVariable UUID funcionarioId) {
        validarAcessoAoColaborador(funcionarioId);
        List<RegimeTrabalhoDTO.Response> lista = regimeTrabalhoService.listarPorFuncionario(funcionarioId);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/horario-previsto")
    @PreAuthorize("hasAnyRole('RH', 'GESTOR', 'COLABORADOR')")
    public ResponseEntity<HorarioPrevistoDTO.Response> obterHorarioPrevisto(
            @RequestParam UUID funcionarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        validarAcessoAoColaborador(funcionarioId);
        LocalDate dataConsulta = data != null ? data : LocalDate.now();
        HorarioPrevistoDTO.Response response = regimeTrabalhoService.obterHorarioPrevisto(funcionarioId, dataConsulta);
        return ResponseEntity.ok(response);
    }

    private void validarAcessoAoColaborador(UUID alvoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof FuncionarioUserDetails fud)) {
            throw new AcessoNegadoException("Usuário não autenticado.");
        }
        Funcionario logado = funcionarioRepository.findById(fud.getFuncionario().getId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário autenticado não encontrado."));
        Funcionario alvo = funcionarioRepository.findById(alvoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));

        if (!alvo.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
            throw new AcessoNegadoException("Colaborador não pertence à sua empresa.");
        }
        if (logado.getId().equals(alvo.getId()) || logado.getPerfilAcesso() == PerfilAcesso.RH) {
            return;
        }
        if (logado.getPerfilAcesso() == PerfilAcesso.GESTOR) {
            boolean ehSubordinado = alvo.getGestor() != null && alvo.getGestor().getId().equals(logado.getId());
            if (ehSubordinado) {
                return;
            }
        }
        throw new AcessoNegadoException("Acesso negado para consultar regime de trabalho deste colaborador.");
    }
}
