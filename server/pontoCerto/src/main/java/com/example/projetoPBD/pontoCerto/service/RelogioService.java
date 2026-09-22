package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Estacao;
import com.example.projetoPBD.pontoCerto.domain.LocalDeTrabalho;
import com.example.projetoPBD.pontoCerto.domain.Relogio;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EquipamentoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RelogioDTO;
import com.example.projetoPBD.pontoCerto.repository.RelogioRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoExistenteException;
import org.springframework.stereotype.Service;

@Service
public class RelogioService {

    private final RelogioRepository relogioRepository;

    public RelogioService(RelogioRepository relogioRepository) {
        this.relogioRepository = relogioRepository;
    }

    public Relogio criar(EquipamentoDTO.Criar criarDto, Empresa empresa, LocalDeTrabalho localDeTrabalho){

        String numFabricante = criarDto.relogio().numFabricacao();
        Long linhasImportadas = criarDto.relogio().linhasImportadas();

        if(relogioRepository.existsByNumeroFabricacaoAndEmpresaId(numFabricante, empresa.getId())){
            throw new EquipamentoExistenteException("Número de fabricante já cadastrado!");
        }

        Relogio relogio = new Relogio();

        relogio.setCodigo(criarDto.codigo());
        relogio.setEmpresa(empresa);
        relogio.setLocalTrabalho(localDeTrabalho);
        relogio.setIdentificacao(criarDto.identificacao());
        // relogio.setCreatedBy(); DEVE SER GERENCIADO AUTOMATICAMENTE PELO Auditor
        relogio.setNumFabricante(numFabricante);
        relogio.setLinhasImportadas(linhasImportadas);

        return relogio;
    }

}
