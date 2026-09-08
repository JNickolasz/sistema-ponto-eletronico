package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.dto.LoginDTO;
import com.example.projetoPBD.pontoCerto.dto.TokenResponseDTO;
import com.example.projetoPBD.pontoCerto.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}