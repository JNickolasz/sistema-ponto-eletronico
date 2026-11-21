package com.example.projetoPBD.pontoCerto.resource;

import com.example.projetoPBD.pontoCerto.domain.Estacao;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BatidaDTO;
import com.example.projetoPBD.pontoCerto.repository.EstacaoRepository;
import com.example.projetoPBD.pontoCerto.security.RateLimiterService;
import com.example.projetoPBD.pontoCerto.service.BatidaService;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/estacao")
public class EstacaoController {

    private final BatidaService batidaService;
    private final EstacaoRepository estacaoRepository;
    private final RateLimiterService rateLimiterService;

    public EstacaoController(
            BatidaService batidaService,
            EstacaoRepository estacaoRepository,
            RateLimiterService rateLimiterService
    ) {
        this.batidaService = batidaService;
        this.estacaoRepository = estacaoRepository;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping(value = "/bater-ponto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatidaDTO.ComprovanteEstacaoResponse> baterPonto(
            @RequestParam("estacaoId") UUID estacaoId,
            @RequestParam("usuario") String usuario,
            @RequestParam("senha") String senha,
            @RequestPart("foto") MultipartFile foto,
            HttpServletRequest request
    ) {
        String ipCliente = request.getRemoteAddr();
        rateLimiterService.verificarLimite("estacao:" + estacaoId + ":" + ipCliente, 30, 60);
        BatidaDTO.ComprovanteEstacaoResponse comprovante = batidaService.registrarPontoEstacao(
                estacaoId, usuario, senha, foto, ipCliente
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(comprovante);
    }

    @GetMapping("/{estacaoId}/status")
    public ResponseEntity<Map<String, Object>> statusEstacao(@PathVariable UUID estacaoId) {
        Estacao estacao = estacaoRepository.findById(estacaoId)
                .orElseThrow(() -> new EquipamentoNaoEncontradoException("Estação não encontrada."));
        return ResponseEntity.ok(Map.of(
                "estacaoId", estacao.getId(),
                "identificacao", estacao.getIdentificacao(),
                "ativa", estacao.getStatus() == StatusEquipamento.ATIVO,
                "horaServidor", LocalDateTime.now()
        ));
    }
}
