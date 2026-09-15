package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.dto.EquipamentoDTO;
import com.example.projetoPBD.pontoCerto.repository.EquipamentoRepository;
import com.example.projetoPBD.pontoCerto.repository.LocalDeTrabalhoRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoExistenteException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipamentoServiceTest {

    @Mock
    private EquipamentoRepository equipamentoRepository;

    @Mock
    private LocalDeTrabalhoRepository localDeTrabalhoRepository;

    @InjectMocks
    private EquipamentoService equipamentoService;

    private Empresa empresa;
    private LocalDeTrabalho localTrabalho;
    private UUID empresaId;
    private UUID localId;

    @BeforeEach
    void setUp() {
        empresaId = UUID.randomUUID();
        localId = UUID.randomUUID();

        empresa = new Empresa();
        empresa.setId(empresaId);
        empresa.setRazaoSocial("Empresa Teste S.A.");
        empresa.setCnpj("33.000.167/0001-01");

        localTrabalho = new LocalDeTrabalho();
        localTrabalho.setId(localId);
        localTrabalho.setNome("Matriz");
        localTrabalho.setEmpresa(empresa);
        localTrabalho.setEndereco("Rua 1, 100");
        localTrabalho.setMunicipio("São Paulo");
        localTrabalho.setUf("SP");
    }

    @Test
    @DisplayName("Deve cadastrar um relógio de ponto com sucesso quando numFabricacao for informado")
    void deveCadastrarRelogioComSucesso() {
        EquipamentoDTO.Request request = new EquipamentoDTO.Request(
                TipoEquipamento.RELOGIO,
                "Catraca Entrada",
                localId,
                "REP-123456"
        );

        when(localDeTrabalhoRepository.findById(localId)).thenReturn(Optional.of(localTrabalho));
        when(equipamentoRepository.existsByEmpresaIdAndNumFabricacao(empresaId, "REP-123456")).thenReturn(false);
        when(equipamentoRepository.save(any(Equipamento.class))).thenAnswer(inv -> {
            Equipamento eq = inv.getArgument(0);
            eq.setId(UUID.randomUUID());
            return eq;
        });

        EquipamentoDTO.Response response = equipamentoService.cadastrar(request);

        assertNotNull(response);
        assertEquals(TipoEquipamento.RELOGIO, response.tipo());
        assertEquals("Catraca Entrada", response.identificacao());
        assertEquals("REP-123456", response.numFabricacao());
        assertEquals(StatusEquipamento.ATIVO, response.status());
        assertEquals(localId, response.localTrabalhoId());
        assertEquals(empresaId, response.empresaId());
        verify(equipamentoRepository, times(1)).save(any(Equipamento.class));
    }

    @Test
    @DisplayName("Deve falhar ao cadastrar relógio sem número de fabricação")
    void deveFalharRelogioSemNumFabricacao() {
        EquipamentoDTO.Request request = new EquipamentoDTO.Request(
                TipoEquipamento.RELOGIO,
                "Catraca Entrada",
                localId,
                null
        );

        when(localDeTrabalhoRepository.findById(localId)).thenReturn(Optional.of(localTrabalho));

        assertThrows(EquipamentoInvalidoException.class, () -> equipamentoService.cadastrar(request));
        verify(equipamentoRepository, never()).save(any(Equipamento.class));
    }

    @Test
    @DisplayName("Deve falhar ao cadastrar relógio com número de fabricação já existente na mesma empresa (Critério C4)")
    void deveFalharRelogioDuplicadoNaEmpresa() {
        EquipamentoDTO.Request request = new EquipamentoDTO.Request(
                TipoEquipamento.RELOGIO,
                "Catraca 2",
                localId,
                "REP-DUPLICADO"
        );

        when(localDeTrabalhoRepository.findById(localId)).thenReturn(Optional.of(localTrabalho));
        when(equipamentoRepository.existsByEmpresaIdAndNumFabricacao(empresaId, "REP-DUPLICADO")).thenReturn(true);

        assertThrows(EquipamentoExistenteException.class, () -> equipamentoService.cadastrar(request));
        verify(equipamentoRepository, never()).save(any(Equipamento.class));
    }

    @Test
    @DisplayName("Deve cadastrar uma estação web com sucesso mesmo sem numFabricacao")
    void deveCadastrarEstacaoSemNumFabricacao() {
        EquipamentoDTO.Request request = new EquipamentoDTO.Request(
                TipoEquipamento.ESTACAO,
                "Estação Portaria Web",
                localId,
                null
        );

        when(localDeTrabalhoRepository.findById(localId)).thenReturn(Optional.of(localTrabalho));
        when(equipamentoRepository.save(any(Equipamento.class))).thenAnswer(inv -> {
            Equipamento eq = inv.getArgument(0);
            eq.setId(UUID.randomUUID());
            return eq;
        });

        EquipamentoDTO.Response response = equipamentoService.cadastrar(request);

        assertNotNull(response);
        assertEquals(TipoEquipamento.ESTACAO, response.tipo());
        assertEquals("Estação Portaria Web", response.identificacao());
        assertNull(response.numFabricacao());
        assertEquals(StatusEquipamento.ATIVO, response.status());
    }

    @Test
    @DisplayName("Deve alterar o status para INATIVO sem excluir o equipamento (Critério C5)")
    void deveDesativarEquipamento() {
        UUID eqId = UUID.randomUUID();
        Equipamento eq = new Equipamento();
        eq.setId(eqId);
        eq.setStatus(StatusEquipamento.ATIVO);
        eq.setLocalTrabalho(localTrabalho);
        eq.setEmpresa(empresa);
        eq.setTipo(TipoEquipamento.RELOGIO);
        eq.setIdentificacao("Relógio Antigo");

        when(equipamentoRepository.findById(eqId)).thenReturn(Optional.of(eq));
        when(equipamentoRepository.save(any(Equipamento.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipamentoDTO.Response response = equipamentoService.alterarStatus(eqId, StatusEquipamento.INATIVO);

        assertNotNull(response);
        assertEquals(StatusEquipamento.INATIVO, response.status());
        verify(equipamentoRepository, never()).delete(any());
    }
}
