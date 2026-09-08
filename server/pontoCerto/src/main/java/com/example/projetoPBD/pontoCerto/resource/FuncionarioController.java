package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.FuncionarioDTO;
import com.example.projetoPBD.pontoCerto.service.FuncionarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/funcionarios")
public class FuncionarioController {

    private final FuncionarioService service;

    public FuncionarioController(FuncionarioService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<FuncionarioDTO.Response> criarFuncionario(@RequestBody FuncionarioDTO.Criar dto) {
        Long empresaId = 1L;
        // Só por hora enquanto ainda n temos o objeto de Empresa definido
        FuncionarioDTO.Response response = service.criar(dto, empresaId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
