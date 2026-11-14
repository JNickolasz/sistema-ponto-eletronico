package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ApuracaoDiariaDTO;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.ApuracaoDiariaService;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/apuracao")
public class ApuracaoController {

    private final ApuracaoDiariaService apuracaoDiariaService;
    private final FuncionarioRepository funcionarioRepository;
    private final SecurityTenantContext securityTenantContext;

    public ApuracaoController(
            ApuracaoDiariaService apuracaoDiariaService,
            FuncionarioRepository funcionarioRepository,
            SecurityTenantContext securityTenantContext
    ) {
        this.apuracaoDiariaService = apuracaoDiariaService;
        this.funcionarioRepository = funcionarioRepository;
        this.securityTenantContext = securityTenantContext;
    }

    @GetMapping
    public ResponseEntity<ApuracaoDiariaDTO.ResumoPeriodoResponse> consultar(
            @RequestParam(required = false) UUID funcionarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String mesReferencia
    ) {
        Funcionario logado = obterFuncionarioLogado();
        UUID alvoId = funcionarioId != null ? funcionarioId : logado.getId();

        validarPermissaoAcesso(logado, alvoId);

        LocalDate[] intervalo = resolverIntervalo(dataInicio, dataFim, mesReferencia);
        ApuracaoDiariaDTO.ResumoPeriodoResponse resumo = apuracaoDiariaService.consultarPeriodo(
                alvoId,
                intervalo[0],
                intervalo[1]
        );

        return ResponseEntity.ok(resumo);
    }

    @PostMapping("/recalcular")
    @PreAuthorize("hasAnyRole('RH', 'GESTOR')")
    public ResponseEntity<ApuracaoDiariaDTO.ResumoPeriodoResponse> recalcular(
            @RequestBody ApuracaoDiariaDTO.RecalcularRequest request
    ) {
        Funcionario logado = obterFuncionarioLogado();
        UUID alvoId = request.funcionarioId() != null ? request.funcionarioId() : logado.getId();

        validarPermissaoAcesso(logado, alvoId);

        LocalDate inicio = request.dataInicio() != null ? request.dataInicio() : LocalDate.now().withDayOfMonth(1);
        LocalDate fim = request.dataFim() != null ? request.dataFim() : LocalDate.now();

        ApuracaoDiariaDTO.ResumoPeriodoResponse resumo = apuracaoDiariaService.recalcularPeriodo(
                alvoId,
                inicio,
                fim
        );

        return ResponseEntity.ok(resumo);
    }

    private void validarPermissaoAcesso(Funcionario logado, UUID alvoId) {
        if (logado.getId().equals(alvoId)) {
            return;
        }

        if (logado.getPerfilAcesso() == PerfilAcesso.RH) {
            Funcionario alvo = funcionarioRepository.findById(alvoId)
                    .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));
            if (!alvo.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                throw new AcessoNegadoException("Colaborador não pertence à sua empresa.");
            }
            return;
        }

        if (logado.getPerfilAcesso() == PerfilAcesso.GESTOR) {
            Funcionario alvo = funcionarioRepository.findById(alvoId)
                    .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));
            boolean ehLiderado = alvo.getGestor() != null && alvo.getGestor().getId().equals(logado.getId());
            if (!ehLiderado) {
                throw new AcessoNegadoException("Você só pode visualizar a apuração de colaboradores da sua equipe.");
            }
            return;
        }

        throw new AcessoNegadoException("Acesso negado para consultar apuração de outro colaborador.");
    }

    private LocalDate[] resolverIntervalo(LocalDate inicio, LocalDate fim, String mesReferencia) {
        if (inicio != null && fim != null) {
            return new LocalDate[]{inicio, fim};
        }
        if (mesReferencia != null && !mesReferencia.isBlank()) {
            YearMonth ym = YearMonth.parse(mesReferencia.trim());
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        }
        LocalDate hoje = LocalDate.now();
        return new LocalDate[]{hoje.withDayOfMonth(1), hoje};
    }

    private Funcionario obterFuncionarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            return funcionarioRepository.findById(userDetails.getFuncionario().getId())
                    .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado."));
        }
        throw new AcessoNegadoException("Usuário não autenticado.");
    }
}
