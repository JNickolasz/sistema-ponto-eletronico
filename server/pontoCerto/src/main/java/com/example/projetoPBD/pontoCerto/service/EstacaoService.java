package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Estacao;
import com.example.projetoPBD.pontoCerto.domain.LocalDeTrabalho;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EquipamentoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EstacaoDTO;
import com.example.projetoPBD.pontoCerto.repository.EstacaoRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoExistenteException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class EstacaoService {


    @Transactional
    public Estacao criar(EquipamentoDTO.Criar criarDto, Empresa empresa, LocalDeTrabalho localDeTrabalho) {

        String credentialId = criarDto.estacao().credentialId();
        String publicKey = criarDto.estacao().publicKey();


        Estacao estacao = new Estacao();
        estacao.setCodigo(criarDto.codigo());
        estacao.setEmpresa(empresa);
        estacao.setLocalTrabalho(localDeTrabalho);
        estacao.setIdentificacao(criarDto.identificacao());
        // estacao.setCreatedBy(); DEVE SER GERENCIADO AUTOMATICAMENTE PELO Auditor
        estacao.setCredentialId(credentialId);
        estacao.setPublicKey(publicKey);

        return estacao;
    }

}
