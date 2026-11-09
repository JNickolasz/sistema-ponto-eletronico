package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Inconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusInconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoInconsistencia;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.InconsistenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InconsistenciaRegistroAutonomoService {

    private final InconsistenciaRepository inconsistenciaRepository;
    private final EmpresaRepository empresaRepository;

    public InconsistenciaRegistroAutonomoService(
            InconsistenciaRepository inconsistenciaRepository,
            EmpresaRepository empresaRepository
    ) {
        this.inconsistenciaRepository = inconsistenciaRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEquipamentoDesconhecidoAutonomo(
            UUID empresaId,
            String nomeArquivo,
            String identificadorEquipamento
    ) {
        if (empresaId == null) {
            return;
        }

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null) {
            return;
        }

        LocalDate hoje = LocalDate.now();
        String chave = "EQ_DESC" + identificadorEquipamento + "_" + nomeArquivo + "_" + hoje;

        if (inconsistenciaRepository.existsByEmpresaIdAndChaveDeduplicacao(empresaId, chave)) {
            return;
        }

        Inconsistencia inconsistencia = new Inconsistencia();
        inconsistencia.setEmpresa(empresa);
        inconsistencia.setTipo(TipoInconsistencia.EQUIPAMENTO_DESCONHECIDO);
        inconsistencia.setSituacao(StatusInconsistencia.ABERTA);
        inconsistencia.setNomeArquivo(nomeArquivo);
        inconsistencia.setIdentificadorRecebido(identificadorEquipamento);
        inconsistencia.setDataReferencia(hoje);
        inconsistencia.setDataHoraOrigem(LocalDateTime.now());
        inconsistencia.setChaveDeduplicacao(chave);
        inconsistencia.setDescricaoOrigem(
                "Importação do arquivo '" + nomeArquivo + "' referenciou equipamento desconhecido/não cadastrado (" + identificadorEquipamento + ")."
        );

        inconsistenciaRepository.saveAndFlush(inconsistencia);
    }


}
