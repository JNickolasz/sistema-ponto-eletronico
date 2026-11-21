package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.domain.Batida;
import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Feriado;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.Inconsistencia;
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
import com.example.projetoPBD.pontoCerto.domain.FechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.SolicitacaoAjuste;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusSolicitacaoAjuste;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.SolicitacaoAjusteDTO;
import com.example.projetoPBD.pontoCerto.repository.SolicitacaoAjusteRepository;
import com.example.projetoPBD.pontoCerto.service.SolicitacaoAjusteService;
import com.example.projetoPBD.pontoCerto.service.exceptions.PeriodoFechadoException;
import com.example.projetoPBD.pontoCerto.domain.ApuracaoDiaria;
import com.example.projetoPBD.pontoCerto.domain.FaixaHoraExtra;
import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import com.example.projetoPBD.pontoCerto.domain.RegraAdicionalNoturno;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusApuracaoDiaria;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ApuracaoDiariaDTO;
import com.example.projetoPBD.pontoCerto.repository.ApuracaoDiariaRepository;
import com.example.projetoPBD.pontoCerto.repository.RegraApuracaoRepository;
import com.example.projetoPBD.pontoCerto.service.ApuracaoDiariaService;
import com.example.projetoPBD.pontoCerto.domain.TipoOcorrencia;
import com.example.projetoPBD.pontoCerto.domain.OcorrenciaColaborador;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoAbonoOcorrencia;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.TipoOcorrenciaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.OcorrenciaColaboradorDTO;
import com.example.projetoPBD.pontoCerto.repository.TipoOcorrenciaRepository;
import com.example.projetoPBD.pontoCerto.repository.OcorrenciaColaboradorRepository;
import com.example.projetoPBD.pontoCerto.service.TipoOcorrenciaService;
import com.example.projetoPBD.pontoCerto.service.OcorrenciaColaboradorService;
import com.example.projetoPBD.pontoCerto.service.AnexoStorageService;
import com.example.projetoPBD.pontoCerto.service.exceptions.OcorrenciaConflitoException;
import com.example.projetoPBD.pontoCerto.domain.MovimentacaoBancoHoras;
import com.example.projetoPBD.pontoCerto.domain.FechamentoBancoHoras;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoMovimentacaoBancoHoras;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BancoHorasDTO;
import com.example.projetoPBD.pontoCerto.repository.MovimentacaoBancoHorasRepository;
import com.example.projetoPBD.pontoCerto.repository.FechamentoBancoHorasRepository;
import com.example.projetoPBD.pontoCerto.service.BancoHorasService;
import com.example.projetoPBD.pontoCerto.domain.HistoricoFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.domain.enums.AcaoFechamentoPeriodo;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FechamentoPeriodoDTO;
import com.example.projetoPBD.pontoCerto.repository.HistoricoFechamentoPeriodoRepository;
import com.example.projetoPBD.pontoCerto.service.CompetenciaLockService;
import com.example.projetoPBD.pontoCerto.service.FechamentoPeriodoService;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EspelhoPontoDTO;
import com.example.projetoPBD.pontoCerto.service.EspelhoService;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import java.math.BigDecimal;
import java.sql.Time;

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

	@Autowired
	private SolicitacaoAjusteService solicitacaoAjusteService;

	@Autowired
	private SolicitacaoAjusteRepository solicitacaoAjusteRepository;

	@Autowired
	private ApuracaoDiariaService apuracaoDiariaService;

	@Autowired
	private ApuracaoDiariaRepository apuracaoDiariaRepository;

	@Autowired
	private RegraApuracaoRepository regraApuracaoRepository;

	@Autowired
	private TipoOcorrenciaRepository tipoOcorrenciaRepository;

	@Autowired
	private OcorrenciaColaboradorRepository ocorrenciaColaboradorRepository;

	@Autowired
	private TipoOcorrenciaService tipoOcorrenciaService;

	@Autowired
	private OcorrenciaColaboradorService ocorrenciaColaboradorService;

	@Autowired
	private AnexoStorageService anexoStorageService;

	@Autowired
	private MovimentacaoBancoHorasRepository movimentacaoBancoHorasRepository;

	@Autowired
	private FechamentoBancoHorasRepository fechamentoBancoHorasRepository;

	@Autowired
	private BancoHorasService bancoHorasService;

	@Autowired
	private FechamentoPeriodoService fechamentoPeriodoService;

	@Autowired
	private HistoricoFechamentoPeriodoRepository historicoFechamentoPeriodoRepository;

	@Autowired
	private CompetenciaLockService competenciaLockService;

	@Autowired
	private EspelhoService espelhoService;

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

	@Test
	@DisplayName("T13 - Imutabilidade: exclusão de batida física (estação ou importada) é estritamente barrada pela Portaria 671")
	void testT13_ImutabilidadeBatidasFiscais() {
		Batida batidaEstacao = new Batida();
		batidaEstacao.setTipoOrigem(TipoOrigem.ESTACAO);
		batidaEstacao.setDataHora(LocalDateTime.of(2026, 11, 10, 8, 0));
		batidaEstacao.setTipoRegistro(TipoRegistro.ENTRADA);

		assertThrows(IllegalStateException.class, batidaEstacao::validarImutabilidadeAoRemover,
				"Deveria lançar IllegalStateException impedindo exclusão de batida de estação");

		Batida batidaArquivo = new Batida();
		batidaArquivo.setTipoOrigem(TipoOrigem.ARQUIVO_IMPORTADO);
		batidaArquivo.setDataHora(LocalDateTime.of(2026, 11, 10, 12, 0));
		batidaArquivo.setTipoRegistro(TipoRegistro.SAIDA);

		assertThrows(IllegalStateException.class, batidaArquivo::validarImutabilidadeAoRemover,
				"Deveria lançar IllegalStateException impedindo exclusão de batida de arquivo AFD");
	}

	@Test
	@DisplayName("T13 - Período Fechado: bloqueia abertura de solicitação de ajuste para competência já encerrada")
	void testT13_RecusaEmPeriodoFechado() {
		fechamentoPeriodoRepository.deleteAll();

		Funcionario colaborador = funcionarioRepository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum funcionário encontrado"));

		FechamentoPeriodo fechamento = new FechamentoPeriodo();
		fechamento.setEmpresa(colaborador.getEmpresa());
		fechamento.setMesReferencia("2026-08");
		fechamento.setDataInicio(LocalDate.of(2026, 8, 1));
		fechamento.setDataFim(LocalDate.of(2026, 8, 31));
		fechamento.setDataFechamento(LocalDateTime.now());
		fechamento.setFechadoPor("RH Teste");
		fechamento.setStatus(StatusFechamentoPeriodo.CONCLUIDO);
		fechamentoPeriodoRepository.save(fechamento);

		// Tentar solicitar ajuste para 15/08/2026 deve ser recusado com PeriodoFechadoException
		SolicitacaoAjusteDTO.CriarRequest request = new SolicitacaoAjusteDTO.CriarRequest(
				LocalDate.of(2026, 8, 15),
				LocalTime.of(8, 30),
				TipoRegistro.ENTRADA,
				"Esqueci de bater entrada neste dia"
		);

		assertThrows(PeriodoFechadoException.class, () ->
				solicitacaoAjusteService.abrirSolicitacao(colaborador, request)
		);
	}

	@Test
	@DisplayName("T13 - Deferimento: gestor aprova solicitação com justificativa e gera nova batida de ajuste com NSR sequencial")
	void testT13_DeferimentoGeraBatidaENsr() {
		Funcionario gestor = funcionarioRepository.findAll().stream()
				.filter(f -> f.getPerfilAcesso() == PerfilAcesso.GESTOR || f.getPerfilAcesso() == PerfilAcesso.RH)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum gestor encontrado"));

		Funcionario colaborador = funcionarioRepository.findAll().stream()
				.filter(f -> !f.getId().equals(gestor.getId()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum colaborador encontrado"));

		colaborador.setGestor(gestor);
		colaborador = funcionarioRepository.save(colaborador);

		// 1. Colaborador abre solicitação
		SolicitacaoAjusteDTO.CriarRequest criarReq = new SolicitacaoAjusteDTO.CriarRequest(
				LocalDate.of(2026, 11, 5),
				LocalTime.of(9, 0),
				TipoRegistro.ENTRADA,
				"Falta de conexão com a estação"
		);
		SolicitacaoAjusteDTO.Response solicitacaoAberta = solicitacaoAjusteService.abrirSolicitacao(colaborador, criarReq);
		assertEquals(StatusSolicitacaoAjuste.PENDENTE, solicitacaoAberta.status());
		assertNull(solicitacaoAberta.batidaCriadaId());

		// 2. Gestor defere com justificativa
		SolicitacaoAjusteDTO.AnalisarRequest analisarReq = new SolicitacaoAjusteDTO.AnalisarRequest(
				StatusSolicitacaoAjuste.DEFERIDO,
				"Atesto que o colaborador compareceu presencialmente às 09:00"
		);
		SolicitacaoAjusteDTO.Response deferida = solicitacaoAjusteService.analisarSolicitacao(
				solicitacaoAberta.id(),
				gestor,
				analisarReq
		);

		assertEquals(StatusSolicitacaoAjuste.DEFERIDO, deferida.status());
		assertEquals("Atesto que o colaborador compareceu presencialmente às 09:00", deferida.justificativaGestor());
		assertNotNull(deferida.batidaCriadaId(), "Deve gerar a batida de ajuste associada");

		// 3. Valida no banco a Batida criada
		Batida batida = batidaRepository.findById(deferida.batidaCriadaId()).orElseThrow();
		assertEquals(TipoOrigem.AJUSTE_MANUAL, batida.getTipoOrigem());
		assertEquals(TipoRegistro.ENTRADA, batida.getTipoRegistro());
		assertNotNull(batida.getNsr(), "NSR deve ser preenchido pela sequence");
		assertEquals(solicitacaoAberta.id(), batida.getSolicitacaoAjusteId());
		assertNotNull(batida.getAutorAjuste());
	}

	@Test
	@DisplayName("T13 - Indeferimento: gestor recusa solicitação com justificativa e nenhuma batida é criada")
	void testT13_IndeferimentoSemGerarBatida() {
		Funcionario gestor = funcionarioRepository.findAll().stream()
				.filter(f -> f.getPerfilAcesso() == PerfilAcesso.GESTOR || f.getPerfilAcesso() == PerfilAcesso.RH)
				.findFirst()
				.orElseThrow();

		Funcionario colaborador = funcionarioRepository.findAll().stream()
				.filter(f -> !f.getId().equals(gestor.getId()))
				.findFirst()
				.orElseThrow();

		colaborador.setGestor(gestor);
		colaborador = funcionarioRepository.save(colaborador);

		SolicitacaoAjusteDTO.CriarRequest criarReq = new SolicitacaoAjusteDTO.CriarRequest(
				LocalDate.of(2026, 11, 6),
				LocalTime.of(18, 0),
				TipoRegistro.SAIDA,
				"Esqueci de bater saída"
		);
		SolicitacaoAjusteDTO.Response solicitacaoAberta = solicitacaoAjusteService.abrirSolicitacao(colaborador, criarReq);

		long totalBatidasAntes = batidaRepository.count();

		SolicitacaoAjusteDTO.AnalisarRequest analisarReq = new SolicitacaoAjusteDTO.AnalisarRequest(
				StatusSolicitacaoAjuste.INDEFERIDO,
				"Imagens da câmera não confirmam permanência no posto até as 18:00"
		);
		SolicitacaoAjusteDTO.Response indeferida = solicitacaoAjusteService.analisarSolicitacao(
				solicitacaoAberta.id(),
				gestor,
				analisarReq
		);

		assertEquals(StatusSolicitacaoAjuste.INDEFERIDO, indeferida.status());
		assertNull(indeferida.batidaCriadaId());
		assertEquals("Imagens da câmera não confirmam permanência no posto até as 18:00", indeferida.justificativaGestor());
		assertEquals(totalBatidasAntes, batidaRepository.count(), "Nenhuma batida nova deve ter sido criada no indeferimento");
	}

	@Test
	@DisplayName("T14 - Pareamento Normal: 4 batidas no dia (08:00, 12:00, 13:00, 17:00) geram 2 pares, 480m trabalhados e 60m intervalo")
	void testT14_PareamentoNormalQuatroBatidas() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		LocalDate data = LocalDate.of(2026, 11, 10);

		// Limpa batidas e apurações anteriores da data de teste
		apuracaoDiariaRepository.deleteByFuncionarioIdAndDataReferencia(funcionario.getId(), data);

		Batida b1 = criarBatidaTeste(funcionario, data.atTime(8, 0), TipoRegistro.ENTRADA);
		Batida b2 = criarBatidaTeste(funcionario, data.atTime(12, 0), TipoRegistro.SAIDA);
		Batida b3 = criarBatidaTeste(funcionario, data.atTime(13, 0), TipoRegistro.ENTRADA);
		Batida b4 = criarBatidaTeste(funcionario, data.atTime(17, 0), TipoRegistro.SAIDA);
		batidaRepository.saveAll(List.of(b1, b2, b3, b4));

		ApuracaoDiaria apuracao = apuracaoDiariaService.apurarDia(funcionario, data);

		assertNotNull(apuracao);
		assertEquals(StatusApuracaoDiaria.APURADO, apuracao.getStatus());
		assertEquals(480, apuracao.getMinutosTrabalhados(), "Deve ter 480 minutos trabalhados (8h)");
		assertEquals(60, apuracao.getMinutosIntervalo(), "Deve ter 60 minutos de intervalo (12h às 13h)");
		assertEquals(0, apuracao.getMinutosAtraso());
		assertEquals(0, apuracao.getMinutosFalta());
		assertNotNull(apuracao.getParesJson());
		assertTrue(apuracao.getParesJson().contains("08:00"));
		assertTrue(apuracao.getParesJson().contains("17:00"));
	}

	@Test
	@DisplayName("T14 - Dia Ímpar: 3 batidas geram status PENDENTE_INCONSISTENCIA, horas zeradas e pendência DIA_IMPAR")
	void testT14_DiaImparPendenteInconsistenciaSemInventarHorario() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		LocalDate data = LocalDate.of(2026, 11, 11);

		apuracaoDiariaRepository.deleteByFuncionarioIdAndDataReferencia(funcionario.getId(), data);

		Batida b1 = criarBatidaTeste(funcionario, data.atTime(8, 0), TipoRegistro.ENTRADA);
		Batida b2 = criarBatidaTeste(funcionario, data.atTime(12, 0), TipoRegistro.SAIDA);
		Batida b3 = criarBatidaTeste(funcionario, data.atTime(13, 0), TipoRegistro.ENTRADA);
		batidaRepository.saveAll(List.of(b1, b2, b3));

		ApuracaoDiaria apuracao = apuracaoDiariaService.apurarDia(funcionario, data);

		assertNotNull(apuracao);
		assertEquals(StatusApuracaoDiaria.PENDENTE_INCONSISTENCIA, apuracao.getStatus());
		assertEquals(0, apuracao.getMinutosTrabalhados(), "Não deve computar horas inventadas em dia ímpar");
		assertEquals(0, apuracao.getMinutosHoraExtraTotal());

		// Confirma que pendência DIA_IMPAR foi registrada ou verificada no InconsistenciaService
		boolean temInconsistenciaImpar = inconsistenciaRepository
				.findByEmpresaIdAndFuncionarioIdAndDataReferenciaAndTipoAndSituacao(
						funcionario.getEmpresa().getId(),
						funcionario.getId(),
						data,
						TipoInconsistencia.DIA_IMPAR,
						StatusInconsistencia.ABERTA
				).size() > 0;
		assertTrue(temInconsistenciaImpar, "Deve haver inconsistência DIA_IMPAR aberta para o dia ímpar");
	}

	@Test
	@DisplayName("T14 - Tolerância CLT Art. 58 §1: Atraso de 4 minutos dentro do limite não desconta atraso")
	void testT14_ToleranciaCltAtraso() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		LocalDate data = LocalDate.of(2026, 11, 12);

		apuracaoDiariaRepository.deleteByFuncionarioIdAndDataReferencia(funcionario.getId(), data);

		// Entrada com 4 minutos de atraso (08:04), saída pontual 17:00 (intervalo 12:00 às 13:00)
		Batida b1 = criarBatidaTeste(funcionario, data.atTime(8, 4), TipoRegistro.ENTRADA);
		Batida b2 = criarBatidaTeste(funcionario, data.atTime(12, 0), TipoRegistro.SAIDA);
		Batida b3 = criarBatidaTeste(funcionario, data.atTime(13, 0), TipoRegistro.ENTRADA);
		Batida b4 = criarBatidaTeste(funcionario, data.atTime(17, 0), TipoRegistro.SAIDA);
		batidaRepository.saveAll(List.of(b1, b2, b3, b4));

		ApuracaoDiaria apuracao = apuracaoDiariaService.apurarDia(funcionario, data);

		assertNotNull(apuracao);
		assertEquals(StatusApuracaoDiaria.APURADO, apuracao.getStatus());
		assertEquals(0, apuracao.getMinutosAtraso(), "Atraso de 4 minutos deve ser absorvido pela tolerância CLT");
		assertEquals(0, apuracao.getMinutosSaidaAntecipada());
	}

	@Test
	@DisplayName("T14 - Faixas de Horas Extras: Excedente rateado conforme faixas configuradas (ex: 2h a 50% e restante a 100%)")
	void testT14_HorasExtrasComFaixas() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		LocalDate data = LocalDate.of(2026, 11, 13); // Sexta-feira

		apuracaoDiariaRepository.deleteByFuncionarioIdAndDataReferencia(funcionario.getId(), data);

		// Criar regra com faixas para a empresa: 120 min a 50%, e excedente a 100%
		RegraApuracao regra = new RegraApuracao();
		regra.setEmpresa(funcionario.getEmpresa());
		regra.setInicioVigencia(data.minusDays(10));
		regra.setLimiteDiarioExtraMinutos(120);

		FaixaHoraExtra f1 = new FaixaHoraExtra(1, 120, new BigDecimal("50.00"));
		FaixaHoraExtra f2 = new FaixaHoraExtra(2, null, new BigDecimal("100.00"));
		regra.setFaixaHoraExtra(f1);
		regra.setFaixaHoraExtra(f2);
		regraApuracaoRepository.save(regra);

		// Trabalhou das 08:00 às 12:00 e 13:00 às 20:00 (4h + 7h = 11h trabalhadas = 660 min)
		// Carga prevista: 8h (480 min). Excedente: 3h (180 min extras)
		Batida b1 = criarBatidaTeste(funcionario, data.atTime(8, 0), TipoRegistro.ENTRADA);
		Batida b2 = criarBatidaTeste(funcionario, data.atTime(12, 0), TipoRegistro.SAIDA);
		Batida b3 = criarBatidaTeste(funcionario, data.atTime(13, 0), TipoRegistro.ENTRADA);
		Batida b4 = criarBatidaTeste(funcionario, data.atTime(20, 0), TipoRegistro.SAIDA);
		batidaRepository.saveAll(List.of(b1, b2, b3, b4));

		ApuracaoDiaria apuracao = apuracaoDiariaService.apurarDia(funcionario, data);

		assertNotNull(apuracao);
		assertEquals(180, apuracao.getMinutosHoraExtraTotal(), "Total de horas extras deve ser 180 min (3h)");
		assertNotNull(apuracao.getHorasExtrasPorFaixaJson());
		assertTrue(apuracao.getHorasExtrasPorFaixaJson().contains("\"50%\":120") || apuracao.getHorasExtrasPorFaixaJson().contains("\"50%\": 120"));
		assertTrue(apuracao.getHorasExtrasPorFaixaJson().contains("\"100%\":60") || apuracao.getHorasExtrasPorFaixaJson().contains("\"100%\": 60"));
	}

	@Test
	@DisplayName("T14 - Hora Noturna Reduzida: Trabalho das 22:00 às 05:00 (420 min relógio) convertido para 480 min noturnos computados")
	void testT14_HoraNoturnaReduzida() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		LocalDate data = LocalDate.of(2026, 11, 14);

		apuracaoDiariaRepository.deleteByFuncionarioIdAndDataReferencia(funcionario.getId(), data);

		// Turno noturno: 22:00 às 05:00 do dia seguinte (7 horas relógio = 420 minutos)
		Batida b1 = criarBatidaTeste(funcionario, data.atTime(22, 0), TipoRegistro.ENTRADA);
		Batida b2 = criarBatidaTeste(funcionario, data.plusDays(1).atTime(5, 0), TipoRegistro.SAIDA);
		batidaRepository.saveAll(List.of(b1, b2));

		ApuracaoDiaria apuracao = apuracaoDiariaService.apurarDia(funcionario, data);

		assertNotNull(apuracao);
		assertEquals(420, apuracao.getMinutosTrabalhados(), "Minutos relógio trabalhados: 420 (7h)");
		// 420 * (60 / 52.5) = 420 * (8 / 7) = 480 minutos noturnos computados
		assertEquals(480, apuracao.getMinutosAdicionalNoturno(), "Com a redução ficta (52m30s), 7h relógio viram 8h noturnas (480 min)");
	}

	@Test
	@DisplayName("T14 - Falta Integral: Dia útil previsto sem nenhuma marcação gera status FALTA e minutosFalta integral")
	void testT14_FaltaIntegralDiaUtilSemBatidas() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		LocalDate data = LocalDate.of(2026, 11, 9); // Segunda-feira (dia útil)

		apuracaoDiariaRepository.deleteByFuncionarioIdAndDataReferencia(funcionario.getId(), data);

		ApuracaoDiaria apuracao = apuracaoDiariaService.apurarDia(funcionario, data);

		assertNotNull(apuracao);
		if (apuracao.getMinutosPrevistos() > 0) {
			assertEquals(StatusApuracaoDiaria.FALTA, apuracao.getStatus());
			assertEquals(apuracao.getMinutosPrevistos(), apuracao.getMinutosFalta(), "Minutos de falta devem ser iguais aos previstos");
			assertEquals(0, apuracao.getMinutosTrabalhados());
		} else {
			assertEquals(StatusApuracaoDiaria.FOLGA, apuracao.getStatus());
		}
	}

	private Batida criarBatidaTeste(Funcionario funcionario, LocalDateTime dataHora, TipoRegistro tipoRegistro) {
		Batida b = new Batida();
		b.setNsr(batidaRepository.proximoNsrSequence());
		b.setFuncionario(funcionario);
		b.setDataHora(dataHora);
		b.setTipoRegistro(tipoRegistro);
		b.setTipoOrigem(TipoOrigem.ESTACAO);
		b.setStatusBatida(StatusBatida.NORMAL);
		return b;
	}

	@Test
	@DisplayName("T15 - Abono Integral: 3 dias de atestado sem batidas não geram falta")
	void testT15_AbonoIntegralSemFalta() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);

		LocalDate inicio = LocalDate.of(2026, 11, 23); // Segunda
		LocalDate fim = LocalDate.of(2026, 11, 25);    // Quarta

		// Limpa apurações e ocorrências prévias no intervalo
		for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
			apuracaoDiariaRepository.deleteByFuncionarioIdAndDataReferencia(funcionario.getId(), d);
		}
		List<OcorrenciaColaborador> prev = ocorrenciaColaboradorRepository.findSobrepostas(funcionario.getId(), inicio, fim);
		ocorrenciaColaboradorRepository.deleteAll(prev);

		TipoOcorrencia tipo = tipoOcorrenciaRepository.findByEmpresaIdOrderByNomeAsc(funcionario.getEmpresa().getId())
				.stream().filter(t -> t.getTipoAbono() == TipoAbonoOcorrencia.INTEGRAL)
				.findFirst()
				.orElseGet(() -> {
					TipoOcorrencia t = new TipoOcorrencia();
					t.setEmpresa(funcionario.getEmpresa());
					t.setNome("Atestado Integral T15 " + UUID.randomUUID().toString().substring(0, 5));
					t.setTipoAbono(TipoAbonoOcorrencia.INTEGRAL);
					t.setExigeAnexo(false);
					t.setAtivo(true);
					return tipoOcorrenciaRepository.save(t);
				});

		OcorrenciaColaboradorDTO.CriarRequest req = new OcorrenciaColaboradorDTO.CriarRequest(
				funcionario.getId(),
				tipo.getId(),
				inicio,
				fim,
				null,
				"Atestado 3 dias T15"
		);

		OcorrenciaColaboradorDTO.Response resp = ocorrenciaColaboradorService.lancarOcorrencia(req, null);
		assertNotNull(resp);

		for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
			ApuracaoDiaria apuracao = apuracaoDiariaService.apurarDia(funcionario, d);
			assertNotNull(apuracao);
			if (apuracao.getMinutosPrevistos() > 0) {
				assertEquals(StatusApuracaoDiaria.ABONADO, apuracao.getStatus(), "Dia útil coberto por abono integral deve ter status ABONADO");
				assertEquals(0, apuracao.getMinutosFalta(), "Não pode ter minutos de falta quando integralmente abonado");
				assertTrue(apuracao.getMinutosAbonados() > 0, "Minutos abonados devem ser maiores que zero");
			}
		}

		// Cleanup
		ocorrenciaColaboradorService.cancelarOcorrencia(resp.id());
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T15 - Abono Parcial (Meio Período): Jornada 8h com 4h abonadas + 4h batidas zera faltas e atrasos")
	void testT15_AbonoParcialMeioPeriodo() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);

		LocalDate data = LocalDate.of(2026, 11, 26); // Quinta-feira

		apuracaoDiariaRepository.deleteByFuncionarioIdAndDataReferencia(funcionario.getId(), data);
		List<OcorrenciaColaborador> prev = ocorrenciaColaboradorRepository.findSobrepostas(funcionario.getId(), data, data);
		ocorrenciaColaboradorRepository.deleteAll(prev);

		TipoOcorrencia tipo = tipoOcorrenciaRepository.findByEmpresaIdOrderByNomeAsc(funcionario.getEmpresa().getId())
				.stream().filter(t -> t.getTipoAbono() == TipoAbonoOcorrencia.PARCIAL)
				.findFirst()
				.orElseGet(() -> {
					TipoOcorrencia t = new TipoOcorrencia();
					t.setEmpresa(funcionario.getEmpresa());
					t.setNome("Atestado Parcial T15 " + UUID.randomUUID().toString().substring(0, 5));
					t.setTipoAbono(TipoAbonoOcorrencia.PARCIAL);
					t.setMinutosAbonoPadrao(240);
					t.setExigeAnexo(false);
					t.setAtivo(true);
					return tipoOcorrenciaRepository.save(t);
				});

		// 1 par de batidas de 4h: 14:00 às 18:00 (240 min trabalhados)
		Batida b1 = criarBatidaTeste(funcionario, data.atTime(14, 0), TipoRegistro.ENTRADA);
		Batida b2 = criarBatidaTeste(funcionario, data.atTime(18, 0), TipoRegistro.SAIDA);
		batidaRepository.saveAll(List.of(b1, b2));

		OcorrenciaColaboradorDTO.CriarRequest req = new OcorrenciaColaboradorDTO.CriarRequest(
				funcionario.getId(),
				tipo.getId(),
				data,
				data,
				240,
				"Atestado Parcial 4h"
		);

		OcorrenciaColaboradorDTO.Response resp = ocorrenciaColaboradorService.lancarOcorrencia(req, null);
		assertNotNull(resp);

		ApuracaoDiaria apuracao = apuracaoDiariaService.apurarDia(funcionario, data);
		assertNotNull(apuracao);
		assertEquals(240, apuracao.getMinutosTrabalhados(), "Deve ter 240 min trabalhados");
		assertEquals(240, apuracao.getMinutosAbonados(), "Deve ter 240 min abonados");
		assertEquals(0, apuracao.getMinutosFalta(), "Não deve ter falta com trabalho + abono cobrindo jornada");
		assertEquals(0, apuracao.getMinutosAtraso(), "Atraso do turno da manhã deve ter sido abatido pelo abono");

		// Cleanup
		ocorrenciaColaboradorService.cancelarOcorrencia(resp.id());
		batidaRepository.deleteAll(List.of(b1, b2));
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T15 - Bloqueio de Sobreposição: Não permite lançar duas ocorrências com interseção de datas")
	void testT15_BloqueioSobreposicaoOcorrencias() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);

		LocalDate ini1 = LocalDate.of(2026, 12, 10);
		LocalDate fim1 = LocalDate.of(2026, 12, 15);
		LocalDate ini2 = LocalDate.of(2026, 12, 14);
		LocalDate fim2 = LocalDate.of(2026, 12, 18);

		List<OcorrenciaColaborador> prev = ocorrenciaColaboradorRepository.findSobrepostas(funcionario.getId(), ini1, fim2);
		ocorrenciaColaboradorRepository.deleteAll(prev);

		TipoOcorrencia tipo = tipoOcorrenciaRepository.findByEmpresaIdOrderByNomeAsc(funcionario.getEmpresa().getId())
				.stream().findFirst().orElseGet(() -> {
					TipoOcorrencia t = new TipoOcorrencia();
					t.setEmpresa(funcionario.getEmpresa());
					t.setNome("Folga T15 " + UUID.randomUUID().toString().substring(0, 5));
					t.setTipoAbono(TipoAbonoOcorrencia.INTEGRAL);
					t.setExigeAnexo(false);
					t.setAtivo(true);
					return tipoOcorrenciaRepository.save(t);
				});

		OcorrenciaColaboradorDTO.CriarRequest req1 = new OcorrenciaColaboradorDTO.CriarRequest(
				funcionario.getId(), tipo.getId(), ini1, fim1, null, "Primeira ocorrência"
		);
		OcorrenciaColaboradorDTO.Response resp1 = ocorrenciaColaboradorService.lancarOcorrencia(req1, null);
		assertNotNull(resp1);

		OcorrenciaColaboradorDTO.CriarRequest req2 = new OcorrenciaColaboradorDTO.CriarRequest(
				funcionario.getId(), tipo.getId(), ini2, fim2, null, "Ocorrência sobreposta"
		);

		assertThrows(OcorrenciaConflitoException.class, () -> {
			ocorrenciaColaboradorService.lancarOcorrencia(req2, null);
		}, "Deve lançar OcorrenciaConflitoException ao detectar colisão de datas");

		// Cleanup
		ocorrenciaColaboradorService.cancelarOcorrencia(resp1.id());
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T15 - Bloqueio de Período Fechado: Recusa lançar ocorrência em competência concluída")
	void testT15_BloqueioPeriodoFechado() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);

		LocalDate data = LocalDate.of(2026, 7, 15);
		String mesReferencia = "2026-07";

		fechamentoPeriodoRepository.deleteAll();

		FechamentoPeriodo fechamento = new FechamentoPeriodo();
		fechamento.setEmpresa(funcionario.getEmpresa());
		fechamento.setMesReferencia(mesReferencia);
		fechamento.setDataInicio(LocalDate.of(2026, 7, 1));
		fechamento.setDataFim(LocalDate.of(2026, 7, 31));
		fechamento.setDataFechamento(LocalDateTime.now());
		fechamento.setFechadoPor("RH Teste");
		fechamento.setStatus(StatusFechamentoPeriodo.CONCLUIDO);
		fechamentoPeriodoRepository.save(fechamento);

		TipoOcorrencia tipo = tipoOcorrenciaRepository.findByEmpresaIdOrderByNomeAsc(funcionario.getEmpresa().getId())
				.stream().findFirst().orElseGet(() -> {
					TipoOcorrencia t = new TipoOcorrencia();
					t.setEmpresa(funcionario.getEmpresa());
					t.setNome("Atestado Fechado T15 " + UUID.randomUUID().toString().substring(0, 5));
					t.setTipoAbono(TipoAbonoOcorrencia.INTEGRAL);
					t.setExigeAnexo(false);
					t.setAtivo(true);
					return tipoOcorrenciaRepository.save(t);
				});

		OcorrenciaColaboradorDTO.CriarRequest req = new OcorrenciaColaboradorDTO.CriarRequest(
				funcionario.getId(), tipo.getId(), data, data, null, "Tentativa em mês fechado"
		);

		assertThrows(PeriodoFechadoException.class, () -> {
			ocorrenciaColaboradorService.lancarOcorrencia(req, null);
		}, "Deve lançar PeriodoFechadoException ao tentar lançar em mês já fechado");

		fechamentoPeriodoRepository.deleteAll();
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T15 - Anexo Comprobatório: Upload de comprovante e recuperação com integridade")
	void testT15_UploadDownloadAnexo() throws Exception {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);

		LocalDate data = LocalDate.of(2026, 11, 27);
		List<OcorrenciaColaborador> prev = ocorrenciaColaboradorRepository.findSobrepostas(funcionario.getId(), data, data);
		ocorrenciaColaboradorRepository.deleteAll(prev);

		TipoOcorrencia tipo = new TipoOcorrencia();
		tipo.setEmpresa(funcionario.getEmpresa());
		tipo.setNome("Atestado com Anexo " + UUID.randomUUID().toString().substring(0, 5));
		tipo.setTipoAbono(TipoAbonoOcorrencia.INTEGRAL);
		tipo.setExigeAnexo(true);
		tipo.setAtivo(true);
		tipo = tipoOcorrenciaRepository.save(tipo);

		byte[] arquivoBytes = "conteudo-do-atestado-medico-em-pdf-simulado".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile mockFile = new MockMultipartFile(
				"arquivo",
				"atestado_teste.pdf",
				"application/pdf",
				arquivoBytes
		);

		OcorrenciaColaboradorDTO.CriarRequest req = new OcorrenciaColaboradorDTO.CriarRequest(
				funcionario.getId(), tipo.getId(), data, data, null, "Comprovante médico anexo"
		);

		OcorrenciaColaboradorDTO.Response resp = ocorrenciaColaboradorService.lancarOcorrencia(req, mockFile);
		assertNotNull(resp);
		assertEquals("atestado_teste.pdf", resp.nomeArquivo());
		assertTrue(resp.possuiAnexo());

		Resource recurso = ocorrenciaColaboradorService.carregarAnexo(resp.id());
		assertNotNull(recurso);
		assertTrue(recurso.exists());
		assertEquals(arquivoBytes.length, recurso.contentLength());

		// Cancelar ocorrência deve também remover o arquivo do disco
		ocorrenciaColaboradorService.cancelarOcorrencia(resp.id());
		assertFalse(ocorrenciaColaboradorRepository.existsById(resp.id()));

		SecurityContextHolder.clearContext();
	}

	private void autenticarComoRh(Funcionario f) {
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
				new FuncionarioUserDetails(f),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_RH"))
		);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Test
	@DisplayName("T16 - Derivação Automática: 1h extra (+60m) e 30m de atraso (-30m) geram saldo acumulado de +30m sem intervenção manual")
	void testT16_DerivacaoAutomatica() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);

		LocalDate dia1 = LocalDate.of(2026, 9, 14); // Segunda
		LocalDate dia2 = LocalDate.of(2026, 9, 15); // Terça

		fechamentoPeriodoRepository.deleteAll();
		fechamentoBancoHorasRepository.deleteAll();
		movimentacaoBancoHorasRepository.deleteAll();
		apuracaoDiariaRepository.deleteByFuncionarioIdAndDataReferencia(funcionario.getId(), dia1);
		apuracaoDiariaRepository.deleteByFuncionarioIdAndDataReferencia(funcionario.getId(), dia2);

		// Dia 1: Trabalhou 9h (08:00-12:00 e 13:00-18:00 = 540m, previsto 480m -> +60m extra)
		Batida b1 = criarBatidaTeste(funcionario, dia1.atTime(8, 0), TipoRegistro.ENTRADA);
		Batida b2 = criarBatidaTeste(funcionario, dia1.atTime(12, 0), TipoRegistro.SAIDA);
		Batida b3 = criarBatidaTeste(funcionario, dia1.atTime(13, 0), TipoRegistro.ENTRADA);
		Batida b4 = criarBatidaTeste(funcionario, dia1.atTime(18, 0), TipoRegistro.SAIDA);
		batidaRepository.saveAll(List.of(b1, b2, b3, b4));

		// Dia 2: Entrou 30m atrasado (08:30-12:00 e 13:00-17:00 = 450m -> -30m atraso)
		Batida b5 = criarBatidaTeste(funcionario, dia2.atTime(8, 30), TipoRegistro.ENTRADA);
		Batida b6 = criarBatidaTeste(funcionario, dia2.atTime(12, 0), TipoRegistro.SAIDA);
		Batida b7 = criarBatidaTeste(funcionario, dia2.atTime(13, 0), TipoRegistro.ENTRADA);
		Batida b8 = criarBatidaTeste(funcionario, dia2.atTime(17, 0), TipoRegistro.SAIDA);
		batidaRepository.saveAll(List.of(b5, b6, b7, b8));

		apuracaoDiariaService.apurarDia(funcionario, dia1);
		apuracaoDiariaService.apurarDia(funcionario, dia2);

		BancoHorasDTO.ExtratoPeriodoResponse extrato = bancoHorasService.consultarExtrato(funcionario.getId(), dia1, dia2);

		assertNotNull(extrato);
		assertEquals(60, extrato.creditosPeriodoMinutos(), "Deve ter +60m de crédito no dia 1");
		assertEquals(30, extrato.debitosPeriodoMinutos(), "Deve ter -30m de débito no dia 2");
		assertEquals(30, extrato.saldoFinalMinutos(), "Saldo acumulado final deve ser +30m");

		batidaRepository.deleteAll(List.of(b1, b2, b3, b4, b5, b6, b7, b8));
		movimentacaoBancoHorasRepository.deleteAll();
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T16 - Extrato com Saldo Inicial e Final: Filtrar período intermediário reflete acumulado anterior")
	void testT16_ExtratoSaldoInicialEFinal() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);

		fechamentoPeriodoRepository.deleteAll();
		fechamentoBancoHorasRepository.deleteAll();
		movimentacaoBancoHorasRepository.deleteAll();

		// Movimentação anterior ao período filtrado (dia 05/09: +120 min)
		MovimentacaoBancoHoras movAnterior = new MovimentacaoBancoHoras();
		movAnterior.setFuncionario(funcionario);
		movAnterior.setDataReferencia(LocalDate.of(2026, 9, 5));
		movAnterior.setTipo(TipoMovimentacaoBancoHoras.CREDITO_APURACAO);
		movAnterior.setMinutos(120);
		movAnterior.setDescricao("Crédito anterior +02:00");
		movAnterior.setRegistradoEm(LocalDateTime.now());
		movimentacaoBancoHorasRepository.saveAndFlush(movAnterior);

		// Movimentação dentro do período filtrado (dia 15/09: +60 min)
		MovimentacaoBancoHoras movNoPeriodo = new MovimentacaoBancoHoras();
		movNoPeriodo.setFuncionario(funcionario);
		movNoPeriodo.setDataReferencia(LocalDate.of(2026, 9, 15));
		movNoPeriodo.setTipo(TipoMovimentacaoBancoHoras.CREDITO_APURACAO);
		movNoPeriodo.setMinutos(60);
		movNoPeriodo.setDescricao("Crédito no período +01:00");
		movNoPeriodo.setRegistradoEm(LocalDateTime.now());
		movimentacaoBancoHorasRepository.saveAndFlush(movNoPeriodo);

		BancoHorasDTO.ExtratoPeriodoResponse extrato = bancoHorasService.consultarExtrato(
				funcionario.getId(),
				LocalDate.of(2026, 9, 10),
				LocalDate.of(2026, 9, 20)
		);

		assertEquals(120, extrato.saldoInicialMinutos(), "Saldo inicial em 10/09 deve refletir os +120m acumulados até 09/09");
		assertEquals(60, extrato.creditosPeriodoMinutos());
		assertEquals(180, extrato.saldoFinalMinutos(), "Saldo final em 20/09 deve ser 120 + 60 = 180m");

		movimentacaoBancoHorasRepository.deleteAll();
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T16 - Registro de Compensação: Abatimento de 02:00 com motivo obrigatório atualiza saldo acumulado")
	void testT16_RegistroCompensacao() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);

		fechamentoPeriodoRepository.deleteAll();
		fechamentoBancoHorasRepository.deleteAll();
		movimentacaoBancoHorasRepository.deleteAll();

		LocalDate dataCredito = LocalDate.of(2026, 9, 10);
		LocalDate dataComp = LocalDate.of(2026, 9, 18);

		MovimentacaoBancoHoras movCredito = new MovimentacaoBancoHoras();
		movCredito.setFuncionario(funcionario);
		movCredito.setDataReferencia(dataCredito);
		movCredito.setTipo(TipoMovimentacaoBancoHoras.CREDITO_APURACAO);
		movCredito.setMinutos(240); // +04:00 acumulados
		movCredito.setDescricao("Crédito +04:00");
		movCredito.setRegistradoEm(LocalDateTime.now());
		movimentacaoBancoHorasRepository.saveAndFlush(movCredito);

		BancoHorasDTO.RegistrarCompensacaoRequest req = new BancoHorasDTO.RegistrarCompensacaoRequest(
				funcionario.getId(),
				dataComp,
				120, // 02:00 compensadas
				"Saída antecipada compensada com banco de horas"
		);

		BancoHorasDTO.ItemExtratoResponse compSalva = bancoHorasService.registrarCompensacao(funcionario, req);
		assertNotNull(compSalva);
		assertEquals(-120, compSalva.minutos());
		assertEquals(120, compSalva.saldoAcumuladoMinutos(), "Saldo após abater 120m de 240m deve ser 120m");

		movimentacaoBancoHorasRepository.deleteAll();
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T16 - Imutabilidade de Período Fechado: Fechar outubro com +10:00 preserva saldo congelado após alterar regras")
	void testT16_ImutabilidadePeriodoFechado() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);

		fechamentoPeriodoRepository.deleteAll();
		fechamentoBancoHorasRepository.deleteAll();
		movimentacaoBancoHorasRepository.deleteAll();

		LocalDate diaOutubro = LocalDate.of(2026, 10, 15);
		MovimentacaoBancoHoras movOutubro = new MovimentacaoBancoHoras();
		movOutubro.setFuncionario(funcionario);
		movOutubro.setDataReferencia(diaOutubro);
		movOutubro.setTipo(TipoMovimentacaoBancoHoras.CREDITO_APURACAO);
		movOutubro.setMinutos(600); // +10:00
		movOutubro.setDescricao("Crédito consolidado outubro +10:00");
		movOutubro.setRegistradoEm(LocalDateTime.now());
		movimentacaoBancoHorasRepository.saveAndFlush(movOutubro);

		// Congela o período de Outubro/2026
		FechamentoBancoHoras snapshot = bancoHorasService.congelarPeriodoFuncionario(funcionario, "2026-10", "RH Teste");
		assertNotNull(snapshot);
		assertEquals(600, snapshot.getSaldoFinalCongeladoMinutos());

		// Simula tentativa de recálculo / alteração de apuração em Outubro/2026
		ApuracaoDiaria apNova = new ApuracaoDiaria();
		apNova.setId(UUID.randomUUID());
		apNova.setFuncionario(funcionario);
		apNova.setDataReferencia(diaOutubro);
		apNova.setStatus(StatusApuracaoDiaria.FALTA);
		apNova.setMinutosFalta(480);
		bancoHorasService.derivarMovimentacoesDaApuracao(apNova);

		// Consulta extrato de outubro -> deve permanecer intacto em +10:00 (600 min)
		BancoHorasDTO.ExtratoPeriodoResponse extratoOutubro = bancoHorasService.consultarExtrato(
				funcionario.getId(),
				LocalDate.of(2026, 10, 1),
				LocalDate.of(2026, 10, 31)
		);
		assertEquals(600, extratoOutubro.saldoFinalMinutos(), "Saldo congelado de outubro deve permanecer intacto em +10:00 (600m)");

		fechamentoBancoHorasRepository.deleteAll();
		movimentacaoBancoHorasRepository.deleteAll();
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T16 - Relatório de Equipe: Gestor visualiza apenas subordinados da sua equipe com saldos atualizados")
	void testT16_RelatorioSaldoEquipe() {
		List<Funcionario> todos = funcionarioRepository.findAll();
		Funcionario gestor = todos.stream()
				.filter(f -> f.getPerfilAcesso() == PerfilAcesso.GESTOR)
				.findFirst()
				.orElse(todos.get(0));

		Funcionario subordinado = todos.stream()
				.filter(f -> f.getGestor() != null && f.getGestor().getId().equals(gestor.getId()))
				.findFirst()
				.orElseGet(() -> {
					Funcionario f = todos.get(0);
					f.setGestor(gestor);
					return funcionarioRepository.saveAndFlush(f);
				});

		List<BancoHorasDTO.SaldoColaboradorEquipeResponse> relatorio = bancoHorasService.consultarSaldoEquipe(
				gestor,
				LocalDate.of(2026, 11, 1),
				LocalDate.of(2026, 11, 30)
		);

		assertNotNull(relatorio);
		assertTrue(relatorio.stream().anyMatch(r -> r.funcionarioId().equals(subordinado.getId())),
				"Subordinado direto deve constar no relatório de banco de horas da equipe do gestor");
	}

	@Test
	@DisplayName("T17 - Bloqueio Pós-Fechamento: Operações em mês fechado são recusadas (409 PeriodoFechadoException)")
	void testT17_BloqueioPosFechamento() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);
		UUID empresaId = funcionario.getEmpresa().getId();
		LocalDate dataOutubro = LocalDate.of(2026, 10, 15);

		// Garante que o período 2026-10 está fechado
		historicoFechamentoPeriodoRepository.deleteAll();
		fechamentoPeriodoRepository.deleteAll();
		fechamentoBancoHorasRepository.deleteAll();
		movimentacaoBancoHorasRepository.deleteAll();

		FechamentoPeriodo fechamento = new FechamentoPeriodo();
		fechamento.setEmpresa(funcionario.getEmpresa());
		fechamento.setMesReferencia("2026-10");
		fechamento.setDataInicio(LocalDate.of(2026, 10, 1));
		fechamento.setDataFim(LocalDate.of(2026, 10, 31));
		fechamento.setDataFechamento(LocalDateTime.now());
		fechamento.setStatus(StatusFechamentoPeriodo.CONCLUIDO);
		fechamento.setFechadoPor("RH Teste");
		fechamento.setPendenciasAbertasNoFechamento(0);
		fechamentoPeriodoRepository.saveAndFlush(fechamento);

		// 1. Tentar bater ponto na estação no período fechado
		assertThrows(PeriodoFechadoException.class, () ->
				batidaService.registrarBatida(new BatidaDTO.Request(
						funcionario.getId(),
						TipoRegistro.ENTRADA,
						null,
						null,
						null,
						"127.0.0.1",
						dataOutubro.atTime(8, 0)
				))
		);

		// 2. Tentar abrir solicitação de ajuste no período fechado
		assertThrows(PeriodoFechadoException.class, () ->
				solicitacaoAjusteService.abrirSolicitacao(funcionario, new SolicitacaoAjusteDTO.CriarRequest(
						dataOutubro,
						LocalTime.of(8, 0),
						TipoRegistro.ENTRADA,
						"Esqueci de bater ponto"
				))
		);

		// 3. Tentar lançar ocorrência no período fechado
		TipoOcorrencia tipo = tipoOcorrenciaRepository.findByEmpresaIdOrderByNomeAsc(empresaId).stream().findFirst().orElseGet(() -> {
			TipoOcorrencia t = new TipoOcorrencia();
			t.setEmpresa(funcionario.getEmpresa());
			t.setNome("Atestado T17");
			t.setTipoAbono(TipoAbonoOcorrencia.INTEGRAL);
			return tipoOcorrenciaRepository.saveAndFlush(t);
		});
		assertThrows(PeriodoFechadoException.class, () ->
				ocorrenciaColaboradorService.lancarOcorrencia(new OcorrenciaColaboradorDTO.CriarRequest(
						funcionario.getId(),
						tipo.getId(),
						dataOutubro,
						dataOutubro,
						null,
						"Atestado em mês fechado"
				), null)
		);

		// 4. Tentar registrar compensação de banco de horas no período fechado
		assertThrows(PeriodoFechadoException.class, () ->
				bancoHorasService.registrarCompensacao(new BancoHorasDTO.RegistrarCompensacaoRequest(
						funcionario.getId(),
						dataOutubro,
						60,
						"Compensação em mês fechado"
				))
		);

		// 5. Tentar tratar pendência do período fechado
		Inconsistencia pendencia = new Inconsistencia();
		pendencia.setEmpresa(funcionario.getEmpresa());
		pendencia.setFuncionario(funcionario);
		pendencia.setTipo(TipoInconsistencia.DIA_IMPAR);
		pendencia.setSituacao(StatusInconsistencia.ABERTA);
		pendencia.setDataReferencia(dataOutubro);
		pendencia.setDataHoraOrigem(dataOutubro.atTime(8, 0));
		pendencia.setChaveDeduplicacao("PEND_FECHADO_" + UUID.randomUUID());
		pendencia.setDescricaoOrigem("Pendência em período fechado");
		Inconsistencia salva = inconsistenciaRepository.saveAndFlush(pendencia);

		assertThrows(PeriodoFechadoException.class, () ->
				inconsistenciaService.tratar(salva.getId(), new InconsistenciaDTO.TratarRequest(
						AcaoTratamento.ABONAR_DIA,
						"Tentativa de tratar em mês fechado",
						null,
						null,
						null,
						null
				))
		);

		// Cleanup
		inconsistenciaRepository.delete(salva);
		historicoFechamentoPeriodoRepository.deleteAll();
		fechamentoPeriodoRepository.deleteAll();
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T17 - Importação Atrasada: Batida de mês fechado preserva data original e é alocada na competência seguinte aberta")
	void testT17_ImportacaoAtrasadaPeriodoFechado() {
		Equipamento equipamento = equipamentoRepository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("Nenhum equipamento encontrado."));
		Funcionario colaborador = funcionarioRepository.findByEmpresaId(equipamento.getEmpresa().getId()).stream()
				.findFirst()
				.orElseThrow();
		autenticarComoRh(colaborador);
		UUID empresaId = colaborador.getEmpresa().getId();

		historicoFechamentoPeriodoRepository.deleteAll();
		fechamentoPeriodoRepository.deleteAll();
		fechamentoBancoHorasRepository.deleteAll();
		inconsistenciaRepository.deleteAll();

		// Fecha o mês 2026-10
		FechamentoPeriodo fechamento = new FechamentoPeriodo();
		fechamento.setEmpresa(colaborador.getEmpresa());
		fechamento.setMesReferencia("2026-10");
		fechamento.setDataInicio(LocalDate.of(2026, 10, 1));
		fechamento.setDataFim(LocalDate.of(2026, 10, 31));
		fechamento.setDataFechamento(LocalDateTime.now());
		fechamento.setStatus(StatusFechamentoPeriodo.CONCLUIDO);
		fechamento.setFechadoPor("RH Teste");
		fechamento.setPendenciasAbertasNoFechamento(0);
		fechamentoPeriodoRepository.saveAndFlush(fechamento);

		// AFD contendo batida em 28/10/2026 às 14:00 (mês fechado)
		String conteudo = String.join("\n",
				"1000000000CABECALHO",
				"3000000999281020261400" + colaborador.getPisPasep(),
				"9000000001TRAILER"
		);
		MockMultipartFile arquivo = new MockMultipartFile(
				"arquivo", "afd_atrasado_outubro.txt", "text/plain", conteudo.getBytes(StandardCharsets.UTF_8)
		);

		ImportacaoDTO.HistoricoResponse hist = importacaoService.confirmarImportacao(arquivo, equipamento.getId());
		assertNotNull(hist);
		assertEquals(1, hist.totalValidas());

		// A batida foi salva preservando data/hora original 28/10/2026 14:00
		List<Batida> batidas = batidaRepository.findAtivasByFuncionarioIdAndDataHoraBetweenOrderByDataHoraAsc(
				colaborador.getId(),
				LocalDate.of(2026, 10, 28).atStartOfDay(),
				LocalDate.of(2026, 10, 28).atTime(LocalTime.MAX)
		);
		assertEquals(1, batidas.size(), "Batida deve ser persistida com data/hora original");
		assertEquals(LocalDateTime.of(2026, 10, 28, 14, 0), batidas.get(0).getDataHora());

		// Inconsistência MARCACAO_PERIODO_FECHADO deve ter sido gerada com alocação em 2026-11
		List<Inconsistencia> atrasadas = inconsistenciaRepository.findByEmpresaIdOrderByDataHoraOrigemDesc(empresaId).stream()
				.filter(i -> i.getTipo() == TipoInconsistencia.MARCACAO_PERIODO_FECHADO)
				.toList();

		assertFalse(atrasadas.isEmpty(), "Deve gerar inconsistência do tipo MARCACAO_PERIODO_FECHADO");
		Inconsistencia incAtrasada = atrasadas.get(0);
		assertEquals(LocalDate.of(2026, 10, 28), incAtrasada.getDataOcorrenciaOriginal());
		assertEquals("2026-11", incAtrasada.getCompetenciaAlocacao());
		assertEquals(LocalDate.of(2026, 11, 1), incAtrasada.getDataReferencia());
		assertEquals(StatusInconsistencia.ABERTA, incAtrasada.getSituacao());

		// Cleanup
		inconsistenciaRepository.deleteAll();
		historicoFechamentoPeriodoRepository.deleteAll();
		fechamentoPeriodoRepository.deleteAll();
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T17 - Fechamento com Pendências: Sem confirmação é bloqueado; com confirmação fecha como CONCLUIDO_COM_PENDENCIAS")
	void testT17_FechamentoComPendencias() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);
		UUID empresaId = funcionario.getEmpresa().getId();

		historicoFechamentoPeriodoRepository.deleteAll();
		fechamentoPeriodoRepository.deleteAll();
		fechamentoBancoHorasRepository.deleteAll();
		inconsistenciaRepository.deleteAll();

		// Cria uma pendência aberta em 2026-08-10
		Inconsistencia pendencia = new Inconsistencia();
		pendencia.setEmpresa(funcionario.getEmpresa());
		pendencia.setFuncionario(funcionario);
		pendencia.setTipo(TipoInconsistencia.DIA_IMPAR);
		pendencia.setSituacao(StatusInconsistencia.ABERTA);
		pendencia.setDataReferencia(LocalDate.of(2026, 8, 10));
		pendencia.setDataHoraOrigem(LocalDateTime.of(2026, 8, 10, 8, 0));
		pendencia.setChaveDeduplicacao("PEND_T17_AGOSTO_" + UUID.randomUUID());
		pendencia.setDescricaoOrigem("Batida ímpar em agosto");
		inconsistenciaRepository.saveAndFlush(pendencia);

		// 1. Prévia deve apontar 1 pendência aberta
		FechamentoPeriodoDTO.PreviaResponse previa = fechamentoPeriodoService.consultarPreviaFechamento(empresaId, "2026-08");
		assertEquals(1, previa.totalPendenciasAbertas());
		assertEquals(StatusFechamentoPeriodo.ABERTO, previa.statusAtual());

		// 2. Fechar sem confirmação explícita -> lança FechamentoComPendenciasException
		assertThrows(FechamentoComPendenciasException.class, () ->
				fechamentoPeriodoService.fecharPeriodo(empresaId, "2026-08", false, null)
		);

		// 3. Fechar com confirmação explícita -> CONCLUIDO_COM_PENDENCIAS
		FechamentoPeriodoDTO.FechamentoDetalheResponse fechado = fechamentoPeriodoService.fecharPeriodo(
				empresaId,
				"2026-08",
				true,
				"Fechamento com pendências autorizado pela diretoria"
		);

		assertEquals(StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS, fechado.status());
		assertEquals(1, fechado.quantidadePendenciasAbertas());
		assertTrue(fechado.ressalvaOuAviso().contains("Fechamento com pendências autorizado"));

		// Trilha de auditoria
		List<HistoricoFechamentoPeriodo> historico = historicoFechamentoPeriodoRepository.findByFechamentoPeriodoIdOrderByDataHoraDesc(fechado.id());
		assertFalse(historico.isEmpty());
		assertEquals(AcaoFechamentoPeriodo.FECHAMENTO_COM_PENDENCIAS, historico.get(0).getAcao());

		// Cleanup
		inconsistenciaRepository.deleteAll();
		historicoFechamentoPeriodoRepository.deleteAll();
		fechamentoPeriodoRepository.deleteAll();
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T17 - Reabertura Auditada: Exige motivo, volta status para ABERTO, descongela banco de horas e libera operações")
	void testT17_ReaberturaAuditada() {
		Funcionario funcionario = funcionarioRepository.findAll().stream().findFirst().orElseThrow();
		autenticarComoRh(funcionario);
		UUID empresaId = funcionario.getEmpresa().getId();

		historicoFechamentoPeriodoRepository.deleteAll();
		fechamentoPeriodoRepository.deleteAll();
		fechamentoBancoHorasRepository.deleteAll();
		movimentacaoBancoHorasRepository.deleteAll();

		// Fecha o período 2026-06 sem pendências
		FechamentoPeriodoDTO.FechamentoDetalheResponse fechado = fechamentoPeriodoService.fecharPeriodo(
				empresaId,
				"2026-06",
				false,
				"Fechamento regular de junho"
		);
		assertEquals(StatusFechamentoPeriodo.CONCLUIDO, fechado.status());
		assertTrue(competenciaLockService.estaCompetenciaFechada(empresaId, "2026-06"));

		// 1. Tentar reabrir sem motivo -> lança IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () ->
				fechamentoPeriodoService.reabrirPeriodo(empresaId, "2026-06", "   ")
		);

		// 2. Reabrir com motivo válido
		FechamentoPeriodoDTO.FechamentoDetalheResponse reaberto = fechamentoPeriodoService.reabrirPeriodo(
				empresaId,
				"2026-06",
				"Reabertura para inclusão de atestado médico retroativo"
		);

		assertEquals(StatusFechamentoPeriodo.ABERTO, reaberto.status());
		assertFalse(competenciaLockService.estaCompetenciaFechada(empresaId, "2026-06"));

		// 3. Auditoria gravou ação REABERTURA
		List<HistoricoFechamentoPeriodo> hist = historicoFechamentoPeriodoRepository.findByFechamentoPeriodoIdOrderByDataHoraDesc(fechado.id());
		assertTrue(hist.stream().anyMatch(h -> h.getAcao() == AcaoFechamentoPeriodo.REABERTURA
				&& h.getMotivo().contains("atestado médico retroativo")));

		// 4. Operações voltam a ser permitidas na competência reaberta
		BatidaDTO.ComprovanteResponse comprovante = batidaService.registrarBatida(new BatidaDTO.Request(
				funcionario.getId(),
				TipoRegistro.ENTRADA,
				null,
				null,
				null,
				"127.0.0.1",
				LocalDateTime.of(2026, 6, 15, 8, 0)
		));
		assertNotNull(comprovante);
		assertNotNull(comprovante.nsr());

		// Cleanup
		historicoFechamentoPeriodoRepository.deleteAll();
		fechamentoPeriodoRepository.deleteAll();
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T18 - Espelho de Ponto Completo: Dia a dia com previsto, origens (Estação/Ajuste), totais, fechamento com data e HTML legível")
	void testT18_EspelhoDiaADiaOrigensFechamentoEImpressao() {
		Funcionario colaborador = funcionarioRepository.findAll().stream()
				.filter(f -> f.getPerfilAcesso() == PerfilAcesso.COLABORADOR)
				.findFirst()
				.orElse(funcionarioRepository.findAll().get(0));
		autenticarComoRh(colaborador);
		UUID empresaId = colaborador.getEmpresa().getId();

		historicoFechamentoPeriodoRepository.deleteAll();
		fechamentoPeriodoRepository.deleteAll();
		fechamentoBancoHorasRepository.deleteAll();
		movimentacaoBancoHorasRepository.deleteAll();
		inconsistenciaRepository.deleteAll();

		LocalDate diaTrabalho = LocalDate.of(2026, 5, 11); // Segunda-feira
		apuracaoDiariaRepository.findByFuncionarioIdAndDataReferencia(colaborador.getId(), diaTrabalho)
				.ifPresent(apuracaoDiariaRepository::delete);

		// 1. Batida pela Estação (08:00)
		Batida bEstacao = new Batida();
		bEstacao.setFuncionario(colaborador);
		bEstacao.setDataHora(diaTrabalho.atTime(8, 0));
		bEstacao.setTipoRegistro(TipoRegistro.ENTRADA);
		bEstacao.setTipoOrigem(TipoOrigem.ESTACAO);
		bEstacao.setStatusBatida(StatusBatida.NORMAL);
		bEstacao.setNsr(batidaRepository.proximoNsrSequence());
		batidaRepository.saveAndFlush(bEstacao);

		// 2. Batida de saída ajustada manualmente (18:00 -> +1h extra assumindo intervalo ou jornada)
		Batida bAjuste = new Batida();
		bAjuste.setFuncionario(colaborador);
		bAjuste.setDataHora(diaTrabalho.atTime(18, 0));
		bAjuste.setTipoRegistro(TipoRegistro.SAIDA);
		bAjuste.setTipoOrigem(TipoOrigem.AJUSTE_MANUAL);
		bAjuste.setStatusBatida(StatusBatida.NORMAL);
		bAjuste.setMotivoAjuste("Esquecimento na saída");
		bAjuste.setAutorAjuste("Gestor Teste");
		bAjuste.setNsr(batidaRepository.proximoNsrSequence());
		batidaRepository.saveAndFlush(bAjuste);

		apuracaoDiariaService.apurarDia(colaborador, diaTrabalho);

		// 3. Fecha a competência 2026-05 para verificar indicação da data de fechamento
		FechamentoPeriodoDTO.FechamentoDetalheResponse fechado = fechamentoPeriodoService.fecharPeriodo(
				empresaId, "2026-05", true, "Fechamento para conferência do espelho T18"
		);

		// 4. Consulta espelho mensal
		EspelhoPontoDTO.EspelhoMensalResponse espelho = espelhoService.consultarEspelhoMensal(colaborador.getId(), "2026-05");
		assertNotNull(espelho);
		assertEquals("2026-05", espelho.competencia());
		assertEquals(31, espelho.dias().size());
		assertTrue(espelho.fechamento().periodoFechado());
		assertNotNull(espelho.fechamento().dataFechamento());
		assertEquals(fechado.fechadoEm().withNano(0), espelho.fechamento().dataFechamento().withNano(0));

		EspelhoPontoDTO.DiaEspelhoResponse dia11 = espelho.dias().get(10);
		assertEquals(diaTrabalho, dia11.data());
		assertEquals(2, dia11.marcacoes().size());
		assertEquals("Estação", dia11.marcacoes().get(0).origemLabel());
		assertEquals("Ajuste", dia11.marcacoes().get(1).origemLabel());
		assertTrue(espelho.totais().totalMinutosTrabalhados() > 0);

		// 5. Impressão em página HTML legível
		String html = espelhoService.gerarHtmlImpressao(colaborador.getId(), "2026-05");
		assertTrue(html.contains("Espelho de Ponto Eletrônico"));
		assertTrue(html.contains(colaborador.getNomeCompleto()));
		assertTrue(html.contains("PERÍODO FECHADO em"));
		assertTrue(html.contains("Estação"));
		assertTrue(html.contains("Ajuste"));

		// Cleanup
		historicoFechamentoPeriodoRepository.deleteAll();
		fechamentoPeriodoRepository.deleteAll();
		fechamentoBancoHorasRepository.deleteAll();
		movimentacaoBancoHorasRepository.deleteAll();
		apuracaoDiariaRepository.findByFuncionarioIdAndDataReferencia(colaborador.getId(), diaTrabalho)
				.ifPresent(apuracaoDiariaRepository::delete);
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("T18 - Segurança de Perfis: Colaborador só vê o próprio espelho; Gestor vê equipe; RH vê toda empresa")
	void testT18_ControleAcessoPerfis() {
		List<Funcionario> todos = funcionarioRepository.findAll();
		Funcionario rh = todos.stream().filter(f -> f.getPerfilAcesso() == PerfilAcesso.RH).findFirst().orElseThrow();
		Funcionario gestor = todos.stream().filter(f -> f.getPerfilAcesso() == PerfilAcesso.GESTOR).findFirst().orElseThrow();
		Funcionario subordinado = todos.stream()
				.filter(f -> f.getPerfilAcesso() == PerfilAcesso.COLABORADOR && f.getGestor() != null && f.getGestor().getId().equals(gestor.getId()))
				.findFirst()
				.orElseGet(() -> {
					Funcionario c = todos.stream().filter(f -> f.getPerfilAcesso() == PerfilAcesso.COLABORADOR).findFirst().orElseThrow();
					c.setGestor(gestor);
					return funcionarioRepository.saveAndFlush(c);
				});

		// 1. Colaborador vê o próprio espelho, mas é bloqueado ao tentar ver o do gestor
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				new FuncionarioUserDetails(subordinado), null, List.of(new SimpleGrantedAuthority("ROLE_COLABORADOR"))
		));
		assertNotNull(espelhoService.consultarEspelhoMensal(null, "2026-11"));
		assertThrows(AcessoNegadoException.class, () ->
				espelhoService.consultarEspelhoMensal(gestor.getId(), "2026-11")
		);

		// 2. Gestor vê espelho do subordinado, mas é bloqueado ao tentar ver o do RH (não subordinado)
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				new FuncionarioUserDetails(gestor), null, List.of(new SimpleGrantedAuthority("ROLE_GESTOR"))
		));
		assertNotNull(espelhoService.consultarEspelhoMensal(subordinado.getId(), "2026-11"));
		assertThrows(AcessoNegadoException.class, () ->
				espelhoService.consultarEspelhoMensal(rh.getId(), "2026-11")
		);

		// 3. RH vê espelho de qualquer colaborador da empresa
		autenticarComoRh(rh);
		assertNotNull(espelhoService.consultarEspelhoMensal(subordinado.getId(), "2026-11"));
		assertNotNull(espelhoService.consultarEspelhoMensal(gestor.getId(), "2026-11"));

		SecurityContextHolder.clearContext();
	}
}