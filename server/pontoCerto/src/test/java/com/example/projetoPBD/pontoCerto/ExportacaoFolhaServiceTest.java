package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusApuracaoDiaria;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ApuracaoDiariaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BancoHorasDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ExportacaoFolhaDTO;
import com.example.projetoPBD.pontoCerto.repository.ApuracaoDiariaRepository;
import com.example.projetoPBD.pontoCerto.repository.FechamentoPeriodoRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.HistoricoExportacaoFolhaRepository;
import com.example.projetoPBD.pontoCerto.service.ApuracaoDiariaService;
import com.example.projetoPBD.pontoCerto.service.BancoHorasService;
import com.example.projetoPBD.pontoCerto.service.ExportacaoFolhaService;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.PeriodoNaoFechadoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportacaoFolhaServiceTest {

    @Mock
    private FechamentoPeriodoRepository fechamentoPeriodoRepository;
    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private ApuracaoDiariaRepository apuracaoDiariaRepository;
    @Mock
    private ApuracaoDiariaService apuracaoDiariaService;
    @Mock
    private BancoHorasService bancoHorasService;
    @Mock
    private HistoricoExportacaoFolhaRepository historicoRepository;

    private ExportacaoFolhaService exportacaoFolhaService;

    private Empresa empresa;
    private Funcionario rh;
    private Funcionario gestor;
    private Funcionario colab1;
    private Funcionario colab2;

    @BeforeEach
    void setUp() {
        exportacaoFolhaService = new ExportacaoFolhaService(
                fechamentoPeriodoRepository,
                funcionarioRepository,
                apuracaoDiariaRepository,
                apuracaoDiariaService,
                bancoHorasService,
                historicoRepository
        );

        empresa = new Empresa();
        empresa.setId(UUID.randomUUID());
        empresa.setRazaoSocial("Empresa Teste T20");

        rh = criarFunc("RH Responsável", "RH-01", "000.000.000-01", PerfilAcesso.RH);
        gestor = criarFunc("Gestor", "GES-01", "000.000.000-02", PerfilAcesso.GESTOR);
        colab1 = criarFunc("Ana Paula", "COL-001", "111.222.333-44", PerfilAcesso.COLABORADOR);
        colab2 = criarFunc("Bruno Costa", "COL-002", "555.666.777-88", PerfilAcesso.COLABORADOR);

        lenient().when(funcionarioRepository.findById(rh.getId())).thenReturn(Optional.of(rh));
        lenient().when(funcionarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Funcionario criarFunc(String nome, String matricula, String cpf, PerfilAcesso perfil) {
        Funcionario f = new Funcionario();
        f.setId(UUID.randomUUID());
        f.setEmpresa(empresa);
        f.setNomeCompleto(nome);
        f.setMatricula(matricula);
        f.setCpf(cpf);
        f.setCargo("Analista");
        f.setPerfilAcesso(perfil);
        f.setUsuario(matricula.toLowerCase());
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
    @DisplayName("T20 Unit 1 - Bloqueia exportação quando período não existe ou está ABERTO")
    void testRecusaPeriodoAberto() {
        autenticar(rh);

        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(
                empresa.getId(), "2026-10")).thenReturn(Optional.empty());

        PeriodoNaoFechadoException exSemFech = assertThrows(PeriodoNaoFechadoException.class, () ->
                exportacaoFolhaService.exportarFolha("2026-10")
        );
        assertTrue(exSemFech.getMessage().contains("ainda não possui fechamento registrado"));

        FechamentoPeriodo aberto = new FechamentoPeriodo();
        aberto.setStatus(StatusFechamentoPeriodo.ABERTO);
        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(
                empresa.getId(), "2026-10")).thenReturn(Optional.of(aberto));

        PeriodoNaoFechadoException exAberto = assertThrows(PeriodoNaoFechadoException.class, () ->
                exportacaoFolhaService.exportarFolha("2026-10")
        );
        assertTrue(exAberto.getMessage().contains("encontra-se com status ABERTO"));
    }

    @Test
    @DisplayName("T20 Unit 2 - Bloqueia acesso para perfil não-RH (Gestor/Colaborador)")
    void testBloqueiaPerfilNaoRh() {
        autenticar(gestor);
        assertThrows(AcessoNegadoException.class, () -> exportacaoFolhaService.exportarFolha("2026-10"));
    }

    @Test
    @DisplayName("T20 Unit 3 - Exportação determinística (reexportação idêntica), auditoria e consolidação de faixas")
    void testExportacaoDeterministicaEAuditoria() {
        autenticar(rh);

        FechamentoPeriodo fechado = new FechamentoPeriodo();
        fechado.setEmpresa(empresa);
        fechado.setMesReferencia("2026-10");
        fechado.setStatus(StatusFechamentoPeriodo.CONCLUIDO);
        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(
                empresa.getId(), "2026-10")).thenReturn(Optional.of(fechado));

        when(funcionarioRepository.findByEmpresaIdOrderByNomeCompletoAsc(empresa.getId()))
                .thenReturn(List.of(colab2, colab1)); // desordenado de propósito

        ApuracaoDiaria ap1 = new ApuracaoDiaria();
        ap1.setId(UUID.randomUUID());
        ap1.setFuncionario(colab1);
        ap1.setDataReferencia(LocalDate.of(2026, 10, 10));
        ap1.setMinutosTrabalhados(660); // 480 normais + 180 extras
        ap1.setMinutosHoraExtraTotal(180);
        ap1.setMinutosAdicionalNoturno(30);
        ap1.setMinutosFalta(0);
        ap1.setMinutosAtraso(10);
        ap1.setMinutosSaidaAntecipada(5);

        when(apuracaoDiariaRepository.findByEmpresaIdAndDataReferenciaBetweenOrderByDataReferenciaAsc(
                eq(empresa.getId()), any(), any())).thenReturn(List.of(ap1));

        when(apuracaoDiariaService.toDTO(ap1)).thenReturn(new ApuracaoDiariaDTO.Response(
                ap1.getId(), colab1.getId(), colab1.getNomeCompleto(), colab1.getMatricula(),
                ap1.getDataReferencia(), StatusApuracaoDiaria.APURADO,
                480, 660, 60, 10, 5, 30, 0, 180, 0, null, null,
                List.of(), Map.of("50%", 120, "100%", 60), LocalDateTime.now()
        ));

        when(bancoHorasService.consultarExtrato(eq(colab1.getId()), any(), any()))
                .thenReturn(new BancoHorasDTO.ExtratoPeriodoResponse(
                        colab1.getId(), colab1.getNomeCompleto(), colab1.getMatricula(),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                        0, 180, -15, 0, 165, List.of()
                ));
        when(bancoHorasService.consultarExtrato(eq(colab2.getId()), any(), any()))
                .thenReturn(new BancoHorasDTO.ExtratoPeriodoResponse(
                        colab2.getId(), colab2.getNomeCompleto(), colab2.getMatricula(),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                        0, 0, 0, 0, 0, List.of()
                ));

        ExportacaoFolhaDTO.ResumoExportacaoResponse primeira = exportacaoFolhaService.exportarFolha("2026-10");
        ExportacaoFolhaDTO.ResumoExportacaoResponse segunda = exportacaoFolhaService.exportarFolha("2026-10");

        String csv1 = exportacaoFolhaService.montarConteudoCsv("2026-10", primeira.linhas());
        String csv2 = exportacaoFolhaService.montarConteudoCsv("2026-10", segunda.linhas());

        // Reexportação idêntica byte a byte e mesmo SHA-256
        assertEquals(csv1, csv2);
        assertEquals(primeira.hashSha256(), segunda.hashSha256());

        // Ordenado por matrícula (COL-001 antes de COL-002)
        assertEquals("COL-001", primeira.linhas().get(0).matricula());
        assertEquals("COL-002", primeira.linhas().get(1).matricula());

        // Valores exatos para COL-001
        ExportacaoFolhaDTO.LinhaColaboradorFolha linhaAna = primeira.linhas().get(0);
        assertEquals(480, linhaAna.horasNormaisMinutos());
        assertEquals(120, linhaAna.horasExtras50Minutos());
        assertEquals(60, linhaAna.horasExtras100Minutos());
        assertEquals(30, linhaAna.adicionalNoturnoMinutos());
        assertEquals(15, linhaAna.atrasosMinutos());
        assertEquals(165, linhaAna.saldoBancoHorasMinutos());

        // Gravou 2 registros de auditoria no histórico
        ArgumentCaptor<HistoricoExportacaoFolha> captor = ArgumentCaptor.forClass(HistoricoExportacaoFolha.class);
        verify(historicoRepository, times(2)).saveAndFlush(captor.capture());
        assertEquals("2026-10", captor.getValue().getCompetencia());
        assertEquals(2, captor.getValue().getQuantidadeColaboradores());
    }
}
