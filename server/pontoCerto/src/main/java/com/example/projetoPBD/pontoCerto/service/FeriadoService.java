package com.example.projetoPBD.pontoCerto.service;


import com.example.projetoPBD.pontoCerto.domain.Alcance;
import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Feriado;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FeriadoDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.FeriadoRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaNaoEncontradaException;
import com.example.projetoPBD.pontoCerto.service.exceptions.FeriadoExistenteException;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


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
        validarCamposPorAlcance(dto.alcance(), dto.uf(), dto.municipio());

        // 2. Busca a empresa se o ID for informado no DTO (Nulo = feriado nacional do sistema)
        Empresa empresa = null;

        if (dto.empresaId() != null) {
            empresa = empresaRepository.findById(dto.empresaId())
                    .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada com o ID informado."));
        }

        boolean jaExiste = feriadoRepository.existsByDataAndAlcanceAndUfAndMunicipioAndEmpresa(
                dto.data(), dto.alcance(), dto.uf(), dto.municipio(), empresa);
        if (jaExiste){
            throw new FeriadoExistenteException("Feriado já existe no sistema");
        }

        Feriado feriado = new Feriado();
        feriado.setDescricao(dto.descricao());
        feriado.setAlcance(dto.alcance());
        feriado.setData(dto.data());
        feriado.setUf(dto.uf());
        feriado.setMunicipio(dto.municipio());

        feriado = feriadoRepository.save(feriado);

        return new FeriadoDTO.Response(feriado);
    }

    @Transactional(readOnly = true)
    public List<FeriadoDTO.Response> buscarCalendarioAnual(int ano, String uf, String municipio) {
        return feriadoRepository.buscarCalendarioAnual(ano, uf, municipio)
                .stream()
                .map(FeriadoDTO.Response::new)
                .toList();
    }

    // VERIFICAR OS EXCEPTIONS AQUI DEPOIS, NO MOMENTO QUE ESTOU FAZENDO DEU PREGUIÇA DE AJEITAR
    // PERDÃO, feriadoservice e facultativoservice - LUAN 20/09 13H41
    private void validarCamposPorAlcance(@NotNull(message = "Alcance é obrigatório, nacional, estadual e municipal") Alcance alcance, String uf, String municipio) {
        if (alcance == null) {
            throw new IllegalArgumentException("O alcance do feriado é obrigatório.");
        }

        if (alcance == Alcance.ESTADUAL && (uf == null || uf.isBlank())) {
            throw new IllegalArgumentException("A UF é obrigatória para feriados estaduais.");
        }

        if (alcance == Alcance.MUNICIPAL) {
            if (uf == null || uf.isBlank()) {
                throw new IllegalArgumentException("A UF é obrigatória para feriados municipais.");
            }
            if (municipio == null || municipio.isBlank()) {
                throw new IllegalArgumentException("O município é obrigatório para feriados municipais.");
            }
        }

    }


}
