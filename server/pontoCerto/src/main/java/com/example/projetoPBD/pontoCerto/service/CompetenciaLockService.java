package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.FechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.repository.FechamentoPeriodoRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.PeriodoFechadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompetenciaLockService {

    private static final DateTimeFormatter MES_REF_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;

    public CompetenciaLockService(FechamentoPeriodoRepository fechamentoPeriodoRepository) {
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
    }

    @Transactional(readOnly = true)
    public void validarPeriodoAberto(UUID empresaId, LocalDate dataReferencia) {
        if (estaPeriodoFechado(empresaId, dataReferencia)) {
            String competencia = dataReferencia.format(MES_REF_FMT);
            throw new PeriodoFechadoException(
                    "O período " + competencia + " encontra-se fechado. Operação não permitida."
            );
        }
    }

    @Transactional(readOnly = true)
    public boolean estaPeriodoFechado(UUID empresaId, LocalDate dataReferencia) {
        if (empresaId == null || dataReferencia == null) {
            return false;
        }
        String competencia = dataReferencia.format(MES_REF_FMT);
        return estaCompetenciaFechada(empresaId, competencia);
    }

    @Transactional(readOnly = true)
    public boolean estaCompetenciaFechada(UUID empresaId, String competencia) {
        if (empresaId == null || competencia == null || competencia.isBlank()) {
            return false;
        }
        Optional<FechamentoPeriodo> fechamentoOpt = fechamentoPeriodoRepository
                .findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, competencia.trim());

        if (fechamentoOpt.isPresent()) {
            StatusFechamentoPeriodo status = fechamentoOpt.get().getStatus();
            return status == StatusFechamentoPeriodo.CONCLUIDO
                    || status == StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public YearMonth obterProximaCompetenciaAberta(UUID empresaId, LocalDate dataOriginal) {
        YearMonth ym = YearMonth.from(dataOriginal).plusMonths(1);
        int tentativas = 0;
        while (estaCompetenciaFechada(empresaId, ym.toString()) && tentativas < 60) {
            ym = ym.plusMonths(1);
            tentativas++;
        }
        return ym;
    }
}
