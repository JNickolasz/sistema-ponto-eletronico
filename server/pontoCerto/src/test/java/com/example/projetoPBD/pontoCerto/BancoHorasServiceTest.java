package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusApuracaoDiaria;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoMovimentacaoBancoHoras;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BancoHorasDTO;
import com.example.projetoPBD.pontoCerto.repository.FechamentoBancoHorasRepository;
import com.example.projetoPBD.pontoCerto.repository.FechamentoPeriodoRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.MovimentacaoBancoHorasRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.BancoHorasService;
import com.example.projetoPBD.pontoCerto.service.exceptions.PeriodoFechadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BancoHorasServiceTest {

    @Mock
    private MovimentacaoBancoHorasRepository movimentacaoRepository;

    @Mock
    private FechamentoBancoHorasRepository fechamentoBancoHorasRepository;

    @Mock
    private FechamentoPeriodoRepository fechamentoPeriodoRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private SecurityTenantContext securityTenantContext;

    private BancoHorasService bancoHorasService;

    private Empresa empresa;
    private Funcionario colaborador;
    private Funcionario gestor;

    @BeforeEach
    void setUp() {
        bancoHorasService = new BancoHorasService(
                movimentacaoRepository,
                fechamentoBancoHorasRepository,
                fechamentoPeriodoRepository,
                funcionarioRepository,
                securityTenantContext
        );

        empresa = new Empresa();
        empresa.setId(UUID.randomUUID());
        empresa.setRazaoSocial("Empresa Teste T16");

        gestor = new Funcionario();
        gestor.setId(UUID.randomUUID());
        gestor.setEmpresa(empresa);
        gestor.setNomeCompleto("Gestor T16");
        gestor.setMatricula("GES-01");
        gestor.setPerfilAcesso(PerfilAcesso.GESTOR);

        colaborador = new Funcionario();
        colaborador.setId(UUID.randomUUID());
        colaborador.setEmpresa(empresa);
        colaborador.setGestor(gestor);
        colaborador.setNomeCompleto("Colaborador T16");
        colaborador.setMatricula("COL-01");
        colaborador.setCargo("Desenvolvedor");
        colaborador.setPerfilAcesso(PerfilAcesso.COLABORADOR);
    }

    @Test
    @DisplayName("T16 Unit 1 - Derivação Automática: 1h extra (+60m) e 30m atraso (-30m) geram lançamentos corretos")
    void testDerivacaoAutomaticaCreditoEDebito() {
        ApuracaoDiaria apDia1 = new ApuracaoDiaria();
        apDia1.setId(UUID.randomUUID());
        apDia1.setFuncionario(colaborador);
        apDia1.setDataReferencia(LocalDate.of(2026, 11, 10));
        apDia1.setStatus(StatusApuracaoDiaria.APURADO);
        apDia1.setMinutosHoraExtraTotal(60);
        apDia1.setMinutosAtraso(0);
        apDia1.setMinutosSaidaAntecipada(0);
        apDia1.setMinutosFalta(0);

        when(fechamentoBancoHorasRepository.findByFuncionarioIdAndMesReferencia(any(), any()))
                .thenReturn(Optional.empty());
        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(movimentacaoRepository.findByApuracaoDiariaIdAndTipo(apDia1.getId(), TipoMovimentacaoBancoHoras.CREDITO_APURACAO))
                .thenReturn(Optional.empty());
        when(movimentacaoRepository.findByApuracaoDiariaIdAndTipo(apDia1.getId(), TipoMovimentacaoBancoHoras.DEBITO_APURACAO))
                .thenReturn(Optional.empty());

        bancoHorasService.derivarMovimentacoesDaApuracao(apDia1);

        ArgumentCaptor<MovimentacaoBancoHoras> captor = ArgumentCaptor.forClass(MovimentacaoBancoHoras.class);
        verify(movimentacaoRepository, times(1)).saveAndFlush(captor.capture());

        MovimentacaoBancoHoras movSalva = captor.getValue();
        assertEquals(TipoMovimentacaoBancoHoras.CREDITO_APURACAO, movSalva.getTipo());
        assertEquals(60, movSalva.getMinutos());
        assertEquals(apDia1.getId(), movSalva.getApuracaoDiariaId());
    }

    @Test
    @DisplayName("T16 Unit 2 - Extrato com Saldo Inicial, Movimentações Progressivas e Saldo Final")
    void testExtratoSaldoInicialEFinal() {
        LocalDate inicio = LocalDate.of(2026, 11, 10);
        LocalDate fim = LocalDate.of(2026, 11, 20);

        when(funcionarioRepository.findById(colaborador.getId())).thenReturn(Optional.of(colaborador));
        when(fechamentoBancoHorasRepository.findFirstByFuncionarioIdAndMesReferenciaLessThanOrderByMesReferenciaDesc(
                colaborador.getId(), "2026-11"
        )).thenReturn(Optional.empty());
        // Acumulado até 09/11: +120 min (2h)
        when(movimentacaoRepository.somarSaldoAnterior(colaborador.getId(), inicio)).thenReturn(120);

        MovimentacaoBancoHoras m1 = new MovimentacaoBancoHoras();
        m1.setId(UUID.randomUUID());
        m1.setFuncionario(colaborador);
        m1.setDataReferencia(LocalDate.of(2026, 11, 12));
        m1.setTipo(TipoMovimentacaoBancoHoras.CREDITO_APURACAO);
        m1.setMinutos(60);
        m1.setDescricao("Crédito +01:00");
        m1.setRegistradoEm(LocalDateTime.now());

        MovimentacaoBancoHoras m2 = new MovimentacaoBancoHoras();
        m2.setId(UUID.randomUUID());
        m2.setFuncionario(colaborador);
        m2.setDataReferencia(LocalDate.of(2026, 11, 15));
        m2.setTipo(TipoMovimentacaoBancoHoras.DEBITO_APURACAO);
        m2.setMinutos(-30);
        m2.setDescricao("Débito -00:30");
        m2.setRegistradoEm(LocalDateTime.now());

        when(movimentacaoRepository.findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAscRegistradoEmAsc(
                colaborador.getId(), inicio, fim
        )).thenReturn(List.of(m1, m2));

        BancoHorasDTO.ExtratoPeriodoResponse extrato = bancoHorasService.consultarExtrato(colaborador.getId(), inicio, fim);

        assertEquals(120, extrato.saldoInicialMinutos());
        assertEquals(60, extrato.creditosPeriodoMinutos());
        assertEquals(30, extrato.debitosPeriodoMinutos());
        assertEquals(150, extrato.saldoFinalMinutos()); // 120 + 60 - 30 = 150
        assertEquals(2, extrato.movimentacoes().size());
        assertEquals(180, extrato.movimentacoes().get(0).saldoAcumuladoMinutos());
        assertEquals(150, extrato.movimentacoes().get(1).saldoAcumuladoMinutos());
    }

    @Test
    @DisplayName("T16 Unit 3 - Registro de Compensação: Abate do saldo acumulado e exige motivo")
    void testRegistroCompensacao() {
        LocalDate dataComp = LocalDate.of(2026, 11, 18);
        when(funcionarioRepository.findById(colaborador.getId())).thenReturn(Optional.of(colaborador));
        when(fechamentoBancoHorasRepository.findByFuncionarioIdAndMesReferencia(colaborador.getId(), "2026-11"))
                .thenReturn(Optional.empty());
        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresa.getId(), "2026-11"))
                .thenReturn(Optional.empty());
        when(movimentacaoRepository.saveAndFlush(any(MovimentacaoBancoHoras.class))).thenAnswer(inv -> {
            MovimentacaoBancoHoras m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(movimentacaoRepository.somarSaldoAnterior(colaborador.getId(), dataComp)).thenReturn(180);
        when(movimentacaoRepository.findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAscRegistradoEmAsc(
                colaborador.getId(), dataComp, dataComp
        )).thenAnswer(inv -> {
            MovimentacaoBancoHoras c = new MovimentacaoBancoHoras();
            c.setId(UUID.randomUUID());
            c.setFuncionario(colaborador);
            c.setDataReferencia(dataComp);
            c.setTipo(TipoMovimentacaoBancoHoras.COMPENSACAO);
            c.setMinutos(-120);
            c.setDescricao("Compensação de horas - Motivo: Saída antecipada médica");
            return List.of(c);
        });

        BancoHorasDTO.RegistrarCompensacaoRequest req = new BancoHorasDTO.RegistrarCompensacaoRequest(
                colaborador.getId(),
                dataComp,
                120,
                "Saída antecipada médica"
        );

        BancoHorasDTO.ItemExtratoResponse item = bancoHorasService.registrarCompensacao(gestor, req);

        assertNotNull(item);
        assertEquals(TipoMovimentacaoBancoHoras.COMPENSACAO, item.tipo());
        assertEquals(-120, item.minutos());
        assertEquals(60, item.saldoAcumuladoMinutos()); // 180 - 120 = 60
    }

    @Test
    @DisplayName("T16 Unit 4 - Imutabilidade de Período Fechado: Recusa compensação e preserva snapshot congelado (+10:00)")
    void testImutabilidadePeriodoFechado() {
        FechamentoBancoHoras snapOutubro = new FechamentoBancoHoras();
        snapOutubro.setId(UUID.randomUUID());
        snapOutubro.setFuncionario(colaborador);
        snapOutubro.setMesReferencia("2026-10");
        snapOutubro.setSaldoAnteriorMinutos(0);
        snapOutubro.setCreditosPeriodoMinutos(600);
        snapOutubro.setDebitosPeriodoMinutos(0);
        snapOutubro.setCompensacoesPeriodoMinutos(0);
        snapOutubro.setSaldoFinalCongeladoMinutos(600); // +10:00 congelados

        when(funcionarioRepository.findById(colaborador.getId())).thenReturn(Optional.of(colaborador));
        when(fechamentoBancoHorasRepository.findByFuncionarioIdAndMesReferencia(colaborador.getId(), "2026-10"))
                .thenReturn(Optional.of(snapOutubro));

        // 1. Tentativa de derivar nova apuração no mês fechado é ignorada
        ApuracaoDiaria apAlterada = new ApuracaoDiaria();
        apAlterada.setId(UUID.randomUUID());
        apAlterada.setFuncionario(colaborador);
        apAlterada.setDataReferencia(LocalDate.of(2026, 10, 15));
        apAlterada.setStatus(StatusApuracaoDiaria.APURADO);
        apAlterada.setMinutosHoraExtraTotal(0);
        apAlterada.setMinutosFalta(480);

        bancoHorasService.derivarMovimentacoesDaApuracao(apAlterada);
        verify(movimentacaoRepository, never()).saveAndFlush(any());

        // 2. Tentativa de compensar em mês fechado lança PeriodoFechadoException
        BancoHorasDTO.RegistrarCompensacaoRequest reqComp = new BancoHorasDTO.RegistrarCompensacaoRequest(
                colaborador.getId(),
                LocalDate.of(2026, 10, 20),
                60,
                "Tentativa em mês fechado"
        );
        assertThrows(PeriodoFechadoException.class, () -> bancoHorasService.registrarCompensacao(gestor, reqComp));

        // 3. Consulta do extrato de outubro continua retornando exatamente +10:00 (600 min)
        BancoHorasDTO.ExtratoPeriodoResponse extratoOutubro = bancoHorasService.consultarExtrato(
                colaborador.getId(),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)
        );
        assertEquals(600, extratoOutubro.saldoFinalMinutos());
    }

    @Test
    @DisplayName("T16 Unit 5 - Relatório de Equipe: Gestor visualiza saldos consolidados dos seus liderados")
    void testRelatorioSaldoEquipe() {
        LocalDate ini = LocalDate.of(2026, 11, 1);
        LocalDate fim = LocalDate.of(2026, 11, 30);

        when(funcionarioRepository.findByGestorIdOrderByNomeCompletoAsc(gestor.getId()))
                .thenReturn(List.of(colaborador));
        when(funcionarioRepository.findById(colaborador.getId())).thenReturn(Optional.of(colaborador));
        when(fechamentoBancoHorasRepository.findByFuncionarioIdAndMesReferencia(colaborador.getId(), "2026-11"))
                .thenReturn(Optional.empty());
        when(fechamentoBancoHorasRepository.findFirstByFuncionarioIdAndMesReferenciaLessThanOrderByMesReferenciaDesc(
                colaborador.getId(), "2026-11"
        )).thenReturn(Optional.empty());
        when(movimentacaoRepository.somarSaldoAnterior(colaborador.getId(), ini)).thenReturn(60);
        when(movimentacaoRepository.findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAscRegistradoEmAsc(
                colaborador.getId(), ini, fim
        )).thenReturn(List.of());

        List<BancoHorasDTO.SaldoColaboradorEquipeResponse> saldos = bancoHorasService.consultarSaldoEquipe(gestor, ini, fim);

        assertEquals(1, saldos.size());
        assertEquals(colaborador.getId(), saldos.get(0).funcionarioId());
        assertEquals(60, saldos.get(0).saldoAnteriorMinutos());
        assertEquals(60, saldos.get(0).saldoFinalMinutos());
    }

    @Test
    @DisplayName("T16 Unit 6 - Idempotência no Recálculo: Se dia recalculado zerar hora extra, remove lançamento anterior")
    void testIdempotenciaRecalculoRemoveLancamentoZerado() {
        UUID apuracaoId = UUID.randomUUID();
        MovimentacaoBancoHoras existente = new MovimentacaoBancoHoras();
        existente.setId(UUID.randomUUID());
        existente.setFuncionario(colaborador);
        existente.setApuracaoDiariaId(apuracaoId);
        existente.setTipo(TipoMovimentacaoBancoHoras.CREDITO_APURACAO);
        existente.setMinutos(60);
        existente.setPeriodoFechado(false);

        ApuracaoDiaria apRecalculada = new ApuracaoDiaria();
        apRecalculada.setId(apuracaoId);
        apRecalculada.setFuncionario(colaborador);
        apRecalculada.setDataReferencia(LocalDate.of(2026, 11, 10));
        apRecalculada.setStatus(StatusApuracaoDiaria.APURADO);
        apRecalculada.setMinutosHoraExtraTotal(0);
        apRecalculada.setMinutosAtraso(0);
        apRecalculada.setMinutosSaidaAntecipada(0);
        apRecalculada.setMinutosFalta(0);

        when(fechamentoBancoHorasRepository.findByFuncionarioIdAndMesReferencia(any(), any()))
                .thenReturn(Optional.empty());
        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(movimentacaoRepository.findByApuracaoDiariaIdAndTipo(apuracaoId, TipoMovimentacaoBancoHoras.CREDITO_APURACAO))
                .thenReturn(Optional.of(existente));
        when(movimentacaoRepository.findByApuracaoDiariaIdAndTipo(apuracaoId, TipoMovimentacaoBancoHoras.DEBITO_APURACAO))
                .thenReturn(Optional.empty());

        bancoHorasService.derivarMovimentacoesDaApuracao(apRecalculada);

        verify(movimentacaoRepository, times(1)).delete(existente);
    }
}
