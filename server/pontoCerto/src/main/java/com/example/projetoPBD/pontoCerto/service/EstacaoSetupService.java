package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.config.ApiTimeProperties;
import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Estacao;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;
import com.example.projetoPBD.pontoCerto.domain.temp.PairingCode;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EstacaoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ttl.PairingCodeDTO;
import com.example.projetoPBD.pontoCerto.repository.EquipamentoRepository;
import com.example.projetoPBD.pontoCerto.repository.PairingCodeRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoInvalidoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoNaoEncontradoException;
import com.example.projetoPBD.pontoCerto.service.utils.PairingCodeGenerator;
import com.example.projetoPBD.pontoCerto.valueobjects.PairingCodeVO;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class EstacaoSetupService {

    private final EquipamentoRepository equipamentoRepository;
    private final SecurityTenantContext securityTenantContext;
    private final PairingCodeGenerator pairingCodeGenerator;
    private final PairingCodeRepository pairingCodeRepository;
    private final ApiTimeProperties apiTimeProperties;

    public EstacaoSetupService(EquipamentoRepository equipamentoRepository, SecurityTenantContext securityTenantContext, SecureRandom secureRandom, PairingCodeGenerator pairingCodeGenerator, PairingCodeRepository pairingCodeRepository, ApiTimeProperties apiTimeProperties) {
        this.equipamentoRepository = equipamentoRepository;
        this.securityTenantContext = securityTenantContext;

        this.pairingCodeGenerator = pairingCodeGenerator;
        this.pairingCodeRepository = pairingCodeRepository;
        this.apiTimeProperties = apiTimeProperties;
    }

    public PairingCodeDTO.Response autorizarEstacao(String codigoEquipamento) {
        Equipamento equipamento = equipamentoRepository.findByEmpresaIdAndCodigo(securityTenantContext.empresaAtual(), codigoEquipamento)
                .orElseThrow(() -> new EquipamentoNaoEncontradoException("Equipamento não encontrado com codigo: " + codigoEquipamento));

        if (equipamento.getTipo().equals(TipoEquipamento.RELOGIO)) throw new EquipamentoInvalidoException("Você so pode validar equipamentos do tipo Estação");

        Estacao estacao = (Estacao) equipamento;
        PairingCodeVO generatedCode = pairingCodeGenerator.generate(6);
        Instant expiresAt = Instant.now().plus(apiTimeProperties.getExpiration(), ChronoUnit.MINUTES);

        PairingCode pairingCode = new PairingCode(pairingCodeGenerator.hash(generatedCode.value()), expiresAt, estacao);
        pairingCode = pairingCodeRepository.save(pairingCode);

        return new PairingCodeDTO.Response(
                pairingCode.getId(),
                generatedCode.value(),
                pairingCode.getStatus(),
                pairingCode.getExpiresAt(),

                new EstacaoDTO.Response(
                        estacao.getId(),
                        estacao.getCodigo(),
                        estacao.getTipo(),
                        estacao.getIdentificacao(),
                        estacao.getStatus(),
                        estacao.getLocalTrabalho().getId(),
                        estacao.getCredentialId(),
                        estacao.getPublicKey(),
                        estacao.getStatusEstacao()
                ));

    }

}
