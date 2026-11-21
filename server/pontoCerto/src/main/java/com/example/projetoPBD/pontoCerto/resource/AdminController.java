package com.example.projetoPBD.pontoCerto.resource;


import com.example.projetoPBD.pontoCerto.dto.domaindtos.EmpresaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FeriadoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.PontoFacultativoDTO;
import com.example.projetoPBD.pontoCerto.service.EmpresaService;
import com.example.projetoPBD.pontoCerto.service.FeriadoService;
import com.example.projetoPBD.pontoCerto.service.PontoFacultativoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.projetoPBD.pontoCerto.dto.EntityResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final EmpresaService empresaService;
    private final FeriadoService feriadoService;
    private final PontoFacultativoService pontoFacultativoService;

    public AdminController(
            EmpresaService empresaService,
            FeriadoService feriadoService,
            PontoFacultativoService pontoFacultativoService
    ) {
        this.empresaService = empresaService;
        this.feriadoService = feriadoService;
        this.pontoFacultativoService = pontoFacultativoService;
    }

    @PostMapping("/empresas")
    public ResponseEntity<EntityResponse<EmpresaDTO.Response>> cadastrarEmpresa(@Valid @RequestBody EmpresaDTO.Criar empresaDTO){
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

    @PostMapping("/feriados")
    public ResponseEntity<FeriadoDTO.Response> cadastrarFeriadoGlobal(@Valid @RequestBody FeriadoDTO.Request dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feriadoService.cadastrarGlobal(dto));
    }

    @DeleteMapping("/feriados/{id}")
    public ResponseEntity<Void> inativarFeriadoGlobal(@PathVariable UUID id) {
        feriadoService.inativarGlobal(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pontos-facultativos")
    public ResponseEntity<PontoFacultativoDTO.Response> cadastrarPontoFacultativoGlobal(@Valid @RequestBody PontoFacultativoDTO.Request dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pontoFacultativoService.cadastrarGlobal(dto));
    }

    @DeleteMapping("/pontos-facultativos/{id}")
    public ResponseEntity<Void> inativarPontoFacultativoGlobal(@PathVariable UUID id) {
        pontoFacultativoService.inativarGlobal(id);
        return ResponseEntity.noContent().build();
    }

}
