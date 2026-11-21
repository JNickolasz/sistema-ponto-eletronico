package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.*;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.*;
import com.example.projetoPBD.pontoCerto.repository.*;
import com.example.projetoPBD.pontoCerto.resource.BancoHorasController;
import com.example.projetoPBD.pontoCerto.security.RateLimiterService;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.*;
import com.example.projetoPBD.pontoCerto.service.exceptions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAuditRound2Test {

    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private LocalDeTrabalhoRepository localDeTrabalhoRepository;
    @Mock private BatidaRepository batidaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SecurityTenantContext securityTenantContext;
    @Mock private OcorrenciaColaboradorRepository ocorrenciaRepository;
    @Mock private TipoOcorrenciaRepository tipoOcorrenciaRepository;
    @Mock private FechamentoPeriodoRepository fechamentoPeriodoRepository;
    @Mock private ApuracaoDiariaService apuracaoDiariaService;
    @Mock private AnexoStorageService anexoStorageService;
    @Mock private FeriadoRepository feriadoRepository;
    @Mock private PontoFacultativoRepository pontoFacultativoRepository;
    @Mock private EquipamentoRepository equipamentoRepository;
    @Mock private RelogioService relogioService;
    @Mock private EstacaoService estacaoService;
    @Mock private BancoHorasService bancoHorasService;
    @Mock private EstacaoRepository estacaoRepository;
    @Mock private HistoricoImportacaoRepository historicoImportacaoRepository;
    @Mock private InconsistenciaRepository inconsistenciaRepository;
    @Mock private InconsistenciaService inconsistenciaService;
    @Mock private CompetenciaLockService competenciaLockService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Empresa criarEmpresa(UUID id, String razao) {
        Empresa e = new Empresa();
        e.setId(id);
        e.setRazaoSocial(razao);
        return e;
    }

    private Funcionario criarFuncionario(UUID id, Empresa empresa, PerfilAcesso perfil, Funcionario gestor) {
        Funcionario f = new Funcionario();
        f.setId(id);
        f.setEmpresa(empresa);
        f.setPerfilAcesso(perfil);
        f.setGestor(gestor);
        f.setNomeCompleto("Usuario " + perfil);
        f.setUsuario("user_" + id.toString().substring(0, 6));
        f.setSenhaHash("hash");
        return f;
    }

    private void autenticar(Funcionario f) {
        FuncionarioUserDetails ud = new FuncionarioUserDetails(f);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities())
        );
    }

    // ==================== R2-01 ====================

    @Test
    void r201_desligarDeveBloquearFuncionarioDeOutraEmpresa() {
        UUID empresaA = UUID.randomUUID();
        UUID funcEmpresaB = UUID.randomUUID();
        when(securityTenantContext.empresaAtual()).thenReturn(empresaA);
        when(funcionarioRepository.findByIdAndEmpresaId(funcEmpresaB, empresaA)).thenReturn(Optional.empty());

        FuncionarioService service = new FuncionarioService(
                funcionarioRepository, empresaRepository, localDeTrabalhoRepository,
                batidaRepository, passwordEncoder, securityTenantContext
        );

        assertThrows(UsuarioNaoEncontradoNaEmpresa.class,
                () -> service.desligar(funcEmpresaB, new FuncionarioDTO.Desligamento(LocalDate.now())));
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    void r201_excluirDeveBloquearQuandoColaboradorPossuiMarcacoes() {
        UUID empresaA = UUID.randomUUID();
        UUID funcId = UUID.randomUUID();
        Empresa emp = criarEmpresa(empresaA, "Empresa A");
        Funcionario func = criarFuncionario(funcId, emp, PerfilAcesso.COLABORADOR, null);

        when(securityTenantContext.empresaAtual()).thenReturn(empresaA);
        when(funcionarioRepository.findByIdAndEmpresaId(funcId, empresaA)).thenReturn(Optional.of(func));
        when(batidaRepository.existsByFuncionarioId(funcId)).thenReturn(true);

        FuncionarioService service = new FuncionarioService(
                funcionarioRepository, empresaRepository, localDeTrabalhoRepository,
                batidaRepository, passwordEncoder, securityTenantContext
        );

        assertThrows(ColaboradorComMarcacaoException.class, () -> service.excluir(funcId));
        verify(funcionarioRepository, never()).delete(any());
    }

    // ==================== R2-02 ====================

    @Test
    void r202_colaboradorEGestorNaoPodemBaixarAtestadoDeOutroColaborador() {
        UUID empresaId = UUID.randomUUID();
        Empresa emp = criarEmpresa(empresaId, "Empresa A");
        Funcionario gestor = criarFuncionario(UUID.randomUUID(), emp, PerfilAcesso.GESTOR, null);
        Funcionario colab1 = criarFuncionario(UUID.randomUUID(), emp, PerfilAcesso.COLABORADOR, gestor);
        Funcionario colab2 = criarFuncionario(UUID.randomUUID(), emp, PerfilAcesso.COLABORADOR, gestor);

        UUID ocorrenciaId = UUID.randomUUID();
        OcorrenciaColaborador oc = new OcorrenciaColaborador();
        oc.setId(ocorrenciaId);
        oc.setFuncionario(colab1);
        oc.setCaminhoAnexo("/tmp/atestado.pdf");

        when(securityTenantContext.empresaAtual()).thenReturn(empresaId);
        when(ocorrenciaRepository.findById(ocorrenciaId)).thenReturn(Optional.of(oc));

        OcorrenciaColaboradorService service = new OcorrenciaColaboradorService(
                ocorrenciaRepository, tipoOcorrenciaRepository, funcionarioRepository,
                fechamentoPeriodoRepository, apuracaoDiariaService, anexoStorageService, securityTenantContext
        );

        // Outro colaborador -> bloqueado
        autenticar(colab2);
        assertThrows(AcessoNegadoException.class, () -> service.buscarPorId(ocorrenciaId));

        // Gestor (mesmo sendo gestor direto) -> bloqueado para anexo médico (LGPD)
        autenticar(gestor);
        assertThrows(AcessoNegadoException.class, () -> service.buscarPorId(ocorrenciaId));

        // Próprio colaborador -> permitido
        autenticar(colab1);
        assertEquals(ocorrenciaId, service.buscarPorId(ocorrenciaId).getId());
    }

    @Test
    void r202_anexoStorageDeveRejeitarHtmlESvg() {
        AnexoStorageService storage = new AnexoStorageService();
        MockMultipartFile html = new MockMultipartFile("arquivo", "atestado.html", "text/html", "<p>x</p>".getBytes());
        MockMultipartFile svg = new MockMultipartFile("arquivo", "img.svg", "image/svg+xml", "<svg/>".getBytes());

        assertThrows(IllegalArgumentException.class, () -> storage.salvarAnexo(html));
        assertThrows(IllegalArgumentException.class, () -> storage.salvarAnexo(svg));
    }

    // ==================== R2-03 ====================

    @Test
    void r203_empresaServiceListarEmpresaAtualRetornaApenasTenantLogado() {
        UUID empresaA = UUID.randomUUID();
        Empresa emp = criarEmpresa(empresaA, "Empresa A");
        when(securityTenantContext.empresaAtual()).thenReturn(empresaA);
        when(empresaRepository.findById(empresaA)).thenReturn(Optional.of(emp));

        EmpresaService service = new EmpresaService(empresaRepository, securityTenantContext);
        List<EmpresaDTO.Response> res = service.listarEmpresaAtual();

        assertEquals(1, res.size());
        assertEquals(empresaA, res.get(0).id());
        verify(empresaRepository, never()).findAll();
    }

    @Test
    void r203_rhNaoPodeCriarFeriadoNacionalOuInativarFeriadoDeTerceiro() {
        UUID empresaA = UUID.randomUUID();
        UUID empresaB = UUID.randomUUID();
        FeriadoService service = new FeriadoService(feriadoRepository, empresaRepository, securityTenantContext);

        FeriadoDTO.Request reqNacional = new FeriadoDTO.Request(
                null, "Natal Fake", LocalDate.of(2026, 12, 25), Alcance.NACIONAL, null, null
        );
        assertThrows(AcessoNegadoException.class, () -> service.cadastrar(reqNacional));

        // Inativar feriado nacional (empresa == null)
        UUID feriadoNacionalId = UUID.randomUUID();
        Feriado feriadoNacional = new Feriado();
        feriadoNacional.setId(feriadoNacionalId);
        feriadoNacional.setEmpresa(null);
        when(securityTenantContext.empresaAtual()).thenReturn(empresaA);
        when(feriadoRepository.findById(feriadoNacionalId)).thenReturn(Optional.of(feriadoNacional));
        assertThrows(AcessoNegadoException.class, () -> service.inativar(feriadoNacionalId));

        // Inativar feriado da empresa B
        UUID feriadoBId = UUID.randomUUID();
        Feriado feriadoB = new Feriado();
        feriadoB.setId(feriadoBId);
        feriadoB.setEmpresa(criarEmpresa(empresaB, "Empresa B"));
        when(feriadoRepository.findById(feriadoBId)).thenReturn(Optional.of(feriadoB));
        assertThrows(AcessoNegadoException.class, () -> service.inativar(feriadoBId));
    }

    @Test
    void r203_rhNaoPodeCriarOuInativarPontoFacultativoGlobal() {
        UUID empresaA = UUID.randomUUID();
        PontoFacultativoService service = new PontoFacultativoService(
                pontoFacultativoRepository, empresaRepository, securityTenantContext
        );

        PontoFacultativoDTO.Request reqGlobal = new PontoFacultativoDTO.Request(
                "Carnaval", LocalDate.of(2026, 2, 17), Alcance.NACIONAL, null, null
        );
        assertThrows(AcessoNegadoException.class, () -> service.cadastrar(reqGlobal));

        UUID pfGlobalId = UUID.randomUUID();
        PontoFacultativo pfGlobal = new PontoFacultativo();
        pfGlobal.setId(pfGlobalId);
        pfGlobal.setEmpresa(null);
        when(securityTenantContext.empresaAtual()).thenReturn(empresaA);
        when(pontoFacultativoRepository.findById(pfGlobalId)).thenReturn(Optional.of(pfGlobal));
        assertThrows(AcessoNegadoException.class, () -> service.inativar(pfGlobalId));
    }

    // ==================== R2-04 ====================

    @Test
    void r204_gestorNaoPodeConsultarBancoHorasForaDaSuaEquipe() {
        Empresa emp = criarEmpresa(UUID.randomUUID(), "Empresa A");
        Funcionario gestor1 = criarFuncionario(UUID.randomUUID(), emp, PerfilAcesso.GESTOR, null);
        Funcionario gestor2 = criarFuncionario(UUID.randomUUID(), emp, PerfilAcesso.GESTOR, null);
        Funcionario colabOutraEquipe = criarFuncionario(UUID.randomUUID(), emp, PerfilAcesso.COLABORADOR, gestor2);

        autenticar(gestor1);
        when(funcionarioRepository.findById(colabOutraEquipe.getId())).thenReturn(Optional.of(colabOutraEquipe));

        BancoHorasController controller = new BancoHorasController(bancoHorasService, funcionarioRepository);
        assertThrows(AcessoNegadoException.class,
                () -> controller.consultarExtrato(colabOutraEquipe.getId(), null, null, "2026-10"));
    }

    // ==================== R2-05 ====================

    @Test
    void r205_ocorrenciaMultiMesDeveBloquearQuandoMesIntermediarioEstaFechado() {
        UUID empresaId = UUID.randomUUID();
        Empresa emp = criarEmpresa(empresaId, "Empresa A");
        Funcionario func = criarFuncionario(UUID.randomUUID(), emp, PerfilAcesso.COLABORADOR, null);

        TipoOcorrencia tipo = new TipoOcorrencia();
        tipo.setId(UUID.randomUUID());
        tipo.setAtivo(true);
        tipo.setTipoAbono(TipoAbonoOcorrencia.INTEGRAL);

        FechamentoPeriodo outFechado = new FechamentoPeriodo();
        outFechado.setStatus(StatusFechamentoPeriodo.CONCLUIDO);

        when(securityTenantContext.empresaAtual()).thenReturn(empresaId);
        when(funcionarioRepository.findById(func.getId())).thenReturn(Optional.of(func));
        when(tipoOcorrenciaRepository.findByIdAndEmpresaId(tipo.getId(), empresaId)).thenReturn(Optional.of(tipo));
        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, "2026-09"))
                .thenReturn(Optional.empty());
        when(fechamentoPeriodoRepository.findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, "2026-10"))
                .thenReturn(Optional.of(outFechado));

        OcorrenciaColaboradorService service = new OcorrenciaColaboradorService(
                ocorrenciaRepository, tipoOcorrenciaRepository, funcionarioRepository,
                fechamentoPeriodoRepository, apuracaoDiariaService, anexoStorageService, securityTenantContext
        );

        OcorrenciaColaboradorDTO.CriarRequest req = new OcorrenciaColaboradorDTO.CriarRequest(
                func.getId(), tipo.getId(),
                LocalDate.of(2026, 9, 28), LocalDate.of(2026, 11, 2),
                null, "Afastamento longo"
        );

        assertThrows(PeriodoFechadoException.class, () -> service.lancarOcorrencia(req, null));
        verify(ocorrenciaRepository, never()).saveAndFlush(any());
    }

    // ==================== R2-06 ====================

    @Test
    void r206_vincularOrfaDeveBloquearQuandoPeriodoEstaFechado() {
        UUID empresaId = UUID.randomUUID();
        Empresa emp = criarEmpresa(empresaId, "Empresa A");
        Funcionario rh = criarFuncionario(UUID.randomUUID(), emp, PerfilAcesso.RH, null);
        Funcionario func = criarFuncionario(UUID.randomUUID(), emp, PerfilAcesso.COLABORADOR, null);
        autenticar(rh);

        Equipamento eq = new Equipamento();
        eq.setEmpresa(emp);

        UUID batidaId = UUID.randomUUID();
        Batida orfa = new Batida();
        orfa.setId(batidaId);
        orfa.setEquipamento(eq);
        orfa.setStatusBatida(StatusBatida.ORFA_PENDENTE);
        orfa.setDataHora(LocalDateTime.of(2026, 10, 15, 8, 0));

        when(securityTenantContext.empresaAtual()).thenReturn(empresaId);
        when(batidaRepository.findById(batidaId)).thenReturn(Optional.of(orfa));
        when(funcionarioRepository.findById(func.getId())).thenReturn(Optional.of(func));
        doThrow(new PeriodoFechadoException("Período fechado"))
                .when(competenciaLockService).validarPeriodoAberto(empresaId, LocalDate.of(2026, 10, 15));

        BatidaService service = new BatidaService(
                batidaRepository, funcionarioRepository, equipamentoRepository, estacaoRepository,
                historicoImportacaoRepository, inconsistenciaRepository, inconsistenciaService,
                apuracaoDiariaService, competenciaLockService, securityTenantContext,
                passwordEncoder, anexoStorageService
        );

        assertThrows(PeriodoFechadoException.class,
                () -> service.vincularOrfa(batidaId, new BatidaDTO.VincularOrfaRequest(func.getId(), "Vinculo teste")));
        verify(batidaRepository, never()).saveAndFlush(any());
    }

    // ==================== R2-07 ====================

    @Test
    void r207_equipamentoServiceDeveBloquearLocalDeTrabalhoDeOutraEmpresa() {
        UUID empresaAId = UUID.randomUUID();
        UUID empresaBId = UUID.randomUUID();
        Empresa empA = criarEmpresa(empresaAId, "Empresa A");
        Empresa empB = criarEmpresa(empresaBId, "Empresa B");

        UUID localBId = UUID.randomUUID();
        LocalDeTrabalho localB = new LocalDeTrabalho();
        localB.setId(localBId);
        localB.setEmpresa(empB);

        when(securityTenantContext.empresaAtual()).thenReturn(empresaAId);
        when(empresaRepository.findById(empresaAId)).thenReturn(Optional.of(empA));
        when(localDeTrabalhoRepository.findById(localBId)).thenReturn(Optional.of(localB));

        EquipamentoService service = new EquipamentoService(
                equipamentoRepository, localDeTrabalhoRepository, empresaRepository,
                securityTenantContext, relogioService, estacaoService
        );

        EquipamentoDTO.Criar dto = new EquipamentoDTO.Criar(
                "Relógio 1", localBId, TipoEquipamento.RELOGIO, "COD-1", null, null
        );

        assertThrows(AcessoNegadoException.class, () -> service.cadastrar(dto));
        verify(equipamentoRepository, never()).save(any());
    }

    // ==================== R2-09 ====================

    @Test
    void r209_rateLimiterDeveBloquearAposLimiteExcedido() {
        RateLimiterService limiter = new RateLimiterService();
        limiter.verificarLimite("ip-test", 2, 60);
        limiter.verificarLimite("ip-test", 2, 60);
        assertThrows(TooManyRequestsException.class, () -> limiter.verificarLimite("ip-test", 2, 60));
    }
}
