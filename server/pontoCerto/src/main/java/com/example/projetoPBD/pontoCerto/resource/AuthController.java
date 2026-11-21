package com.example.projetoPBD.pontoCerto.resource;


import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.dto.ErroResponseDTO;
import com.example.projetoPBD.pontoCerto.dto.LoginDTO;
import com.example.projetoPBD.pontoCerto.dto.TokenResponseDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.security.JwtService;
import com.example.projetoPBD.pontoCerto.security.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.AuthenticationException;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmpresaRepository empresaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            EmpresaRepository empresaRepository,
            FuncionarioRepository funcionarioRepository,
            PasswordEncoder passwordEncoder,
            RateLimiterService rateLimiterService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.empresaRepository = empresaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(
            @RequestBody @Valid LoginDTO loginDTO,
            @RequestHeader(value = "X-Tenant-Subdomain", required = false) String headerSubdomain,
            HttpServletRequest request
    ) {
        rateLimiterService.verificarLimite("login:" + request.getRemoteAddr(), 15, 60);

        String subdominio = (loginDTO.subdominio() != null && !loginDTO.subdominio().isBlank())
                ? loginDTO.subdominio().trim()
                : (headerSubdomain != null && !headerSubdomain.isBlank() ? headerSubdomain.trim() : null);

        Funcionario funcionario;
        if (subdominio != null) {
            Empresa empresa = empresaRepository.findBySubDominioIgnoreCase(subdominio)
                    .orElseThrow(() -> new BadCredentialsException("Usuário ou senha inválidos."));

            funcionario = funcionarioRepository.findByUsuarioAndEmpresaId(loginDTO.usuario().trim(), empresa.getId())
                    .orElseThrow(() -> new BadCredentialsException("Usuário ou senha inválidos."));

            if (!passwordEncoder.matches(loginDTO.senha(), funcionario.getSenhaHash())) {
                throw new BadCredentialsException("Usuário ou senha inválidos.");
            }
            if (!new FuncionarioUserDetails(funcionario).isEnabled()) {
                throw new DisabledException("Colaborador desligado ou inativo.");
            }
        } else {
            var authToken = new UsernamePasswordAuthenticationToken(loginDTO.usuario(), loginDTO.senha());
            var authentication = authenticationManager.authenticate(authToken);
            var userDetails = (FuncionarioUserDetails) authentication.getPrincipal();
            funcionario = userDetails.getFuncionario();
        }

        String token = jwtService.generateToken(funcionario);

        return ResponseEntity.ok(new TokenResponseDTO(
                token,
                "Bearer",
                funcionario.getUsuario(),
                funcionario.getPerfilAcesso().name()
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponseDTO> handleBadCredentials(HttpServletRequest request) {
        ErroResponseDTO erro = new ErroResponseDTO(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Não autorizado",
                "Usuário ou senha inválidos.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erro);
    }
}