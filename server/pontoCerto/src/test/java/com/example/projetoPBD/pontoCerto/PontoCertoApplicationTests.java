package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.domain.Batida;
import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Feriado;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.enums.AcaoTratamento;
import com.example.projetoPBD.pontoCerto.domain.enums.Alcance;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusBatida;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusInconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoInconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegistro;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BatidaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ImportacaoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.InconsistenciaDTO;
import com.example.projetoPBD.pontoCerto.repository.BatidaRepository;
import com.example.projetoPBD.pontoCerto.repository.EquipamentoRepository;
import com.example.projetoPBD.pontoCerto.repository.FechamentoPeriodoRepository;
import com.example.projetoPBD.pontoCerto.repository.FeriadoRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.HistoricoImportacaoRepository;
import com.example.projetoPBD.pontoCerto.repository.InconsistenciaRepository;
import com.example.projetoPBD.pontoCerto.service.BatidaService;
import com.example.projetoPBD.pontoCerto.service.ImportacaoService;
import com.example.projetoPBD.pontoCerto.service.InconsistenciaService;
import com.example.projetoPBD.pontoCerto.service.ProcessadorArquivoPosicional;
import com.example.projetoPBD.pontoCerto.service.exceptions.ArquivoImportacaoInvalidoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoNaoEncontradoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.FechamentoComPendenciasException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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

	@Autowired
	private InconsistenciaService inconsistenciaService;

	@Autowired
	private InconsistenciaRepository inconsistenciaRepository;

	@Autowired
	private FechamentoPeriodoRepository fechamentoPeriodoRepository;

	@Autowired
	private FeriadoRepository feriadoRepository;

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
		assertNotNull(registro.nsr());
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

	@Test
	@DisplayName("T11 - Idempotência na Reimportação: Reimportar o mesmo arquivo conta todas como repetidas e não duplica no banco")
	void testReimportacaoMesmoArquivoNaoDuplica() {
		batidaRepository.deleteAll();
		historicoImportacaoRepository.deleteAll();

		Equipamento equipamento = equipamentoRepository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum equipamento encontrado para o teste."));

		String conteudo = String.join("\n",
				"1000000000CABECALHO",
				"300000010115102026080011111111111",
				"300000010215102026120011111111111",
				"300000010315102026130011111111111",
				"300000010415102026170011111111111",
				"300000010516102026080011111111111",
				"9000000005TRAILER"
		);

		MockMultipartFile arquivo = new MockMultipartFile(
				"arquivo", "afd_lote_5.txt", "text/plain", conteudo.getBytes(StandardCharsets.UTF_8)
		);

		// 1ª Importação: grava as 5 batidas
		ImportacaoDTO.HistoricoResponse primeira = importacaoService.confirmarImportacao(arquivo, equipamento.getId());
		assertEquals(5, primeira.totalValidas());
		assertEquals(0, primeira.totalRepetidas());
		assertEquals(5, batidaRepository.count());

		// Preview da 2ª Importação (mesmo arquivo e mesmo equipamento): 5 repetidas e 0 válidas
		ImportacaoDTO.PreviewResponse previewSegunda = importacaoService.gerarPreview(arquivo, equipamento.getId());
		assertEquals(0, previewSegunda.totalValidas());
		assertEquals(5, previewSegunda.totalRepetidas());

		// Confirmação da 2ª Importação: não duplica nada no banco
		ImportacaoDTO.HistoricoResponse segunda = importacaoService.confirmarImportacao(arquivo, equipamento.getId());
		assertEquals(0, segunda.totalValidas());
		assertEquals(5, segunda.totalRepetidas());
		assertEquals(5, batidaRepository.count(), "Nenhuma batida duplicada deve ser criada ao reimportar o arquivo!");
	}

	@Test
	@DisplayName("T11 - Constraint de Unicidade no Banco: Duas gravações simultâneas com mesmo Equipamento + NSR não geram duplicidade")
	void testConcorrenciaMesmoEquipamentoENsrBloqueiaDuplicidade() throws InterruptedException {
		batidaRepository.deleteAll();

		Equipamento equipamento = equipamentoRepository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum equipamento encontrado para o teste."));
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum funcionário encontrado para o teste."));

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch largada = new CountDownLatch(1);

		Callable<Boolean> inserirMesmoNsr = () -> {
			largada.await();
			try {
				Batida b = new Batida();
				b.setEquipamento(equipamento);
				b.setNsr(99999L);
				b.setFuncionario(funcionario);
				b.setDataHora(LocalDateTime.of(2026, 10, 20, 8, 0));
				b.setTipoRegistro(TipoRegistro.ENTRADA);
				b.setTipoOrigem(com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem.ARQUIVO_IMPORTADO);
				b.setStatusBatida(StatusBatida.NORMAL);
				batidaRepository.saveAndFlush(b);
				return true;
			} catch (Exception e) {
				return false;
			}
		};

		Future<Boolean> f1 = executor.submit(inserirMesmoNsr);
		Future<Boolean> f2 = executor.submit(inserirMesmoNsr);
		largada.countDown();

		try {
			boolean r1 = f1.get();
			boolean r2 = f2.get();
			// Apenas uma das duas threads pode ter sucesso; a outra deve bater na constraint uk_batida_equipamento_nsr
			assertTrue(r1 ^ r2, "Exatamente uma inserção deve funcionar e a outra deve ser barrada pela constraint única!");
		} catch (ExecutionException e) {
			fail("Erro inesperado na execução concorrente: " + e.getMessage());
		} finally {
			executor.shutdown();
		}

		assertEquals(1, batidaRepository.count(), "O banco deve conter exatamente 1 registro para o par (equipamento, nsr)!");
	}

	@Test
	@DisplayName("T12 - Detecção automática dos 6 tipos de inconsistência (ORFA, DIA_IMPAR, DUPLICADA_MINUTO, FORA_DA_JORNADA, SEQUENCIA_INVALIDA, EQUIPAMENTO_DESCONHECIDO)")
	void testDeteccaoAutomaticaSeisTiposInconsistencia() {
		inconsistenciaRepository.deleteAll();
		batidaRepository.deleteAll();
		historicoImportacaoRepository.deleteAll();

		Equipamento equipamento = equipamentoRepository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum equipamento encontrado."));
		Funcionario colaborador = funcionarioRepository.findByEmpresaId(equipamento.getEmpresa().getId()).stream()
				.filter(f -> "11111111111".equals(f.getPisPasep()))
				.findFirst()
				.orElseGet(() -> funcionarioRepository.findByEmpresaId(equipamento.getEmpresa().getId()).get(0));

		// 1. EQUIPAMENTO_DESCONHECIDO: tentativa de importar arquivo com equipamento inexistente
		MockMultipartFile arqEqDesc = new MockMultipartFile(
				"arquivo", "afd_eq_desc.txt", "text/plain",
				"1CABECALHO\n318102026080011111111111\n9TRAILER".getBytes(StandardCharsets.UTF_8)
		);
		assertThrows(EquipamentoNaoEncontradoException.class, () ->
				importacaoService.gerarPreview(arqEqDesc, UUID.randomUUID())
		);

		// 2. Feriado Nacional para disparar FORA_DA_JORNADA em 25/12/2026
		LocalDate dataFeriado = LocalDate.of(2026, 12, 25);
		if (feriadoRepository.buscarCalendarioAnual(2026, "SP", "São Paulo", equipamento.getEmpresa().getId())
				.stream().noneMatch(f -> dataFeriado.equals(f.getData()))) {
			Feriado feriado = new Feriado();
			feriado.setDescricao("Natal Teste T12");
			feriado.setData(dataFeriado);
			feriado.setAlcance(Alcance.NACIONAL);
			feriado.setAtivo(true);
			feriadoRepository.saveAndFlush(feriado);
		}

		// 3. Importação com:
		// - 2 batidas no mesmo minuto (25/12/2026 08:00) -> DUPLICADA_MINUTO + DIA_IMPAR (1 batida ativa) + FORA_DA_JORNADA (feriado)
		// - 1 batida com PIS desconhecido -> ORFA
		String conteudo = String.join("\n",
				"1000000000CABECALHO",
				"3000000501251220260800" + colaborador.getPisPasep(),
				"3000000502251220260800" + colaborador.getPisPasep(),
				"300000050325122026120088888888888",
				"9000000003TRAILER"
		);
		MockMultipartFile arqMisto = new MockMultipartFile(
				"arquivo", "afd_t12_tipos.txt", "text/plain", conteudo.getBytes(StandardCharsets.UTF_8)
		);
		importacaoService.confirmarImportacao(arqMisto, equipamento.getId());

		// 4. SEQUENCIA_INVALIDA: primeira batida do dia 26/12/2026 registrada como SAIDA
		batidaService.registrarBatida(new BatidaDTO.Request(
				colaborador.getId(),
				TipoRegistro.SAIDA,
				equipamento.getId(),
				null,
				null,
				"127.0.0.1",
				LocalDateTime.of(2026, 12, 26, 9, 0)
		));

		InconsistenciaDTO.PainelContagemResponse painel = inconsistenciaService.obterPainelContagem(null, null, null);
		assertTrue(painel.abertasPorTipo().getOrDefault(TipoInconsistencia.ORFA, 0L) >= 1, "Deve detectar ORFA");
		assertTrue(painel.abertasPorTipo().getOrDefault(TipoInconsistencia.DIA_IMPAR, 0L) >= 1, "Deve detectar DIA_IMPAR");
		assertTrue(painel.abertasPorTipo().getOrDefault(TipoInconsistencia.DUPLICADA_MINUTO, 0L) >= 1, "Deve detectar DUPLICADA_MINUTO");
		assertTrue(painel.abertasPorTipo().getOrDefault(TipoInconsistencia.FORA_DA_JORNADA, 0L) >= 1, "Deve detectar FORA_DA_JORNADA");
		assertTrue(painel.abertasPorTipo().getOrDefault(TipoInconsistencia.SEQUENCIA_INVALIDA, 0L) >= 1, "Deve detectar SEQUENCIA_INVALIDA");
		assertTrue(painel.abertasPorTipo().getOrDefault(TipoInconsistencia.EQUIPAMENTO_DESCONHECIDO, 0L) >= 1, "Deve detectar EQUIPAMENTO_DESCONHECIDO");
	}

	@Test
	@DisplayName("T12 - Filtros por tipo, situação, colaborador e período + resolução com 4 ações e histórico auditável")
	void testFiltrosTratamentoEHistoricoAuditavel() {
		inconsistenciaRepository.deleteAll();
		batidaRepository.deleteAll();
		historicoImportacaoRepository.deleteAll();

		Equipamento equipamento = equipamentoRepository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum equipamento encontrado."));
		Funcionario colaborador = funcionarioRepository.findByEmpresaId(equipamento.getEmpresa().getId()).stream()
				.findFirst().orElseThrow();

		// Gera 1 batida de ENTRADA isolada em 10/11/2026 -> cria DIA_IMPAR
		batidaService.registrarBatida(new BatidaDTO.Request(
				colaborador.getId(),
				TipoRegistro.ENTRADA,
				equipamento.getId(),
				null,
				null,
				"127.0.0.1",
				LocalDateTime.of(2026, 11, 10, 8, 0)
		));

		// Gera 2 batidas no mesmo minuto em 11/11/2026 -> cria DUPLICADA_MINUTO
		batidaService.registrarBatida(new BatidaDTO.Request(
				colaborador.getId(),
				TipoRegistro.ENTRADA,
				equipamento.getId(),
				null,
				null,
				"127.0.0.1",
				LocalDateTime.of(2026, 11, 11, 8, 15, 10)
		));
		batidaService.registrarBatida(new BatidaDTO.Request(
				colaborador.getId(),
				TipoRegistro.SAIDA,
				equipamento.getId(),
				null,
				null,
				"127.0.0.1",
				LocalDateTime.of(2026, 11, 11, 8, 15, 40)
		));

		// Filtro por tipo DIA_IMPAR, situação ABERTA, colaborador e período
		List<InconsistenciaDTO.Response> abertasDiaImpar = inconsistenciaService.listar(
				TipoInconsistencia.DIA_IMPAR,
				"ABERTA",
				colaborador.getId(),
				LocalDate.of(2026, 11, 1),
				LocalDate.of(2026, 11, 30),
				null
		);
		assertEquals(1, abertasDiaImpar.size(), "Deve encontrar exatamente 1 DIA_IMPAR no período para o colaborador");

		UUID idDiaImpar = abertasDiaImpar.get(0).id();

		// Motivo em branco deve ser rejeitado
		assertThrows(IllegalArgumentException.class, () ->
				inconsistenciaService.tratar(idDiaImpar, new InconsistenciaDTO.TratarRequest(
						AcaoTratamento.INCLUIR_MARCACAO_AJUSTE,
						"   ",
						colaborador.getId(),
						null,
						LocalTime.of(17, 0),
						TipoRegistro.SAIDA
				))
		);

		// Tratamento válido com INCLUIR_MARCACAO_AJUSTE
		InconsistenciaDTO.Response resolvida1 = inconsistenciaService.tratar(
				idDiaImpar,
				new InconsistenciaDTO.TratarRequest(
						AcaoTratamento.INCLUIR_MARCACAO_AJUSTE,
						"Colaborador esqueceu de bater saída ao final do expediente",
						colaborador.getId(),
						null,
						LocalTime.of(17, 0),
						TipoRegistro.SAIDA
				)
		);
		assertEquals(StatusInconsistencia.RESOLVIDA, resolvida1.situacao());
		assertEquals(AcaoTratamento.INCLUIR_MARCACAO_AJUSTE, resolvida1.acaoTomada());
		assertNotNull(resolvida1.tratadoPor());
		assertNotNull(resolvida1.tratadoEm());
		assertEquals("Colaborador esqueceu de bater saída ao final do expediente", resolvida1.motivoTratamento());

		// Saiu da fila aberta de DIA_IMPAR, mas continua consultável em RESOLVIDA
		assertTrue(inconsistenciaService.listar(TipoInconsistencia.DIA_IMPAR, "ABERTA", colaborador.getId(), null, null, "2026-11").isEmpty());
		List<InconsistenciaDTO.Response> historicoDiaImpar = inconsistenciaService.listar(
				TipoInconsistencia.DIA_IMPAR, "RESOLVIDA", colaborador.getId(), null, null, "2026-11"
		);
		assertEquals(1, historicoDiaImpar.size());

		// Tratamento de DUPLICADA_MINUTO com DESCARTAR_DUPLICADA
		List<InconsistenciaDTO.Response> abertasDup = inconsistenciaService.listar(
				TipoInconsistencia.DUPLICADA_MINUTO, "ABERTA", colaborador.getId(), null, null, "2026-11"
		);
		assertEquals(1, abertasDup.size());
		InconsistenciaDTO.Response resolvidaDup = inconsistenciaService.tratar(
				abertasDup.get(0).id(),
				new InconsistenciaDTO.TratarRequest(
						AcaoTratamento.DESCARTAR_DUPLICADA,
						"Duplo clique no leitor biométrico",
						null,
						null,
						null,
						null
				)
		);
		assertEquals(StatusInconsistencia.RESOLVIDA, resolvidaDup.situacao());
		assertEquals(AcaoTratamento.DESCARTAR_DUPLICADA, resolvidaDup.acaoTomada());
	}

	@Test
	@DisplayName("T12 - Fechamento de Período bloqueia quando há pendência aberta e permite fechar registrando aviso explícito")
	void testFechamentoPeriodoBloqueioERessalva() {
		fechamentoPeriodoRepository.deleteAll();
		inconsistenciaRepository.deleteAll();
		batidaRepository.deleteAll();

		Equipamento equipamento = equipamentoRepository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum equipamento encontrado."));
		Funcionario colaborador = funcionarioRepository.findByEmpresaId(equipamento.getEmpresa().getId()).stream()
				.findFirst().orElseThrow();

		// Cria uma batida ímpar em 2026-09 para deixar pendência aberta no período
		batidaService.registrarBatida(new BatidaDTO.Request(
				colaborador.getId(),
				TipoRegistro.ENTRADA,
				equipamento.getId(),
				null,
				null,
				"127.0.0.1",
				LocalDateTime.of(2026, 9, 14, 8, 0)
		));

		// 1. Tentativa de fechar sem confirmar ressalva deve bloquear com FechamentoComPendenciasException
		assertThrows(FechamentoComPendenciasException.class, () ->
				inconsistenciaService.fecharPeriodo(new InconsistenciaDTO.FechamentoRequest(
						"2026-09", null, null, false
				))
		);

		// 2. Fechamento confirmando pendências registra CONCLUIDO_COM_PENDENCIAS e aviso explícito
		InconsistenciaDTO.FechamentoResponse fechamentoComRessalva = inconsistenciaService.fecharPeriodo(
				new InconsistenciaDTO.FechamentoRequest("2026-09", null, null, true)
		);
		assertEquals(StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS, fechamentoComRessalva.status());
		assertTrue(fechamentoComRessalva.fechadoComRessalva());
		assertTrue(fechamentoComRessalva.pendenciasAbertasNoFechamento() >= 1);
		assertTrue(fechamentoComRessalva.avisoPendencias().contains("DIA_IMPAR"));

		// 3. Resolve a pendência com ABONAR_DIA e fecha outro período limpo (CONCLUIDO)
		List<InconsistenciaDTO.Response> abertasSetembro = inconsistenciaService.listar(
				null, "ABERTA", null, null, null, "2026-09"
		);
		for (InconsistenciaDTO.Response inc : abertasSetembro) {
			inconsistenciaService.tratar(inc.id(), new InconsistenciaDTO.TratarRequest(
					AcaoTratamento.ABONAR_DIA,
					"Abono autorizado pela gerência",
					 null, null, null, null
			));
		}

		InconsistenciaDTO.FechamentoResponse fechamentoLimpo = inconsistenciaService.fecharPeriodo(
				new InconsistenciaDTO.FechamentoRequest("2026-09", null, null, false)
		);
		assertEquals(StatusFechamentoPeriodo.CONCLUIDO, fechamentoLimpo.status());
		assertFalse(fechamentoLimpo.fechadoComRessalva());
		assertEquals(0, fechamentoLimpo.pendenciasAbertasNoFechamento());
	}
}