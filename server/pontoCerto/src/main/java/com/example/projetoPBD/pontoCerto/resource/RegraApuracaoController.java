package com.example.projetoPBD.pontoCerto.resource;


import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RegraApuracaoDTO;
import com.example.projetoPBD.pontoCerto.service.RegraApuracaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"api/v1/regra-apuracao", "api/v1/regras-apuracao"})
public class RegraApuracaoController {

    private RegraApuracaoService regraApuracaoService;

    public RegraApuracaoController(RegraApuracaoService regraApuracaoService) {
        this.regraApuracaoService = regraApuracaoService;
    }


    @GetMapping
    public ResponseEntity<RegraApuracao> listar(){
        return null;
    }

    @PostMapping
    public ResponseEntity<RegraApuracao> criar(@Valid @RequestBody RegraApuracaoDTO.Criar regraDto){
        regraApuracaoService.cadastro(regraDto);
        return null;
    }


}
