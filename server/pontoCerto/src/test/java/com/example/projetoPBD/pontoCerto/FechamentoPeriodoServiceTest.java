package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.FechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.HistoricoFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.Inconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.AcaoFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusInconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoInconsistencia;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FechamentoPeriodoDTO;
import com.example.projetoPBD.pontoCerto.repository.FechamentoPeriodoRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.HistoricoFechamentoPeriodoRepository;
import com.example.projetoPBD.pontoCerto.repository.InconsistenciaRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.BancoHorasService;
import com.example.projetoPBD.pontoCerto.service.FechamentoPeriodoService;
import com.example.projetoPBD.pontoCerto.service.exceptions.FechamentoComPendenciasException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FechamentoPeriodoServiceTest {

    @Mock
    private FechamentoPeriodoRepository fechamentoPeriodoRepository;

    @Mock
    private HistoricoFechamentoPeriodoRepository historicoRepository;

    @Mock
    private InconsistenciaRepository inconsistenciaRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private BancoHorasService bancoHorasService;

    @Mock
    private SecurityTenantContext securityTenantContext;

    private FechamentoPeriodoService fechamentoPeriodoService;

    private UUID empresaId;
    private Empresa empresa;

    @BeforeEach
    void setUp() {
        fechamentoPeriodoService = new FechamentoPeriodoService(
                fechamentoPeriodoRepository,
                historicoRepository,
                inconsistenciaRepository,
                funcionarioRepository,
                bancoHorasService,
                securityTenantContext
        );

        empresaId = UUID.randomUUID();
        empresa = new Empresa();
        empresa.setId(empresaId);
        empresa.setRazaoSocial("Empresa Teste T17");
    }

    @Test
    @DisplayName("T17 Unit 1 - Prévia de Fechamento: lista pendências abertas e identifica status aberto")
    void testConsultarPreviaFechamento() {
        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, "2026-10"))
                .thenReturn(Optional.empty());

        Inconsistencia pendencia = new Inconsistencia();
        pendencia.setId(UUID.randomUUID());
        pendencia.setEmpresa(empresa);
        pendencia.setTipo(TipoInconsistencia.DIA_IMPAR);
        pendencia.setSituacao(StatusInconsistencia.ABERTA);
        pendencia.setDataReferencia(LocalDate.of(2026, 10, 15));
        pendencia.setDataHoraOrigem(LocalDateTime.of(2026, 10, 15, 8, 0));
        pendencia.setDescricaoOrigem("Dia com batidas ímpares");

        when(inconsistenciaRepository.findByEmpresaIdAndSituacaoAndDataReferenciaBetweenOrderByDataHoraOrigemDesc(
                eq(empresaId), eq(StatusInconsistencia.ABERTA), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31))))
                .thenReturn(List.of(pendencia));

        FechamentoPeriodoDTO.PreviaResponse previa = fechamentoPeriodoService.consultarPreviaFechamento(empresaId, "2026-10");

        assertNotNull(previa);
        assertEquals(empresaId, previa.empresaId());
        assertEquals("2026-10", previa.competencia());
        assertEquals(StatusFechamentoPeriodo.ABERTO, previa.statusAtual());
        assertFalse(previa.periodoFechado());
        assertEquals(1, previa.totalPendenciasAbertas());
        assertEquals(1, previa.pendenciasAbertas().size());
        assertEquals(TipoInconsistencia.DIA_IMPAR, previa.pendenciasAbertas().get(0).tipo());
    }

    @Test
    @DisplayName("T17 Unit 2 - Fechamento Limpo: sem pendências conclui com CONCLUIDO e congela banco de horas")
    void testFecharPeriodoSemPendencias() {
        when(inconsistenciaRepository.findByEmpresaIdAndSituacaoAndDataReferenciaBetweenOrderByDataHoraOrigemDesc(
                eq(empresaId), eq(StatusInconsistencia.ABERTA), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31))))
                .thenReturn(Collections.emptyList());

        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, "2026-10"))
                .thenReturn(Optional.empty());

        when(fechamentoPeriodoRepository.saveAndFlush(any(FechamentoPeriodo.class)))
                .thenAnswer(invocation -> {
                    FechamentoPeriodo fp = invocation.getArgument(0);
                    fp.setId(UUID.randomUUID());
                    return fp;
                });

        FechamentoPeriodoDTO.FechamentoDetalheResponse response = fechamentoPeriodoService.fecharPeriodo(
                empresaId,
                "2026-10",
                false,
                "Fechamento regular mensal"
        );

        assertNotNull(response);
        assertEquals(StatusFechamentoPeriodo.CONCLUIDO, response.status());
        assertEquals(0, response.quantidadePendenciasAbertas());

        verify(bancoHorasService, times(1)).congelarPeriodoEmpresa(eq(empresaId), eq("2026-10"), any());

        ArgumentCaptor<HistoricoFechamentoPeriodo> histCaptor = ArgumentCaptor.forClass(HistoricoFechamentoPeriodo.class);
        verify(historicoRepository, times(1)).saveAndFlush(histCaptor.capture());
        assertEquals(AcaoFechamentoPeriodo.FECHAMENTO, histCaptor.getValue().getAcao());
    }

    @Test
    @DisplayName("T17 Unit 3 - Fechamento com Pendências Sem Confirmação: lança FechamentoComPendenciasException")
    void testFecharPeriodoComPendenciasSemConfirmacao() {
        Inconsistencia inc = new Inconsistencia();
        inc.setId(UUID.randomUUID());
        inc.setEmpresa(empresa);
        inc.setTipo(TipoInconsistencia.DUPLICADA_MINUTO);
        inc.setSituacao(StatusInconsistencia.ABERTA);

        when(inconsistenciaRepository.findByEmpresaIdAndSituacaoAndDataReferenciaBetweenOrderByDataHoraOrigemDesc(
                eq(empresaId), eq(StatusInconsistencia.ABERTA), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31))))
                .thenReturn(List.of(inc));

        FechamentoComPendenciasException ex = assertThrows(FechamentoComPendenciasException.class, () ->
                fechamentoPeriodoService.fecharPeriodo(empresaId, "2026-10", false, null)
        );

        assertTrue(ex.getMessage().contains("existem 1 inconsistência(s) aberta(s)"));
        verify(fechamentoPeriodoRepository, never()).saveAndFlush(any());
        verify(bancoHorasService, never()).congelarPeriodoEmpresa(any(), any(), any());
    }

    @Test
    @DisplayName("T17 Unit 4 - Fechamento com Pendências Confirmado: conclui com CONCLUIDO_COM_PENDENCIAS")
    void testFecharPeriodoComPendenciasConfirmado() {
        Inconsistencia inc = new Inconsistencia();
        inc.setId(UUID.randomUUID());
        inc.setEmpresa(empresa);
        inc.setTipo(TipoInconsistencia.SEQUENCIA_INVALIDA);
        inc.setSituacao(StatusInconsistencia.ABERTA);

        when(inconsistenciaRepository.findByEmpresaIdAndSituacaoAndDataReferenciaBetweenOrderByDataHoraOrigemDesc(
                eq(empresaId), eq(StatusInconsistencia.ABERTA), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31))))
                .thenReturn(List.of(inc));

        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, "2026-10"))
                .thenReturn(Optional.empty());

        when(fechamentoPeriodoRepository.saveAndFlush(any(FechamentoPeriodo.class)))
                .thenAnswer(invocation -> {
                    FechamentoPeriodo fp = invocation.getArgument(0);
                    fp.setId(UUID.randomUUID());
                    return fp;
                });

        FechamentoPeriodoDTO.FechamentoDetalheResponse response = fechamentoPeriodoService.fecharPeriodo(
                empresaId,
                "2026-10",
                true,
                "Folha de pagamento já gerada, pendência será ajustada via abono"
        );

        assertNotNull(response);
        assertEquals(StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS, response.status());
        assertEquals(1, response.quantidadePendenciasAbertas());
        assertNotNull(response.ressalvaOuAviso());

        verify(bancoHorasService, times(1)).congelarPeriodoEmpresa(eq(empresaId), eq("2026-10"), any());

        ArgumentCaptor<HistoricoFechamentoPeriodo> histCaptor = ArgumentCaptor.forClass(HistoricoFechamentoPeriodo.class);
        verify(historicoRepository, times(1)).saveAndFlush(histCaptor.capture());
        assertEquals(AcaoFechamentoPeriodo.FECHAMENTO_COM_PENDENCIAS, histCaptor.getValue().getAcao());
    }

    @Test
    @DisplayName("T17 Unit 5 - Reabertura Auditada: altera para ABERTO, descongela banco de horas e grava trilha")
    void testReaberturaAuditada() {
        FechamentoPeriodo fechamentoExistente = new FechamentoPeriodo();
        fechamentoExistente.setId(UUID.randomUUID());
        fechamentoExistente.setEmpresa(empresa);
        fechamentoExistente.setMesReferencia("2026-10");
        fechamentoExistente.setStatus(StatusFechamentoPeriodo.CONCLUIDO);

        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, "2026-10"))
                .thenReturn(Optional.of(fechamentoExistente));

        when(fechamentoPeriodoRepository.saveAndFlush(any(FechamentoPeriodo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FechamentoPeriodoDTO.FechamentoDetalheResponse response = fechamentoPeriodoService.reabrirPeriodo(
                empresaId,
                "2026-10",
                "Decisão judicial para retificação de ponto retroativo"
        );

        assertNotNull(response);
        assertEquals(StatusFechamentoPeriodo.ABERTO, response.status());

        verify(bancoHorasService, times(1)).descongelarPeriodoEmpresa(empresaId, "2026-10");

        ArgumentCaptor<HistoricoFechamentoPeriodo> histCaptor = ArgumentCaptor.forClass(HistoricoFechamentoPeriodo.class);
        verify(historicoRepository, times(1)).saveAndFlush(histCaptor.capture());
        assertEquals(AcaoFechamentoPeriodo.REABERTURA, histCaptor.getValue().getAcao());
        assertEquals("Decisão judicial para retificação de ponto retroativo", histCaptor.getValue().getMotivo());
    }

    @Test
    @DisplayName("T17 Unit 6 - Reabertura sem Motivo: lança IllegalArgumentException")
    void testReaberturaSemMotivoLancaExcecao() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                fechamentoPeriodoService.reabrirPeriodo(empresaId, "2026-10", "   ")
        );
        assertTrue(ex.getMessage().contains("O motivo da reabertura é obrigatório"));
    }
}
