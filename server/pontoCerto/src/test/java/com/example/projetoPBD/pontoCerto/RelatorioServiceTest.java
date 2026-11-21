package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.AcaoTratamento;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusApuracaoDiaria;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusInconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoInconsistencia;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ApuracaoDiariaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BancoHorasDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RelatorioDTO;
import com.example.projetoPBD.pontoCerto.repository.ApuracaoDiariaRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.InconsistenciaRepository;
import com.example.projetoPBD.pontoCerto.service.ApuracaoDiariaService;
import com.example.projetoPBD.pontoCerto.service.BancoHorasService;
import com.example.projetoPBD.pontoCerto.service.RelatorioService;
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
class RelatorioServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private ApuracaoDiariaRepository apuracaoDiariaRepository;
    @Mock
    private ApuracaoDiariaService apuracaoDiariaService;
    @Mock
    private BancoHorasService bancoHorasService;
    @Mock
    private InconsistenciaRepository inconsistenciaRepository;

    private RelatorioService relatorioService;

    private Empresa empresa;
    private LocalDeTrabalho localMatriz;
    private LocalDeTrabalho localFilial;
    private Funcionario rh;
    private Funcionario gestorA;
    private Funcionario gestorB;
    private Funcionario colabA1;
    private Funcionario colabA2Filial;
    private Funcionario colabB1;

    @BeforeEach
    void setUp() {
        relatorioService = new RelatorioService(
                funcionarioRepository,
                apuracaoDiariaRepository,
                apuracaoDiariaService,
                bancoHorasService,
                inconsistenciaRepository
        );

        empresa = new Empresa();
        empresa.setId(UUID.randomUUID());
        empresa.setRazaoSocial("Empresa Teste T19");

        localMatriz = new LocalDeTrabalho();
        localMatriz.setId(UUID.randomUUID());
        localMatriz.setNome("Matriz");

        localFilial = new LocalDeTrabalho();
        localFilial.setId(UUID.randomUUID());
        localFilial.setNome("Filial Sul");

        rh = criarFunc("RH", "RH-1", PerfilAcesso.RH, localMatriz, null);
        gestorA = criarFunc("Gestor A", "GES-A", PerfilAcesso.GESTOR, localMatriz, null);
        gestorB = criarFunc("Gestor B", "GES-B", PerfilAcesso.GESTOR, localFilial, null);

        colabA1 = criarFunc("Alice Equipe A", "COL-A1", PerfilAcesso.COLABORADOR, localMatriz, gestorA);
        colabA2Filial = criarFunc("Bruno Equipe A Filial", "COL-A2", PerfilAcesso.COLABORADOR, localFilial, gestorA);
        colabB1 = criarFunc("Carlos Equipe B", "COL-B1", PerfilAcesso.COLABORADOR, localFilial, gestorB);

        lenient().when(funcionarioRepository.findById(rh.getId())).thenReturn(Optional.of(rh));
        lenient().when(funcionarioRepository.findById(gestorA.getId())).thenReturn(Optional.of(gestorA));
        lenient().when(funcionarioRepository.findById(gestorB.getId())).thenReturn(Optional.of(gestorB));
        lenient().when(funcionarioRepository.findById(colabA1.getId())).thenReturn(Optional.of(colabA1));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Funcionario criarFunc(String nome, String mat, PerfilAcesso perfil, LocalDeTrabalho local, Funcionario gestor) {
        Funcionario f = new Funcionario();
        f.setId(UUID.randomUUID());
        f.setEmpresa(empresa);
        f.setNomeCompleto(nome);
        f.setMatricula(mat);
        f.setCargo("Analista");
        f.setPerfilAcesso(perfil);
        f.setLocalDeTrabalho(local);
        f.setGestor(gestor);
        f.setUsuario(mat.toLowerCase());
        f.setSenhaHash("hash");
        return f;
    }

    private void autenticar(Funcionario f) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new FuncionarioUserDetails(f),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + f.getPerfilAcesso().name()))
        ));
    }

    @Test
    @DisplayName("T19 Unit 1 - Horas Extras: Isolamento por equipe do Gestor, filtro por local e exportação CSV")
    void testRelatorioHorasExtrasComFiltrosEIsolamento() {
        autenticar(gestorA);
        when(funcionarioRepository.findByGestorIdOrderByNomeCompletoAsc(gestorA.getId()))
                .thenReturn(List.of(colabA1, colabA2Filial));

        ApuracaoDiaria apA1 = new ApuracaoDiaria();
        apA1.setId(UUID.randomUUID());
        apA1.setFuncionario(colabA1);
        apA1.setDataReferencia(LocalDate.of(2026, 11, 10));
        apA1.setMinutosHoraExtraTotal(90);

        ApuracaoDiaria apB1 = new ApuracaoDiaria();
        apB1.setId(UUID.randomUUID());
        apB1.setFuncionario(colabB1);
        apB1.setDataReferencia(LocalDate.of(2026, 11, 10));
        apB1.setMinutosHoraExtraTotal(300); // não deve aparecer para gestorA!

        when(apuracaoDiariaRepository.findByEmpresaIdAndDataReferenciaBetweenOrderByDataReferenciaAsc(
                eq(empresa.getId()), any(), any())).thenReturn(List.of(apA1, apB1));

        when(apuracaoDiariaService.toDTO(apA1)).thenReturn(new ApuracaoDiariaDTO.Response(
                apA1.getId(), colabA1.getId(), colabA1.getNomeCompleto(), colabA1.getMatricula(),
                apA1.getDataReferencia(), StatusApuracaoDiaria.APURADO,
                480, 570, 60, 0, 0, 0, 0, 90, 0, null, null,
                List.of(), Map.of("50%", 90), LocalDateTime.now()
        ));

        // 1. Consulta sem filtro de local -> vê apenas sua equipe (2 colaboradores), ignorando colabB1
        RelatorioDTO.HorasExtrasRelatorioResponse resp = relatorioService.gerarRelatorioHorasExtras(
                null, null, "2026-11", null, null
        );
        assertEquals(2, resp.itens().size());
        assertEquals(90, resp.totalMinutosExtrasGeral());
        assertEquals(90, resp.extrasPorFaixaGeral().get("50%"));

        // 2. Filtro por local Matriz -> apenas colabA1
        RelatorioDTO.HorasExtrasRelatorioResponse respMatriz = relatorioService.gerarRelatorioHorasExtras(
                null, null, "2026-11", localMatriz.getId(), null
        );
        assertEquals(1, respMatriz.itens().size());
        assertEquals(colabA1.getId(), respMatriz.itens().get(0).funcionarioId());

        // 3. Exportação CSV
        String csv = relatorioService.exportarHorasExtrasCsv(null, null, "2026-11", localMatriz.getId(), null);
        assertTrue(csv.contains("Colaborador;Matricula;Cargo;LocalTrabalho;Gestor"));
        assertTrue(csv.contains("Alice Equipe A;COL-A1;Analista;Matriz;Gestor A;1;90;01:30;50%=01:30"));

        // 4. Gestor A tentando filtrar pela equipe de Gestor B -> AcessoNegadoException
        assertThrows(AcessoNegadoException.class, () ->
                relatorioService.gerarRelatorioHorasExtras(null, null, "2026-11", null, gestorB.getId())
        );

        // 5. Colaborador tentando acessar relatório -> AcessoNegadoException
        autenticar(colabA1);
        assertThrows(AcessoNegadoException.class, () ->
                relatorioService.gerarRelatorioHorasExtras(null, null, "2026-11", null, null)
        );
    }

    @Test
    @DisplayName("T19 Unit 2 - Absenteísmo: Calcula faltas, atrasos, percentual e exporta CSV")
    void testRelatorioAbsenteismo() {
        autenticar(rh);
        when(funcionarioRepository.findByEmpresaIdOrderByNomeCompletoAsc(empresa.getId()))
                .thenReturn(List.of(colabA1, colabB1));

        ApuracaoDiaria apFalta = new ApuracaoDiaria();
        apFalta.setId(UUID.randomUUID());
        apFalta.setFuncionario(colabA1);
        apFalta.setDataReferencia(LocalDate.of(2026, 11, 5));
        apFalta.setMinutosPrevistos(480);
        apFalta.setMinutosFalta(480);
        apFalta.setMinutosAtraso(0);

        ApuracaoDiaria apAtraso = new ApuracaoDiaria();
        apAtraso.setId(UUID.randomUUID());
        apAtraso.setFuncionario(colabA1);
        apAtraso.setDataReferencia(LocalDate.of(2026, 11, 6));
        apAtraso.setMinutosPrevistos(480);
        apAtraso.setMinutosFalta(0);
        apAtraso.setMinutosAtraso(30);

        when(apuracaoDiariaRepository.findByEmpresaIdAndDataReferenciaBetweenOrderByDataReferenciaAsc(
                eq(empresa.getId()), any(), any())).thenReturn(List.of(apFalta, apAtraso));

        // RH filtrando pela equipe de gestorA -> traz apenas colabA1
        RelatorioDTO.AbsenteismoRelatorioResponse resp = relatorioService.gerarRelatorioAbsenteismo(
                null, null, "2026-11", null, gestorA.getId()
        );

        assertEquals(1, resp.itens().size());
        RelatorioDTO.AbsenteismoColaboradorItem item = resp.itens().get(0);
        assertEquals(1, item.diasComFalta());
        assertEquals(1, item.diasComAtraso());
        assertEquals(480, item.totalMinutosFalta());
        assertEquals(30, item.totalMinutosAtraso());
        assertEquals(53.13, item.percentualAbsenteismo()); // (510 / 960) * 100 = 53.125 -> 53.13

        String csv = relatorioService.exportarAbsenteismoCsv(null, null, "2026-11", null, gestorA.getId());
        assertTrue(csv.contains("Colaborador;Matricula;LocalTrabalho;Gestor;DiasComFalta;DiasComAtraso"));
        assertTrue(csv.contains("Alice Equipe A;COL-A1;Matriz;Gestor A;1;1;08:00;00:30;00:00;00:00;53.13%"));
    }

    @Test
    @DisplayName("T19 Unit 3 - Banco de Horas: Consolida saldo por colaborador da equipe e exporta CSV")
    void testRelatorioBancoHoras() {
        autenticar(gestorA);
        when(funcionarioRepository.findByGestorIdOrderByNomeCompletoAsc(gestorA.getId()))
                .thenReturn(List.of(colabA1));

        when(bancoHorasService.consultarExtrato(
                eq(colabA1.getId()), eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 30))))
                .thenReturn(new BancoHorasDTO.ExtratoPeriodoResponse(
                        colabA1.getId(), colabA1.getNomeCompleto(), colabA1.getMatricula(),
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                        60, 120, -30, -60, 90, List.of()
                ));

        RelatorioDTO.BancoHorasRelatorioResponse resp = relatorioService.gerarRelatorioBancoHoras(
                null, null, "2026-11", null, null
        );

        assertEquals(1, resp.itens().size());
        assertEquals(90, resp.saldoGeralMinutos());
        assertEquals(60, resp.itens().get(0).saldoInicialMinutos());
        assertEquals(90, resp.itens().get(0).saldoFinalMinutos());

        String csv = relatorioService.exportarBancoHorasCsv(null, null, "2026-11", null, null);
        assertTrue(csv.contains("Colaborador;Matricula;LocalTrabalho;Gestor;SaldoInicialHHMM"));
        assertTrue(csv.contains("Alice Equipe A;COL-A1;Matriz;Gestor A;01:00;02:00;-00:30;-01:00;01:30;90"));
    }

    @Test
    @DisplayName("T19 Unit 4 - Pendências: Separa abertas e tratadas respeitando isolamento de equipe e exporta CSV")
    void testRelatorioPendenciasSeparaAbertasETratadas() {
        autenticar(gestorA);
        when(funcionarioRepository.findByGestorIdOrderByNomeCompletoAsc(gestorA.getId()))
                .thenReturn(List.of(colabA1));

        Inconsistencia incAberta = new Inconsistencia();
        incAberta.setId(UUID.randomUUID());
        incAberta.setEmpresa(empresa);
        incAberta.setFuncionario(colabA1);
        incAberta.setTipo(TipoInconsistencia.DIA_IMPAR);
        incAberta.setSituacao(StatusInconsistencia.ABERTA);
        incAberta.setDataReferencia(LocalDate.of(2026, 11, 12));
        incAberta.setDataHoraOrigem(LocalDateTime.of(2026, 11, 12, 8, 0));
        incAberta.setDescricaoOrigem("Falta marcação de saída");

        Inconsistencia incTratada = new Inconsistencia();
        incTratada.setId(UUID.randomUUID());
        incTratada.setEmpresa(empresa);
        incTratada.setFuncionario(colabA1);
        incTratada.setTipo(TipoInconsistencia.FORA_DA_JORNADA);
        incTratada.setSituacao(StatusInconsistencia.RESOLVIDA);
        incTratada.setDataReferencia(LocalDate.of(2026, 11, 15));
        incTratada.setDataHoraOrigem(LocalDateTime.of(2026, 11, 15, 9, 0));
        incTratada.setDescricaoOrigem("Feriado");
        incTratada.setAcaoTomada(AcaoTratamento.ABONAR_DIA);
        incTratada.setTratadoPor("Gestor A");
        incTratada.setTratadoEm(LocalDateTime.of(2026, 11, 16, 10, 0));
        incTratada.setMotivoTratamento("Plantão autorizado");

        Inconsistencia incOutroTime = new Inconsistencia();
        incOutroTime.setId(UUID.randomUUID());
        incOutroTime.setEmpresa(empresa);
        incOutroTime.setFuncionario(colabB1);
        incOutroTime.setTipo(TipoInconsistencia.DIA_IMPAR);
        incOutroTime.setSituacao(StatusInconsistencia.ABERTA);
        incOutroTime.setDataReferencia(LocalDate.of(2026, 11, 12));

        when(inconsistenciaRepository.findByEmpresaIdOrderByDataHoraOrigemDesc(empresa.getId()))
                .thenReturn(List.of(incAberta, incTratada, incOutroTime));

        RelatorioDTO.PendenciasRelatorioResponse resp = relatorioService.gerarRelatorioPendencias(
                null, null, "2026-11", null, null
        );

        assertEquals(1, resp.totalAbertas());
        assertEquals(1, resp.totalTratadas());
        assertEquals(StatusInconsistencia.ABERTA, resp.abertas().get(0).situacao());
        assertEquals(StatusInconsistencia.RESOLVIDA, resp.tratadas().get(0).situacao());
        assertEquals(AcaoTratamento.ABONAR_DIA, resp.tratadas().get(0).acaoTomada());

        String csv = relatorioService.exportarPendenciasCsv(null, null, "2026-11", null, null);
        assertTrue(csv.contains("Situacao;Tipo;DataReferencia;Colaborador"));
        assertTrue(csv.contains("ABERTA;DIA_IMPAR;12/11/2026;Alice Equipe A"));
        assertTrue(csv.contains("RESOLVIDA;FORA_DA_JORNADA;15/11/2026;Alice Equipe A"));
        assertFalse(csv.contains("Carlos Equipe B"));
    }
}
