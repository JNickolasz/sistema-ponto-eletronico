package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Alcance;
import com.example.projetoPBD.pontoCerto.domain.PontoFacultativo;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.PontoFacultativoDTO;
import com.example.projetoPBD.pontoCerto.repository.PontoFacultativoRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.PontoFacultativoExistenteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PontoFacultativoService {

    private final PontoFacultativoRepository pontoFacultativoRepository;

    public PontoFacultativoService(PontoFacultativoRepository pontoFacultativoRepository) {
        this.pontoFacultativoRepository = pontoFacultativoRepository;
    }

    @Transactional
    public PontoFacultativoDTO.Response cadastrar(PontoFacultativoDTO.Request dto) {
        // 1. Valida a consistência da UF/Município de acordo com o alcance selecionado
        validarCamposPorAlcance(dto.alcance(), dto.uf(), dto.municipio());

        // 2. Valida a duplicidade na mesma data, alcance e localidade[cite: 1]
        boolean jaExiste = pontoFacultativoRepository.existsByDataAndAlcanceAndUfAndMunicipio(
                dto.data(), dto.alcance(), dto.uf(), dto.municipio());

        if (jaExiste) {
            throw new PontoFacultativoExistenteException("Já existe um ponto facultativo cadastrado para esta data, alcance e localidade.");
        }

        // 3. Cria e preenche a entidade PontoFacultativo
        PontoFacultativo ponto = new PontoFacultativo();
        ponto.setDescricao(dto.descricao());
        ponto.setAlcance(dto.alcance());
        ponto.setData(dto.data());
        ponto.setUf(dto.uf());
        ponto.setMunicipio(dto.municipio());

        // 4. Salva a entidade na base de dados[cite: 1]
        ponto = pontoFacultativoRepository.save(ponto);

        // 5. Retorna a resposta convertida para DTO via construtor
        return new PontoFacultativoDTO.Response(ponto);
    }

    @Transactional(readOnly = true)
    public List<PontoFacultativoDTO.Response> buscarCalendarioAnual(int ano, String uf, String municipio) {
        return pontoFacultativoRepository.buscarCalendarioAnual(ano, uf, municipio)
                .stream()
                .map(PontoFacultativoDTO.Response::new)
                .toList();
    }


    // VERIFICAR OS EXCEPTIONS AQUI DEPOIS, NO MOMENTO QUE ESTOU FAZENDO DEU PREGUIÇA DE AJEITAR
    // PERDÃO, feriadoservice e facultativoservice - LUAN 20/09 13H47
    private void validarCamposPorAlcance(Alcance alcance, String uf, String municipio) {
        if (alcance == null) {
            throw new IllegalArgumentException("O alcance do ponto facultativo é obrigatório.");
        }

        if (alcance == Alcance.ESTADUAL && (uf == null || uf.isBlank())) {
            throw new IllegalArgumentException("A UF é obrigatória para pontos facultativos estaduais.");
        }

        if (alcance == Alcance.MUNICIPAL) {
            if (uf == null || uf.isBlank()) {
                throw new IllegalArgumentException("A UF é obrigatória para pontos facultativos municipais.");
            }
            if (municipio == null || municipio.isBlank()) {
                throw new IllegalArgumentException("O município é obrigatório para pontos facultativos municipais.");
            }
        }
    }
}