package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BatidaDTO;
import com.example.projetoPBD.pontoCerto.repository.*;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.*;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.CredenciaisInvalidasException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstacaoPontoServiceTest {

    @Mock
    private BatidaRepository batidaRepository;
    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private EquipamentoRepository equipamentoRepository;
    @Mock
    private EstacaoRepository estacaoRepository;
    @Mock
    private HistoricoImportacaoRepository historicoImportacaoRepository;
    @Mock
    private InconsistenciaRepository inconsistenciaRepository;
    @Mock
    private InconsistenciaService inconsistenciaService;
    @Mock
    private ApuracaoDiariaService apuracaoDiariaService;
    @Mock
    private CompetenciaLockService competenciaLockService;
    @Mock
    private SecurityTenantContext securityTenantContext;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AnexoStorageService anexoStorageService;

    private BatidaService batidaService;

    private Empresa empresa;
    private Estacao estacao;
    private Funcionario colaboradorAtivo;
    private Funcionario colaboradorDesligado;
    private MockMultipartFile fotoValida;

    @BeforeEach
    void setUp() {
        batidaService = new BatidaService(
                batidaRepository,
                funcionarioRepository,
                equipamentoRepository,
                estacaoRepository,
                historicoImportacaoRepository,
                inconsistenciaRepository,
                inconsistenciaService,
                apuracaoDiariaService,
                competenciaLockService,
                securityTenantContext,
                passwordEncoder,
                anexoStorageService
        );

        empresa = new Empresa();
        empresa.setId(UUID.randomUUID());
        empresa.setRazaoSocial("Empresa T07");
        empresa.setCnpj("12.345.678/0001-99");

        estacao = new Estacao();
        estacao.setId(UUID.randomUUID());
        estacao.setEmpresa(empresa);
        estacao.setIdentificacao("Estação Corredor Térreo");
        estacao.setCodigo("EST-01");
        estacao.setStatus(StatusEquipamento.ATIVO);

        colaboradorAtivo = new Funcionario();
        colaboradorAtivo.setId(UUID.randomUUID());
        colaboradorAtivo.setEmpresa(empresa);
        colaboradorAtivo.setNomeCompleto("João da Estação");
        colaboradorAtivo.setMatricula("COL-07");
        colaboradorAtivo.setUsuario("joao.estacao");
        colaboradorAtivo.setSenhaHash("hash_seguro");
        colaboradorAtivo.setPerfilAcesso(PerfilAcesso.COLABORADOR);
        colaboradorAtivo.setDataDesligamento(null);

        colaboradorDesligado = new Funcionario();
        colaboradorDesligado.setId(UUID.randomUUID());
        colaboradorDesligado.setEmpresa(empresa);
        colaboradorDesligado.setNomeCompleto("Ex Colaborador");
        colaboradorDesligado.setUsuario("ex.colab");
        colaboradorDesligado.setSenhaHash("hash_seguro");
        colaboradorDesligado.setPerfilAcesso(PerfilAcesso.COLABORADOR);
        colaboradorDesligado.setDataDesligamento(LocalDate.now().minusDays(1));

        fotoValida = new MockMultipartFile("foto", "rosto.jpg", "image/jpeg", "fake-image-bytes".getBytes());
    }

    @Test
    @DisplayName("T07 Unit 1 - Registra ponto na estação autenticando colaborador, salvando foto, hora do servidor e vínculo com estação")
    void testRegistrarPontoEstacaoSucesso() {
        when(estacaoRepository.findById(estacao.getId())).thenReturn(Optional.of(estacao));
        when(funcionarioRepository.findByUsuarioAndEmpresaId("joao.estacao", empresa.getId()))
                .thenReturn(Optional.of(colaboradorAtivo));
        when(passwordEncoder.matches("123456", "hash_seguro")).thenReturn(true);
        when(anexoStorageService.salvarFotoBatida(fotoValida)).thenReturn("/uploads/batidas/foto-uuid.jpg");
        when(batidaRepository.findAtivasByFuncionarioIdAndDataHoraBetweenOrderByDataHoraAsc(
                eq(colaboradorAtivo.getId()), any(), any())).thenReturn(List.of());
        when(batidaRepository.proximoNsrSequence()).thenReturn(9001L);
        when(batidaRepository.saveAndFlush(any(Batida.class))).thenAnswer(inv -> {
            Batida b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        LocalDateTime antes = LocalDateTime.now().minusSeconds(2);
        BatidaDTO.ComprovanteEstacaoResponse resp = batidaService.registrarPontoEstacao(
                estacao.getId(), "joao.estacao", "123456", fotoValida, "192.168.0.10"
        );
        LocalDateTime depois = LocalDateTime.now().plusSeconds(2);

        assertNotNull(resp);
        assertEquals(9001L, resp.nsr());
        assertEquals(estacao.getId(), resp.estacaoId());
        assertEquals("Estação Corredor Térreo", resp.estacaoIdentificacao());
        assertEquals(TipoOrigem.ESTACAO, resp.tipoOrigem());
        assertTrue(resp.fotoCapturada());
        assertFalse(resp.dataHoraServidor().isBefore(antes));
        assertFalse(resp.dataHoraServidor().isAfter(depois));

        ArgumentCaptor<Batida> captor = ArgumentCaptor.forClass(Batida.class);
        verify(batidaRepository).saveAndFlush(captor.capture());
        assertEquals("/uploads/batidas/foto-uuid.jpg", captor.getValue().getFotoUrl());
        assertEquals(estacao, captor.getValue().getEquipamento());
        assertEquals(TipoOrigem.ESTACAO, captor.getValue().getTipoOrigem());
    }

    @Test
    @DisplayName("T07 Unit 2 - Recusa registro quando senha do colaborador está incorreta")
    void testRecusaSenhaInvalida() {
        when(estacaoRepository.findById(estacao.getId())).thenReturn(Optional.of(estacao));
        when(funcionarioRepository.findByUsuarioAndEmpresaId("joao.estacao", empresa.getId()))
                .thenReturn(Optional.of(colaboradorAtivo));
        when(passwordEncoder.matches("senha_errada", "hash_seguro")).thenReturn(false);

        assertThrows(CredenciaisInvalidasException.class, () ->
                batidaService.registrarPontoEstacao(estacao.getId(), "joao.estacao", "senha_errada", fotoValida, "127.0.0.1")
        );
        verify(batidaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("T07 Unit 3 - Recusa colaborador desligado ou inativo com mensagem clara")
    void testRecusaColaboradorDesligado() {
        when(estacaoRepository.findById(estacao.getId())).thenReturn(Optional.of(estacao));
        when(funcionarioRepository.findByUsuarioAndEmpresaId("ex.colab", empresa.getId()))
                .thenReturn(Optional.of(colaboradorDesligado));
        when(passwordEncoder.matches("123456", "hash_seguro")).thenReturn(true);

        AcessoNegadoException ex = assertThrows(AcessoNegadoException.class, () ->
                batidaService.registrarPontoEstacao(estacao.getId(), "ex.colab", "123456", fotoValida, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("desligado ou inativo"));
        verify(batidaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("T07 Unit 4 - Recusa registro sem foto capturada ou estação inativa")
    void testRecusaSemFotoOuEstacaoInativa() {
        MockMultipartFile fotoVazia = new MockMultipartFile("foto", new byte[0]);
        assertThrows(IllegalArgumentException.class, () ->
                batidaService.registrarPontoEstacao(estacao.getId(), "joao.estacao", "123456", fotoVazia, "127.0.0.1")
        );

        estacao.setStatus(StatusEquipamento.INATIVO);
        when(estacaoRepository.findById(estacao.getId())).thenReturn(Optional.of(estacao));
        assertThrows(EquipamentoInvalidoException.class, () ->
                batidaService.registrarPontoEstacao(estacao.getId(), "joao.estacao", "123456", fotoValida, "127.0.0.1")
        );
    }
}
