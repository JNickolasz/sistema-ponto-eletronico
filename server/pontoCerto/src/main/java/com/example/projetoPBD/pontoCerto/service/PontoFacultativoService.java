package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.enums.Alcance;
import com.example.projetoPBD.pontoCerto.domain.PontoFacultativo;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.PontoFacultativoDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.PontoFacultativoRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PontoFacultativoService {

    private final PontoFacultativoRepository pontoFacultativoRepository;
    private final EmpresaRepository empresaRepository;
    private final SecurityTenantContext securityTenantContext;

    public PontoFacultativoService(
            PontoFacultativoRepository pontoFacultativoRepository,
            EmpresaRepository empresaRepository,
            SecurityTenantContext securityTenantContext
    ) {
        this.pontoFacultativoRepository = pontoFacultativoRepository;
        this.empresaRepository = empresaRepository;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional
    public PontoFacultativoDTO.Response cadastrar(PontoFacultativoDTO.Request dto) {
        validarCamposPorAlcance(dto.alcance(), dto.uf(), dto.municipio());

        if (dto.alcance() == Alcance.NACIONAL) {
            throw new AcessoNegadoException("Pontos facultativos nacionais/globais são gerenciados exclusivamente pelo administrador da plataforma.");
        }

        UUID empresaAtualId = securityTenantContext.empresaAtual();
        Empresa empresa = empresaRepository.findById(empresaAtualId)
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada."));

        return salvarPontoFacultativo(dto, empresa);
    }

    @Transactional
    public PontoFacultativoDTO.Response cadastrarGlobal(PontoFacultativoDTO.Request dto) {
        validarCamposPorAlcance(dto.alcance(), dto.uf(), dto.municipio());
        return salvarPontoFacultativo(dto, null);
    }

    private PontoFacultativoDTO.Response salvarPontoFacultativo(PontoFacultativoDTO.Request dto, Empresa empresa) {
        boolean jaExiste = pontoFacultativoRepository.existsPontoFacultativoConflitante(
                dto.data(), dto.alcance(), dto.uf(), dto.municipio(), empresa, null);

        if (jaExiste) {
            throw new PontoFacultativoExistenteException("Já existe um ponto facultativo cadastrado para esta data, alcance e localidade.");
        }

        PontoFacultativo ponto = new PontoFacultativo();
        ponto.setDescricao(dto.descricao());
        ponto.setAlcance(dto.alcance());
        ponto.setData(dto.data());
        ponto.setUf(dto.uf());
        ponto.setMunicipio(dto.municipio());
        ponto.setEmpresa(empresa);
        ponto.setAtivo(true);

        ponto = pontoFacultativoRepository.save(ponto);
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
        UUID empresaAtualId = securityTenantContext.empresaAtual();
        PontoFacultativo ponto = pontoFacultativoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ponto facultativo não encontrado com o ID informado."));

        if (ponto.getEmpresa() == null || !ponto.getEmpresa().getId().equals(empresaAtualId)) {
            throw new AcessoNegadoException("Acesso negado: você só pode inativar pontos facultativos pertencentes à sua própria empresa.");
        }

        ponto.setAtivo(false);
        pontoFacultativoRepository.save(ponto);
    }

    @Transactional
    public void inativarGlobal(UUID id) {
        PontoFacultativo ponto = pontoFacultativoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ponto facultativo não encontrado com o ID informado."));
        ponto.setAtivo(false);
        pontoFacultativoRepository.save(ponto);
    }

    @Transactional(readOnly = true)
    public List<PontoFacultativoDTO.Response> buscarCalendarioAnual(int ano, String uf, String municipio) {
        UUID empresaAtualId = securityTenantContext.empresaAtual();
        return pontoFacultativoRepository.buscarCalendarioAnual(ano, uf, municipio, empresaAtualId)
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