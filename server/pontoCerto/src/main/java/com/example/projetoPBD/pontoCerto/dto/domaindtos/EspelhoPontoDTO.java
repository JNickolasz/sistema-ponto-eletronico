package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.enums.DiaSemana;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusApuracaoDiaria;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegistro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EspelhoPontoDTO {

    public record MarcacaoItem(
            UUID id,
            Long nsr,
            LocalDateTime dataHora,
            LocalTime horario,
            TipoRegistro tipoRegistro,
            TipoOrigem tipoOrigem,
            String origemLabel,
            String equipamentoNome,
            String motivoAjuste,
            String autorAjuste
    ) {}

    public record HorarioPrevistoDia(
            boolean diaTrabalho,
            String descricaoRegime,
            LocalTime horaEntrada,
            LocalTime intervaloInicio,
            LocalTime intervaloFim,
            LocalTime horaSaida,
            Integer cargaPrevistaMinutos,
            String observacao
    ) {}

    public record DiaEspelhoResponse(
            LocalDate data,
            DiaSemana diaSemana,
            boolean feriado,
            String descricaoFeriado,
            HorarioPrevistoDia previsto,
            List<MarcacaoItem> marcacoes,
            List<ApuracaoDiariaDTO.ParMarcacao> pares,
            StatusApuracaoDiaria statusApuracao,
            Integer minutosPrevistos,
            Integer minutosTrabalhados,
            Integer minutosIntervalo,
            Integer minutosAtraso,
            Integer minutosSaidaAntecipada,
            Integer minutosAdicionalNoturno,
            Integer minutosFalta,
            Integer minutosHoraExtraTotal,
            Map<String, Integer> horasExtrasPorFaixa,
            Integer minutosAbonados,
            UUID ocorrenciaId,
            String tipoOcorrenciaNome
    ) {}

    public record TotaisPeriodo(
            int totalDiasNoMes,
            int totalDiasTrabalhados,
            int totalMinutosPrevistos,
            int totalMinutosTrabalhados,
            int totalMinutosIntervalo,
            int totalMinutosAtraso,
            int totalMinutosSaidaAntecipada,
            int totalMinutosAdicionalNoturno,
            int totalMinutosFalta,
            int totalMinutosHoraExtra,
            int totalMinutosAbonados,
            Map<String, Integer> horasExtrasPorFaixa
    ) {}

    public record FechamentoInfo(
            StatusFechamentoPeriodo status,
            boolean periodoFechado,
            LocalDateTime dataFechamento,
            String fechadoPor,
            String avisoOuRessalva
    ) {}

    public record CabecalhoEmpresa(
            UUID id,
            String razaoSocial,
            String cnpj,
            String endereco
    ) {}

    public record CabecalhoColaborador(
            UUID id,
            String nomeCompleto,
            String matricula,
            String cpf,
            String pisPasep,
            String cargo,
            String localTrabalho
    ) {}

    public record EspelhoMensalResponse(
            String competencia,
            LocalDate dataInicio,
            LocalDate dataFim,
            CabecalhoEmpresa empresa,
            CabecalhoColaborador colaborador,
            FechamentoInfo fechamento,
            TotaisPeriodo totais,
            List<DiaEspelhoResponse> dias
    ) {}
}
