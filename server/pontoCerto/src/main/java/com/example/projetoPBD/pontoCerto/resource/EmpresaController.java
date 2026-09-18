package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.dto.domaindtos.EmpresaDTO;
import com.example.projetoPBD.pontoCerto.service.EmpresaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/v1/api/empresa", "/v1/api/empresas"})
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<List<EmpresaDTO.Response>> listar() {
        List<EmpresaDTO.Response> empresas = empresaService.listarEmpresas();
        return ResponseEntity.ok(empresas);
    }
}
