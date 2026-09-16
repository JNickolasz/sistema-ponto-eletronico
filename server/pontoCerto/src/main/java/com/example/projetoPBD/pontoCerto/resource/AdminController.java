package com.example.projetoPBD.pontoCerto.resource;


import com.example.projetoPBD.pontoCerto.dto.domaindtos.EmpresaDTO;
import com.example.projetoPBD.pontoCerto.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.projetoPBD.pontoCerto.dto.EntityResponse;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final EmpresaService empresaService;

    public AdminController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }



    @PostMapping("/empresas")
    public ResponseEntity<EntityResponse<EmpresaDTO.Response>> cadastrarEmpresa(@Valid @RequestBody EmpresaDTO.Criar empresaDTO){

        System.out.println(">>> CHEGOU NO ADMIN CONTROLLER");

        EmpresaDTO.Response empresa = empresaService.cadastrarEmpresa(empresaDTO);

        EntityResponse<EmpresaDTO.Response> response = new EntityResponse<>(
                empresa,
                "Empresa Criada Com Sucesso!"
        );


        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/empresas")
    public ResponseEntity<java.util.List<EmpresaDTO.Response>> listarEmpresas() {
        java.util.List<EmpresaDTO.Response> empresas = empresaService.listarEmpresas();
        return ResponseEntity.ok(empresas);
    }

}
