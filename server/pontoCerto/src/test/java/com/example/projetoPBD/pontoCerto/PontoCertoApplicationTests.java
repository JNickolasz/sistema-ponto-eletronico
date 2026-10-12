package com.example.projetoPBD.pontoCerto;

import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegistro;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BatidaDTO;
import com.example.projetoPBD.pontoCerto.repository.BatidaRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.service.BatidaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
}