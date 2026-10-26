package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.domain.Batida;
import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusBatida;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegistro;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BatidaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ImportacaoDTO;
import com.example.projetoPBD.pontoCerto.repository.BatidaRepository;
import com.example.projetoPBD.pontoCerto.repository.EquipamentoRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.HistoricoImportacaoRepository;
import com.example.projetoPBD.pontoCerto.service.BatidaService;
import com.example.projetoPBD.pontoCerto.service.ImportacaoService;
import com.example.projetoPBD.pontoCerto.service.ProcessadorArquivoPosicional;
import com.example.projetoPBD.pontoCerto.service.exceptions.ArquivoImportacaoInvalidoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoNaoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PontoCertoApplicationTests {

	@Autowired
	private BatidaService batidaService;

	@Autowired
	private BatidaRepository batidaRepository;

	@Autowired
	private FuncionarioRepository funcionarioRepository;

	@Autowired
	private EquipamentoRepository equipamentoRepository;

	@Autowired
	private HistoricoImportacaoRepository historicoImportacaoRepository;

	@Autowired
	private ProcessadorArquivoPosicional processadorArquivoPosicional;

	@Autowired
	private ImportacaoService importacaoService;

	@Test
	void contextLoads() {
	}

	@Test
	@DisplayName("Teste de Concorrência: Dois registros simultâneos não produzem NSR repetido")
	void testRegistrosSimultaneosNaoDuplicamNsr() throws InterruptedException, ExecutionException {
		// 1. Pega um funcionário existente no banco criado pelo seeder
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum funcionário encontrado para o teste."));

		// 2. Prepara 2 threads e o sinal de largada
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch sinalLargada = new CountDownLatch(1);

		Callable<BatidaDTO.ComprovanteResponse> tarefaBaterPonto = () -> {
			sinalLargada.await(); // Fica aguardando o tiro de largada
			return batidaService.registrarBatida(new BatidaDTO.Request(
					funcionario.getId(),
					TipoRegistro.ENTRADA,
					null,
					null,
					null,
					"127.0.0.1"
			));
		};

		// 3. Dispara as duas requisições para ficarem no ponto
		Future<BatidaDTO.ComprovanteResponse> futuro1 = executor.submit(tarefaBaterPonto);
		Future<BatidaDTO.ComprovanteResponse> futuro2 = executor.submit(tarefaBaterPonto);

		// 4. TIRO DE LARGADA: Libera as duas chamadas no exato mesmo milissegundo!
		sinalLargada.countDown();

		// 5. Espera os dois comprovantes voltarem
		BatidaDTO.ComprovanteResponse comprovante1 = futuro1.get();
		BatidaDTO.ComprovanteResponse comprovante2 = futuro2.get();

		executor.shutdown();

		// 6. Prints no console para verificação visual
		System.out.println("=== COMPROVANTE 1 ===");
		System.out.println("NSR: " + comprovante1.nsr() + " | Hora: " + comprovante1.dataHoraBatida());
		System.out.println("=== COMPROVANTE 2 ===");
		System.out.println("NSR: " + comprovante2.nsr() + " | Hora: " + comprovante2.dataHoraBatida());

		// 7. Validações dos Critérios do Kanban:
		assertNotNull(comprovante1.nsr());
		assertNotNull(comprovante2.nsr());

		// O NSR não pode se repetir:
		assertNotEquals(comprovante1.nsr(), comprovante2.nsr(),
				"FALHA: Os dois registros geraram o mesmo NSR!");

		// Devem ser estritamente sequenciais (ex: 1 e 2, ou 10 e 11):
		long diferenca = Math.abs(comprovante1.nsr() - comprovante2.nsr());
		assertEquals(1, diferenca, "Os números devem ser sequenciais!");
	}

	@Test
	@DisplayName("T09 - Teste de Leitura (Unitário): Recorta tipo, data, hora e PIS nos índices exatos com substring")
	void testLeituraLinhaPosicionalUnitario() {
		// Índices: [0,1)='3', [1,9)='18102026', [9,13)='0830', [13,24)='11111111111'
		String linhaSimulada = "318102026083011111111111";

		ProcessadorArquivoPosicional.RegistroBatidaLinha registro =
				processadorArquivoPosicional.extrairRegistroBatida(2, linhaSimulada);

		assertEquals(2, registro.numeroLinha());
		assertEquals("11111111111", registro.pis());
		assertEquals(LocalDateTime.of(2026, 10, 18, 8, 30), registro.dataHora());
	}

	@Test
	@DisplayName("T09 - Validação de Cabeçalho e Trailer: Rejeita arquivo sem cabeçalho (1) ou sem trailer (9)")
	void testValidacaoCabecalhoETrailer() {
		String semCabecalho = "318102026080011111111111\n9000000001";
		assertThrows(ArquivoImportacaoInvalidoException.class, () ->
				processadorArquivoPosicional.processar(new ByteArrayInputStream(semCabecalho.getBytes(StandardCharsets.UTF_8))),
				"Deve rejeitar arquivo que não inicia com registro tipo 1 (cabeçalho)"
		);

		String semTrailer = "1CABECALHOAFD\n318102026080011111111111";
		assertThrows(ArquivoImportacaoInvalidoException.class, () ->
				processadorArquivoPosicional.processar(new ByteArrayInputStream(semTrailer.getBytes(StandardCharsets.UTF_8))),
				"Deve rejeitar arquivo que não termina com registro tipo 9 (trailer)"
		);
	}

	@Test
	@DisplayName("T09 - Rejeita importação se o equipamento não existir no banco")
	void testRejeitaEquipamentoInexistente() {
		String conteudoValido = "1CABECALHO\n318102026080011111111111\n9TRAILER";
		MockMultipartFile arquivo = new MockMultipartFile(
				"arquivo", "afd.txt", "text/plain", conteudoValido.getBytes(StandardCharsets.UTF_8)
		);

		assertThrows(EquipamentoNaoEncontradoException.class, () ->
				importacaoService.gerarPreview(arquivo, UUID.randomUUID())
		);
	}

	@Test
	@DisplayName("T09 - Teste Transacional (Integração): 10 linhas válidas + 1 linha com erro faz Rollback completo (count == 0)")
	void testRollbackTransacionalImportacao() {
		batidaRepository.deleteAll();
		historicoImportacaoRepository.deleteAll();
		assertEquals(0, batidaRepository.count());

		Equipamento equipamento = equipamentoRepository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum equipamento encontrado para o teste."));

		// Monta arquivo com cabeçalho (1), 10 linhas válidas (3), 11ª linha corrompida que força falha no meio do loop, e trailer (9)
		StringBuilder sb = new StringBuilder();
		sb.append("1000000000CABECALHOAFD\n");
		for (int i = 1; i <= 10; i++) {
			String dia = String.format("%02d", i);
			sb.append("3").append(dia).append("102026080011111111111\n");
		}
		// 11ª linha de batida com data inválida ("99999999") forçando falha após as 10 anteriores terem sido enviadas ao banco
		sb.append("399999999080011111111111\n");
		sb.append("9000000011TRAILER\n");

		MockMultipartFile arquivoComErro = new MockMultipartFile(
				"arquivo",
				"lote_erro_transacional.txt",
				"text/plain",
				sb.toString().getBytes(StandardCharsets.UTF_8)
		);

		// A confirmação deve lançar exceção na 11ª linha e o @Transactional deve desfazer as 10 linhas válidas!
		assertThrows(Exception.class, () ->
				importacaoService.confirmarImportacao(arquivoComErro, equipamento.getId())
		);

		assertEquals(0, batidaRepository.count(), "Rollback falhou: nenhuma das 10 linhas válidas deveria permanecer salva!");
		assertEquals(0, historicoImportacaoRepository.count(), "Rollback falhou: o histórico da transação falha também deve sofrer rollback!");
	}

	@Test
	@DisplayName("T09 - Fluxo Completo Preview + Confirmação: Identifica válidas, repetidas e recusadas e persiste histórico")
	void testFluxoPreviewEConfirmacao() {
		batidaRepository.deleteAll();
		historicoImportacaoRepository.deleteAll();

		Equipamento equipamento = equipamentoRepository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum equipamento encontrado para o teste."));

		// Linha 1: Cabeçalho
		// Linha 2: Válida (PIS 11111111111 - João Colaborador, 15/10/2026 08:00)
		// Linha 3: Repetida no próprio arquivo (PIS 11111111111, 15/10/2026 08:00)
		// Linha 4: Recusada (PIS 99999999999 inexistente)
		// Linha 5: Trailer
		String conteudo = String.join("\n",
				"1000000000CABECALHO",
				"315102026080011111111111",
				"315102026080011111111111",
				"315102026120099999999999",
				"9000000003TRAILER"
		);

		MockMultipartFile arquivo = new MockMultipartFile(
				"arquivo", "afd_teste.txt", "text/plain", conteudo.getBytes(StandardCharsets.UTF_8)
		);

		// Etapa 1: Preview (não deve salvar nada no banco)
		ImportacaoDTO.PreviewResponse preview = importacaoService.gerarPreview(arquivo, equipamento.getId());
		assertEquals(3, preview.totalLidas());
		assertEquals(1, preview.totalValidas());
		assertEquals(1, preview.totalRepetidas());
		assertEquals(1, preview.totalRecusadas());
		assertEquals(0, batidaRepository.count(), "Preview não pode persistir batidas no banco!");

		// Etapa 2: Confirmar Gravação
		ImportacaoDTO.HistoricoResponse historico = importacaoService.confirmarImportacao(arquivo, equipamento.getId());
		assertNotNull(historico.id());
		assertEquals(1, historico.totalValidas());
		assertEquals(1, historico.totalRepetidas());
		assertEquals(1, historico.totalRecusadas());
		assertEquals(2, batidaRepository.count(), "A batida válida e a batida órfã devem ser persistidas sem descartar!");
		assertEquals(1, historicoImportacaoRepository.count(), "O registro de histórico deve ser salvo!");

		Batida batidaOrfa = batidaRepository.findByEmpresaIdAndStatusBatidaOrderByDataHoraDesc(
				equipamento.getEmpresa().getId(), StatusBatida.ORFA_PENDENTE
		).stream().findFirst().orElseThrow();

		assertNull(batidaOrfa.getFuncionario());
		assertEquals("99999999999", batidaOrfa.getIdentificadorOriginal());
		assertEquals(StatusBatida.ORFA_PENDENTE, batidaOrfa.getStatusBatida());

		Funcionario colaborador = funcionarioRepository.findByEmpresaId(equipamento.getEmpresa().getId())
				.stream().findFirst().orElseThrow();

		BatidaDTO.BatidaOrfaResponse vinculada = batidaService.vincularOrfa(
				batidaOrfa.getId(),
				new BatidaDTO.VincularOrfaRequest(colaborador.getId(), "Colaborador recém-admitido")
		);

		assertEquals(StatusBatida.VINCULADA, vinculada.statusBatida());
		assertEquals(colaborador.getNomeCompleto(), vinculada.nomeFuncionarioVinculado());
		assertEquals("Colaborador recém-admitido", vinculada.motivoVinculo());
		assertNotNull(vinculada.dataVinculo());
		assertNotNull(vinculada.vinculadoPor());
	}
}