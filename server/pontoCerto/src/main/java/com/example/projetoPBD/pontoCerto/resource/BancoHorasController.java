package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BancoHorasDTO;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.service.BancoHorasService;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/banco-horas", "/api/banco-horas"})
public class BancoHorasController {

    private final BancoHorasService bancoHorasService;
    private final FuncionarioRepository funcionarioRepository;

    public BancoHorasController(
            BancoHorasService bancoHorasService,
            FuncionarioRepository funcionarioRepository
    ) {
        this.bancoHorasService = bancoHorasService;
        this.funcionarioRepository = funcionarioRepository;
    }

    @GetMapping("/extrato")
    public ResponseEntity<BancoHorasDTO.ExtratoPeriodoResponse> consultarExtrato(
            @RequestParam(required = false) UUID funcionarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String mesReferencia
    ) {
        Funcionario logado = obterFuncionarioLogado();
        UUID alvoId = funcionarioId != null ? funcionarioId : logado.getId();

        validarPermissaoAcesso(logado, alvoId);

        LocalDate[] intervalo = resolverIntervalo(dataInicio, dataFim, mesReferencia);
        BancoHorasDTO.ExtratoPeriodoResponse extrato = bancoHorasService.consultarExtrato(
                alvoId,
                intervalo[0],
                intervalo[1]
        );

        return ResponseEntity.ok(extrato);
    }

    @PostMapping("/compensacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'GESTOR')")
    public ResponseEntity<BancoHorasDTO.ItemExtratoResponse> registrarCompensacao(
            @RequestBody BancoHorasDTO.RegistrarCompensacaoRequest req
    ) {
        Funcionario logado = obterFuncionarioLogado();
        BancoHorasDTO.ItemExtratoResponse item = bancoHorasService.registrarCompensacao(logado, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @GetMapping("/equipe")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'GESTOR')")
    public ResponseEntity<List<BancoHorasDTO.SaldoColaboradorEquipeResponse>> consultarSaldoEquipe(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String mesReferencia
    ) {
        Funcionario logado = obterFuncionarioLogado();
        LocalDate[] intervalo = resolverIntervalo(dataInicio, dataFim, mesReferencia);
        List<BancoHorasDTO.SaldoColaboradorEquipeResponse> saldos = bancoHorasService.consultarSaldoEquipe(
                logado,
                intervalo[0],
                intervalo[1]
        );
        return ResponseEntity.ok(saldos);
    }

    private void validarPermissaoAcesso(Funcionario logado, UUID alvoId) {
        if (logado.getPerfilAcesso() == PerfilAcesso.COLABORADOR && !logado.getId().equals(alvoId)) {
            throw new AcessoNegadoException("Colaborador só pode consultar o próprio extrato de banco de horas.");
        }

        Funcionario alvo = funcionarioRepository.findById(alvoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));

        if (!alvo.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
            throw new AcessoNegadoException("Acesso negado ao extrato de outra empresa.");
        }
    }

    private LocalDate[] resolverIntervalo(LocalDate dataInicio, LocalDate dataFim, String mesReferencia) {
        if (dataInicio != null && dataFim != null) {
            return new LocalDate[]{dataInicio, dataFim};
        }

        YearMonth ym;
        if (mesReferencia != null && !mesReferencia.isBlank()) {
            ym = YearMonth.parse(mesReferencia.trim());
        } else {
            ym = YearMonth.now();
        }

        LocalDate inicio = dataInicio != null ? dataInicio : ym.atDay(1);
        LocalDate fim = dataFim != null ? dataFim : ym.atEndOfMonth();
        return new LocalDate[]{inicio, fim};
    }

    private Funcionario obterFuncionarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            return userDetails.getFuncionario();
        }
        throw new AcessoNegadoException("Usuário não autenticado.");
    }
}
