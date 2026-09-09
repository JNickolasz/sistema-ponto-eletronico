package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.dto.ErroResponseDTO;
import com.example.projetoPBD.pontoCerto.dto.LoginDTO;
import com.example.projetoPBD.pontoCerto.dto.TokenResponseDTO;
import com.example.projetoPBD.pontoCerto.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import javax.security.sasl.AuthenticationException;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@RequestBody @Valid LoginDTO loginDTO) {
            var authToken = new UsernamePasswordAuthenticationToken(loginDTO.usuario(), loginDTO.senha());
            var authentication = authenticationManager.authenticate(authToken);

            var funcionario = (Funcionario) authentication.getPrincipal();

            String token = jwtService.generateToken(funcionario);

            return ResponseEntity.ok(new TokenResponseDTO(
                    token,
                    "Bearer",
                    funcionario.getUsuario(),
                    funcionario.getPerfilAcesso().name()
            ));
    }

    // Dessa forma, bem a grosso modo dizendo, ele faz um "Override"
    // sobrescrevendo a exception padrão. A AuthenticationException se refere a:
    // usuario inexistente ou errado, senha errada ou conta inativa...
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