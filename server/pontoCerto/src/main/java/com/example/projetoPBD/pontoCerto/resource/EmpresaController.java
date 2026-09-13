package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.dto.EmpresaDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.service.EmpresaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresa")

public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaRepository repository, EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @PostMapping("/cadastrar")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<EmpresaDTO.Response> cadastrarEmpresa(@RequestBody EmpresaDTO.Request dto){
        EmpresaDTO.Response response = empresaService.cadastrarEmpresa(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
