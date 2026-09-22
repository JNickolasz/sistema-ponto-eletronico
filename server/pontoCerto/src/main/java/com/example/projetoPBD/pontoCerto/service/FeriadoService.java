package com.example.projetoPBD.pontoCerto.service;


import com.example.projetoPBD.pontoCerto.domain.Alcance;
import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Feriado;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FeriadoDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.FeriadoRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Service
public class FeriadoService {

    private final FeriadoRepository feriadoRepository;
    private final EmpresaRepository empresaRepository;

    public FeriadoService(FeriadoRepository feriadoRepository, EmpresaRepository empresaRepository) {
        this.feriadoRepository = feriadoRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    public FeriadoDTO.Response cadastrar(FeriadoDTO.Request dto){
//        caso um dia precise formatado
//        String ufNormalizada = sanitizarUf(dto.uf());
//        String municipioNormalizado = sanitizarMunicipio(dto.municipio());

        validarCamposPorAlcance(dto.alcance(), dto.uf(), dto.municipio());

        // feriado nacional não contém empresa
        if(dto.alcance() == Alcance.NACIONAL && dto.empresaId() != null) {
            throw new FeriadoNacionalEmpresaInvalidaException("Feriado nacional é válido para todo o sistema e não deve conter empresa vinculada.");
        }

        // 2. Busca a empresa se o ID for informado no DTO (Nulo = feriado nacional do sistema)
        Empresa empresa = null;
        if (dto.empresaId() != null) {
            empresa = empresaRepository.findById(dto.empresaId())
                    .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada com o ID informado."));
        }

        // validação de regra de conflito em feriados
        boolean jaExiste = feriadoRepository.existsFeriadoConflitante(
                dto.data(), dto.alcance(), dto.uf(), dto.municipio(), empresa, null);
        if (jaExiste){
            throw new FeriadoExistenteException("Feriado já existe no sistema");
        }

        Feriado feriado = new Feriado();
        feriado.setDescricao(dto.descricao());
        feriado.setAlcance(dto.alcance());
        feriado.setData(dto.data());
        feriado.setUf(dto.uf());
        feriado.setMunicipio(dto.municipio());
        feriado.setEmpresa(empresa);
        feriado.setAtivo(true);

        feriado = feriadoRepository.save(feriado);

        return new FeriadoDTO.Response(feriado);
    }
//     Futuro possivel método novo
//    @Transactional
//    public FeriadoDTO.Response atualizar(UUID id, FeriadoDTO.Request dto) {
//        Feriado feriado = feriadoRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Feriado não encontrado"));
//
//        String ufNormalizada = sanitizarUf(dto.uf());
//        String municipioNormalizado = sanitizarMunicipio(dto.municipio());
//
//        validarCamposPorAlcance(dto.alcance(), ufNormalizada, municipioNormalizado);
//
//        Empresa empresa = feriado.getEmpresa();
//        if (dto.alcance() == Alcance.NACIONAL) {
//            empresa = null;
//        } else if (dto.empresaId() != null) {
//            empresa = empresaRepository.findById(dto.empresaId())
//                    .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada com o ID informado."));
//        }
//
//        // Valida duplicidade excluindo o próprio ID atual
//        boolean jaExiste = feriadoRepository.existsFeriadoConflitante(
//                dto.data(), dto.alcance(), ufNormalizada, municipioNormalizado, empresa, id);
//        if (jaExiste) {
//            throw new FeriadoExistenteException("Já existe outro feriado conflitante com estes dados.");
//        }
//
//        feriado.setDescricao(dto.descricao().trim());
//        feriado.setData(dto.data());
//        feriado.setAlcance(dto.alcance());
//        feriado.setUf(ufNormalizada);
//        feriado.setMunicipio(municipioNormalizado);
//        feriado.setEmpresa(empresa);
//
//        return new FeriadoDTO.Response(feriadoRepository.save(feriado));
//    }

    @Transactional
    public void inativar(UUID id) {
        Feriado feriado = feriadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feriado não encontrado"));
        feriado.setAtivo(false); // Inativação lógica
        feriadoRepository.save(feriado);
    }

    @Transactional(readOnly = true)
    public List<FeriadoDTO.Response> buscarCalendarioAnual(int ano, String uf, String municipio, UUID empresaId) {
        return feriadoRepository.buscarCalendarioAnual(ano, uf, municipio, empresaId)
                .stream()
                .map(FeriadoDTO.Response::new)
                .toList();
    }

    private void validarCamposPorAlcance(@NotNull(message = "Alcance é obrigatório, nacional, estadual e municipal") Alcance alcance, String uf, String municipio) {
        if (alcance == null) {
            throw new AlcanceInvalidoException("O alcance do feriado é obrigatório.");
        }

        if (alcance == Alcance.ESTADUAL && (uf == null || uf.isBlank())) {
            throw new UfCampoInvalidoException("A UF é obrigatória para feriados estaduais.");
        }

        if (alcance == Alcance.MUNICIPAL) {
            if (uf == null || uf.isBlank()) {
                throw new UfCampoInvalidoException("A UF é obrigatória para feriados municipais.");
            }
            if (municipio == null || municipio.isBlank()) {
                throw new MunicipioCampoInvalidoException("O município é obrigatório para feriados municipais.");
            }
        }

    }
    // Utilitários de Case Sensitivity e Sanitização
    private String sanitizarUf(String uf) {
        return (uf != null && !uf.isBlank()) ? uf.trim().toUpperCase() : null;
    }

    private String sanitizarMunicipio(String municipio) {
        return (municipio != null && !municipio.isBlank()) ? municipio.trim() : null;
    }


}
