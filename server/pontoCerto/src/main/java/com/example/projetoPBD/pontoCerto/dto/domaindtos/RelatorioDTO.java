package com.example.projetoPBD.pontoCerto.dto.domaindtos;

import com.example.projetoPBD.pontoCerto.domain.enums.AcaoTratamento;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusInconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoInconsistencia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RelatorioDTO {

    public record HorasExtrasColaboradorItem(
            UUID funcionarioId,
            String nomeCompleto,
            String matricula,
            String cargo,
            UUID localTrabalhoId,
            String localTrabalhoNome,
            UUID gestorId,
            String gestorNome,
            int diasComExtra,
            int totalMinutosExtras,
            Map<String, Integer> extrasPorFaixa
    ) {}

    public record HorasExtrasRelatorioResponse(
            LocalDate dataInicio,
            LocalDate dataFim,
            UUID localTrabalhoId,
            UUID gestorId,
            int totalMinutosExtrasGeral,
            Map<String, Integer> extrasPorFaixaGeral,
            List<HorasExtrasColaboradorItem> itens
    ) {}

    public record AbsenteismoColaboradorItem(
            UUID funcionarioId,
            String nomeCompleto,
            String matricula,
            String cargo,
            UUID localTrabalhoId,
            String localTrabalhoNome,
            UUID gestorId,
            String gestorNome,
            int diasComFalta,
            int diasComAtraso,
            int totalMinutosPrevistos,
            int totalMinutosFalta,
            int totalMinutosAtraso,
            int totalMinutosSaidaAntecipada,
            int totalMinutosAbonados,
            double percentualAbsenteismo
    ) {}

    public record AbsenteismoRelatorioResponse(
            LocalDate dataInicio,
            LocalDate dataFim,
            UUID localTrabalhoId,
            UUID gestorId,
            int totalMinutosFaltaGeral,
            int totalMinutosAtrasoGeral,
            int totalMinutosSaidaAntecipadaGeral,
            int totalMinutosAbonadosGeral,
            List<AbsenteismoColaboradorItem> itens
    ) {}

    public record BancoHorasColaboradorItem(
            UUID funcionarioId,
            String nomeCompleto,
            String matricula,
            String cargo,
            UUID localTrabalhoId,
            String localTrabalhoNome,
            UUID gestorId,
            String gestorNome,
            int saldoInicialMinutos,
            int creditosPeriodoMinutos,
            int debitosPeriodoMinutos,
            int compensacoesPeriodoMinutos,
            int saldoFinalMinutos
    ) {}

    public record BancoHorasRelatorioResponse(
            LocalDate dataInicio,
            LocalDate dataFim,
            UUID localTrabalhoId,
            UUID gestorId,
            int saldoGeralMinutos,
            List<BancoHorasColaboradorItem> itens
    ) {}

    public record PendenciaItem(
            UUID id,
            TipoInconsistencia tipo,
            StatusInconsistencia situacao,
            LocalDate dataReferencia,
            LocalDateTime dataHoraOrigem,
            UUID funcionarioId,
            String nomeColaborador,
            String matricula,
            String localTrabalhoNome,
            String gestorNome,
            String descricaoOrigem,
            AcaoTratamento acaoTomada,
            String tratadoPor,
            LocalDateTime tratadoEm,
            String motivoTratamento
    ) {}

    public record PendenciasRelatorioResponse(
            LocalDate dataInicio,
            LocalDate dataFim,
            UUID localTrabalhoId,
            UUID gestorId,
            int totalAbertas,
            int totalTratadas,
            List<PendenciaItem> abertas,
            List<PendenciaItem> tratadas
    ) {}
}
