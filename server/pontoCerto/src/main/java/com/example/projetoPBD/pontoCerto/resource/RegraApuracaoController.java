package com.example.projetoPBD.pontoCerto.resource;


import com.example.projetoPBD.pontoCerto.dto.domaindtos.RegraApuracaoDTO;
import com.example.projetoPBD.pontoCerto.service.RegraApuracaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"api/v1/regra-apuracao", "api/v1/regras-apuracao"})
@PreAuthorize("hasRole('RH')")
public class RegraApuracaoController {

    private final RegraApuracaoService regraApuracaoService;

    public RegraApuracaoController(RegraApuracaoService regraApuracaoService) {
        this.regraApuracaoService = regraApuracaoService;
    }


    @PostMapping
    public ResponseEntity<RegraApuracaoDTO.Response> criar(@Valid @RequestBody RegraApuracaoDTO.Criar regraDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(regraApuracaoService.cadastro(regraDto));
    }

    @GetMapping
    public ResponseEntity<List<RegraApuracaoDTO.Response>> listar(){

        List<RegraApuracaoDTO.Response> response = regraApuracaoService.listar();
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }


}
