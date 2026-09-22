package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Alcance;
import com.example.projetoPBD.pontoCerto.domain.PontoFacultativo;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.PontoFacultativoDTO;
import com.example.projetoPBD.pontoCerto.repository.PontoFacultativoRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.AlcanceInvalidoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.MunicipioCampoInvalidoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.PontoFacultativoExistenteException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UfCampoInvalidoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
        boolean jaExiste = pontoFacultativoRepository.existsPontoFacultativoConflitante(
                dto.data(), dto.alcance(), dto.uf(), dto.municipio(), null);

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
        ponto.setAtivo(true);

        // 4. Salva a entidade na base de dados[cite: 1]
        ponto = pontoFacultativoRepository.save(ponto);

        // 5. Retorna a resposta convertida para DTO via construtor
        return new PontoFacultativoDTO.Response(ponto);
    }

//    @Transactional
//    public PontoFacultativoDTO.Response atualizar(UUID id, PontoFacultativoDTO.Request dto) {
//        PontoFacultativo ponto = pontoFacultativoRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Ponto facultativo não encontrado com o ID informado."));
//
//        String ufNormalizada = sanitizarUf(dto.uf());
//        String municipioNormalizado = sanitizarMunicipio(dto.municipio());
//
//        // 1. Valida campos
//        validarCamposPorAlcance(dto.alcance(), ufNormalizada, municipioNormalizado);
//
//        // 2. Valida se já existe outro ponto conflitante ativo (ignorando o próprio ID atual)
//        boolean jaExiste = pontoFacultativoRepository.existsPontoFacultativoConflitante(
//                dto.data(), dto.alcance(), ufNormalizada, municipioNormalizado, id);
//
//        if (jaExiste) {
//            throw new PontoFacultativoExistenteException("Já existe outro ponto facultativo ativo com estes mesmos dados.");
//        }
//
//    // 3. Atualiza os dados
//            ponto.setDescricao(dto.descricao().trim());
//            ponto.setData(dto.data());
//            ponto.setAlcance(dto.alcance());
//            ponto.setUf(ufNormalizada);
//            ponto.setMunicipio(municipioNormalizado);
//
//            return new PontoFacultativoDTO.Response(pontoFacultativoRepository.save(ponto));
//}

    @Transactional
    public void inativar(UUID id) {
        PontoFacultativo ponto = pontoFacultativoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ponto facultativo não encontrado com o ID informado."));

        // Exclusão lógica: muda o status para inativo
        ponto.setAtivo(false);
        pontoFacultativoRepository.save(ponto);
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
            throw new AlcanceInvalidoException("O alcance do ponto facultativo é obrigatório.");
        }

        if (alcance == Alcance.ESTADUAL && (uf == null || uf.isBlank())) {
            throw new UfCampoInvalidoException("A UF é obrigatória para pontos facultativos estaduais.");
        }

        if (alcance == Alcance.MUNICIPAL) {
            if (uf == null || uf.isBlank()) {
                throw new UfCampoInvalidoException("A UF é obrigatória para pontos facultativos municipais.");
            }
            if (municipio == null || municipio.isBlank()) {
                throw new MunicipioCampoInvalidoException("O município é obrigatório para pontos facultativos municipais.");
            }
        }
    }
}