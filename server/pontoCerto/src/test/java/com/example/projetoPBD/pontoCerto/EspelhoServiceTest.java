package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.*;
import com.example.projetoPBD.pontoCerto.dto.HorarioPrevistoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ApuracaoDiariaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EspelhoPontoDTO;
import com.example.projetoPBD.pontoCerto.repository.*;
import com.example.projetoPBD.pontoCerto.service.ApuracaoDiariaService;
import com.example.projetoPBD.pontoCerto.service.EspelhoService;
import com.example.projetoPBD.pontoCerto.service.RegimeTrabalhoService;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspelhoServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private BatidaRepository batidaRepository;
    @Mock
    private ApuracaoDiariaRepository apuracaoDiariaRepository;
    @Mock
    private ApuracaoDiariaService apuracaoDiariaService;
    @Mock
    private RegimeTrabalhoService regimeTrabalhoService;
    @Mock
    private OcorrenciaColaboradorRepository ocorrenciaRepository;
    @Mock
    private FechamentoPeriodoRepository fechamentoPeriodoRepository;
    @Mock
    private FeriadoRepository feriadoRepository;

    private EspelhoService espelhoService;

    private Empresa empresa;
    private Empresa outraEmpresa;
    private Funcionario rh;
    private Funcionario gestor;
    private Funcionario colaboradorEquipe;
    private Funcionario colaboradorOutroTime;
    private Funcionario colaboradorOutraEmpresa;

    @BeforeEach
    void setUp() {
        espelhoService = new EspelhoService(
                funcionarioRepository,
                batidaRepository,
                apuracaoDiariaRepository,
                apuracaoDiariaService,
                regimeTrabalhoService,
                ocorrenciaRepository,
                fechamentoPeriodoRepository,
                feriadoRepository
        );

        empresa = new Empresa();
        empresa.setId(UUID.randomUUID());
        empresa.setRazaoSocial("Ponto Certo Tecnologia Ltda");
        empresa.setCnpj("12.345.678/0001-99");
        empresa.setEndereco("Av. Principal, 100");

        outraEmpresa = new Empresa();
        outraEmpresa.setId(UUID.randomUUID());
        outraEmpresa.setRazaoSocial("Empresa Concorrente S/A");
        outraEmpresa.setCnpj("98.765.432/0001-11");

        rh = criarFuncionario("RH Admin", "RH-01", PerfilAcesso.RH, empresa, null);
        gestor = criarFuncionario("Gestor Equipe", "GES-01", PerfilAcesso.GESTOR, empresa, null);
        colaboradorEquipe = criarFuncionario("João Silva", "COL-01", PerfilAcesso.COLABORADOR, empresa, gestor);
        colaboradorOutroTime = criarFuncionario("Maria Souza", "COL-02", PerfilAcesso.COLABORADOR, empresa, null);
        colaboradorOutraEmpresa = criarFuncionario("Carlos Externo", "EXT-01", PerfilAcesso.COLABORADOR, outraEmpresa, null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Funcionario criarFuncionario(String nome, String matricula, PerfilAcesso perfil, Empresa emp, Funcionario gestorResp) {
        Funcionario f = new Funcionario();
        f.setId(UUID.randomUUID());
        f.setNomeCompleto(nome);
        f.setMatricula(matricula);
        f.setCpf("111.222.333-44");
        f.setPisPasep("12345678901");
        f.setCargo("Analista");
        f.setPerfilAcesso(perfil);
        f.setEmpresa(emp);
        f.setGestor(gestorResp);
        f.setUsuario(matricula.toLowerCase());
        f.setSenhaHash("hash");
        return f;
    }

    private void autenticar(Funcionario f) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new FuncionarioUserDetails(f),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + f.getPerfilAcesso().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void mockarDadosBasicosMes(Funcionario alvo, LocalDate inicio, LocalDate fim) {
        lenient().when(batidaRepository.findAtivasByFuncionarioIdAndDataHoraBetweenOrderByDataHoraAsc(
                eq(alvo.getId()), any(), any())).thenReturn(List.of());
        lenient().when(apuracaoDiariaRepository.findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAsc(
                alvo.getId(), inicio, fim)).thenReturn(List.of());
        lenient().when(ocorrenciaRepository.findFiltradas(alvo.getEmpresa().getId(), alvo.getId(), inicio, fim))
                .thenReturn(List.of());
        lenient().when(feriadoRepository.buscarCalendarioAnual(eq(inicio.getYear()), any(), any(), eq(alvo.getEmpresa().getId())))
                .thenReturn(List.of());
        lenient().when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(
                alvo.getEmpresa().getId(), String.format("%04d-%02d", inicio.getYear(), inicio.getMonthValue())))
                .thenReturn(Optional.empty());
        lenient().when(regimeTrabalhoService.obterHorarioPrevisto(eq(alvo.getId()), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    LocalDate d = inv.getArgument(1);
                    return new HorarioPrevistoDTO.Response(
                            alvo.getId(), d, DiaSemana.from(d.getDayOfWeek()),
                            true, TipoRegime.JORNADA, "Padrão 08h-17h",
                            LocalTime.of(8, 0), LocalTime.of(17, 0),
                            LocalTime.of(12, 0), LocalTime.of(13, 0),
                            10, 480, "Dia útil"
                    );
                });
    }

    @Test
    @DisplayName("T18 Unit 1 - Espelho dia a dia com previsto, origens (Relógio, Estação, Ajuste) e totais por faixa")
    void testEspelhoDiaADiaOrigensETotais() {
        autenticar(colaboradorEquipe);
        when(funcionarioRepository.findById(colaboradorEquipe.getId())).thenReturn(Optional.of(colaboradorEquipe));

        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fim = LocalDate.of(2026, 11, 30);
        mockarDadosBasicosMes(colaboradorEquipe, inicio, fim);

        Batida b1 = new Batida();
        b1.setId(UUID.randomUUID());
        b1.setNsr(101L);
        b1.setDataHora(LocalDateTime.of(2026, 11, 10, 8, 0));
        b1.setTipoRegistro(TipoRegistro.ENTRADA);
        b1.setTipoOrigem(TipoOrigem.ARQUIVO_IMPORTADO);

        Batida b2 = new Batida();
        b2.setId(UUID.randomUUID());
        b2.setNsr(102L);
        b2.setDataHora(LocalDateTime.of(2026, 11, 10, 12, 0));
        b2.setTipoRegistro(TipoRegistro.SAIDA);
        b2.setTipoOrigem(TipoOrigem.ESTACAO);

        Batida b3 = new Batida();
        b3.setId(UUID.randomUUID());
        b3.setNsr(103L);
        b3.setDataHora(LocalDateTime.of(2026, 11, 10, 18, 30));
        b3.setTipoRegistro(TipoRegistro.SAIDA);
        b3.setTipoOrigem(TipoOrigem.AJUSTE_MANUAL);
        b3.setMotivoAjuste("Esquecimento na saída");

        when(batidaRepository.findAtivasByFuncionarioIdAndDataHoraBetweenOrderByDataHoraAsc(
                eq(colaboradorEquipe.getId()), any(), any())).thenReturn(List.of(b1, b2, b3));

        ApuracaoDiaria apDia10 = new ApuracaoDiaria();
        apDia10.setId(UUID.randomUUID());
        apDia10.setFuncionario(colaboradorEquipe);
        apDia10.setDataReferencia(LocalDate.of(2026, 11, 10));
        apDia10.setStatus(StatusApuracaoDiaria.APURADO);
        when(apuracaoDiariaRepository.findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAsc(
                colaboradorEquipe.getId(), inicio, fim)).thenReturn(List.of(apDia10));

        when(apuracaoDiariaService.toDTO(apDia10)).thenReturn(new ApuracaoDiariaDTO.Response(
                apDia10.getId(), colaboradorEquipe.getId(), colaboradorEquipe.getNomeCompleto(),
                colaboradorEquipe.getMatricula(), LocalDate.of(2026, 11, 10), StatusApuracaoDiaria.APURADO,
                480, 540, 60, 10, 0, 25, 0, 60, 0, null, null,
                List.of(), Map.of("50%", 60), LocalDateTime.now()
        ));

        EspelhoPontoDTO.EspelhoMensalResponse resp = espelhoService.consultarEspelhoMensal(null, "2026-11");

        assertEquals("2026-11", resp.competencia());
        assertEquals(30, resp.dias().size());
        assertEquals(30, resp.totais().totalDiasNoMes());
        assertEquals(540, resp.totais().totalMinutosTrabalhados());
        assertEquals(10, resp.totais().totalMinutosAtraso());
        assertEquals(25, resp.totais().totalMinutosAdicionalNoturno());
        assertEquals(60, resp.totais().totalMinutosHoraExtra());
        assertEquals(60, resp.totais().horasExtrasPorFaixa().get("50%"));

        EspelhoPontoDTO.DiaEspelhoResponse dia10 = resp.dias().get(9);
        assertEquals(LocalDate.of(2026, 11, 10), dia10.data());
        assertEquals(3, dia10.marcacoes().size());
        assertEquals("Relógio", dia10.marcacoes().get(0).origemLabel());
        assertEquals("Estação", dia10.marcacoes().get(1).origemLabel());
        assertEquals("Ajuste", dia10.marcacoes().get(2).origemLabel());
    }

    @Test
    @DisplayName("T18 Unit 2 - Controle de Acesso: Colaborador não pode ver espelho de outro colaborador")
    void testControleAcessoColaborador() {
        autenticar(colaboradorEquipe);
        when(funcionarioRepository.findById(colaboradorEquipe.getId())).thenReturn(Optional.of(colaboradorEquipe));
        when(funcionarioRepository.findById(colaboradorOutroTime.getId())).thenReturn(Optional.of(colaboradorOutroTime));

        assertThrows(AcessoNegadoException.class, () ->
                espelhoService.consultarEspelhoMensal(colaboradorOutroTime.getId(), "2026-11")
        );
    }

    @Test
    @DisplayName("T18 Unit 3 - Controle de Acesso: Gestor vê sua equipe, mas é bloqueado para colaborador de outra equipe")
    void testControleAcessoGestor() {
        autenticar(gestor);
        when(funcionarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
        when(funcionarioRepository.findById(colaboradorEquipe.getId())).thenReturn(Optional.of(colaboradorEquipe));
        when(funcionarioRepository.findById(colaboradorOutroTime.getId())).thenReturn(Optional.of(colaboradorOutroTime));

        mockarDadosBasicosMes(colaboradorEquipe, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        EspelhoPontoDTO.EspelhoMensalResponse ok = espelhoService.consultarEspelhoMensal(colaboradorEquipe.getId(), "2026-11");
        assertNotNull(ok);
        assertEquals(colaboradorEquipe.getId(), ok.colaborador().id());

        assertThrows(AcessoNegadoException.class, () ->
                espelhoService.consultarEspelhoMensal(colaboradorOutroTime.getId(), "2026-11")
        );
    }

    @Test
    @DisplayName("T18 Unit 4 - Controle de Acesso: RH acessa toda sua empresa, mas é bloqueado em outro tenant")
    void testControleAcessoRhETenant() {
        autenticar(rh);
        when(funcionarioRepository.findById(rh.getId())).thenReturn(Optional.of(rh));
        when(funcionarioRepository.findById(colaboradorOutroTime.getId())).thenReturn(Optional.of(colaboradorOutroTime));
        when(funcionarioRepository.findById(colaboradorOutraEmpresa.getId())).thenReturn(Optional.of(colaboradorOutraEmpresa));

        mockarDadosBasicosMes(colaboradorOutroTime, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        EspelhoPontoDTO.EspelhoMensalResponse ok = espelhoService.consultarEspelhoMensal(colaboradorOutroTime.getId(), "2026-11");
        assertNotNull(ok);

        assertThrows(AcessoNegadoException.class, () ->
                espelhoService.consultarEspelhoMensal(colaboradorOutraEmpresa.getId(), "2026-11")
        );
    }

    @Test
    @DisplayName("T18 Unit 5 - Espelho de Período Fechado indica status fechado, data de fechamento e gera HTML legível")
    void testEspelhoPeriodoFechadoEHtmlImpressao() {
        autenticar(colaboradorEquipe);
        when(funcionarioRepository.findById(colaboradorEquipe.getId())).thenReturn(Optional.of(colaboradorEquipe));

        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fim = LocalDate.of(2026, 10, 31);
        mockarDadosBasicosMes(colaboradorEquipe, inicio, fim);

        LocalDateTime dataFechamento = LocalDateTime.of(2026, 11, 2, 16, 45);
        FechamentoPeriodo fechamento = new FechamentoPeriodo();
        fechamento.setEmpresa(empresa);
        fechamento.setMesReferencia("2026-10");
        fechamento.setStatus(StatusFechamentoPeriodo.CONCLUIDO);
        fechamento.setDataFechamento(dataFechamento);
        fechamento.setFechadoPor("RH Admin");
        fechamento.setAvisoPendencias("Período fechado sem pendências.");

        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(
                empresa.getId(), "2026-10")).thenReturn(Optional.of(fechamento));

        EspelhoPontoDTO.EspelhoMensalResponse resp = espelhoService.consultarEspelhoMensal(null, "2026-10");
        assertTrue(resp.fechamento().periodoFechado());
        assertEquals(StatusFechamentoPeriodo.CONCLUIDO, resp.fechamento().status());
        assertEquals(dataFechamento, resp.fechamento().dataFechamento());
        assertEquals("RH Admin", resp.fechamento().fechadoPor());

        String html = espelhoService.gerarHtmlImpressao(null, "2026-10");
        assertTrue(html.contains("PERÍODO FECHADO em 02/11/2026 16:45"));
        assertTrue(html.contains("João Silva"));
        assertTrue(html.contains("@media print"));
    }
}
