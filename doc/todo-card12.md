# TODO - Issue T12: Fila de Inconsistências e Tratamento

**História de Usuário:**
> Como RH, eu quero ver numa lista tudo o que não fecha para resolver antes do fechamento, em vez de descobrir na reclamação.
> Toda vez que algo não fecha, entra uma pendência na fila, com tipo, situação e o que a originou. Cada uma se resolve com uma ação registrada — vincular, incluir marcação de ajuste, descartar a duplicada ou abonar o dia. Nada é resolvido em silêncio: fica autor, data e motivo, e a pendência resolvida continua consultável.

---

## ✅ 1. Banco de Dados & Ajustes de Schema (`DatabaseSchemaAdjuster.java`) — [CONCLUÍDO]

- [x] **Tabelas mapeadas via JPA (`ddl-auto=update`)**: `inconsistencia` e `fechamento_periodo`.
- [x] **Ajuste em `config/DatabaseSchemaAdjuster.java`**:
  - Adicionado `jdbcTemplate.execute("ALTER TABLE batida DROP CONSTRAINT IF EXISTS batida_status_batida_check");` para permitir o novo status `DESCARTADA`.
- [x] **Proteção de Foreign Keys para testes existentes**:
  - `batidaOrigemId`, `batidaSecundariaId` e `arquivoImportadoId` mapeados como `UUID` simples para não travar `batidaRepository.deleteAll()` e `historicoImportacaoRepository.deleteAll()` nos testes das tarefas anteriores.

---

## ✅ 2. Backend — Modelagem de Domínio e Enums — [CONCLUÍDO]

### 2.1. Enums (`domain/enums/`)
- [x] `domain/enums/TipoInconsistencia.java` (`ORFA`, `DIA_IMPAR`, `DUPLICADA_MINUTO`, `FORA_DA_JORNADA`, `SEQUENCIA_INVALIDA`, `EQUIPAMENTO_DESCONHECIDO`)
- [x] `domain/enums/StatusInconsistencia.java` (`ABERTA`, `RESOLVIDA`)
- [x] `domain/enums/AcaoTratamento.java` (`VINCULAR`, `INCLUIR_MARCACAO_AJUSTE`, `DESCARTAR_DUPLICADA`, `ABONAR_DIA`)
- [x] `domain/enums/StatusBatida.java` (adicionado `DESCARTADA`)
- [x] `domain/enums/StatusFechamentoPeriodo.java` (`EM_ANDAMENTO`, `CONCLUIDO`, `CONCLUIDO_COM_PENDENCIAS`)

### 2.2. Entidades (`domain/`)
- [x] `domain/Inconsistencia.java`
- [x] `domain/FechamentoPeriodo.java`

---

## ✅ 3. Backend — Repositories, DTOs, Exceções e Motor de Detecção Automática (Critério 1) — [CONCLUÍDO]

### ✅ 3.1. Arquivos de Repository (`repository/`) — [CONCLUÍDO]
- [x] **[CRIAR] `repository/InconsistenciaRepository.java`**
  - `boolean existsByEmpresaIdAndChaveDeduplicacaoAndSituacao(UUID empresaId, String chaveDeduplicacao, StatusInconsistencia situacao)`
  - `boolean existsByEmpresaIdAndChaveDeduplicacao(UUID empresaId, String chaveDeduplicacao)`
  - `Optional<Inconsistencia> findFirstByBatidaOrigemIdAndTipoAndSituacao(UUID batidaOrigemId, TipoInconsistencia tipo, StatusInconsistencia situacao)`
  - `List<Inconsistencia> findByEmpresaIdAndFuncionarioIdAndDataReferenciaAndTipoAndSituacao(UUID empresaId, UUID funcionarioId, LocalDate dataReferencia, TipoInconsistencia tipo, StatusInconsistencia situacao)`
  - `List<Inconsistencia> findByEmpresaIdOrderByDataHoraOrigemDesc(UUID empresaId)` (com `LEFT JOIN FETCH i.funcionario` e `LEFT JOIN FETCH i.equipamento`)
  - `List<Inconsistencia> findByEmpresaIdAndSituacaoAndDataReferenciaBetweenOrderByDataHoraOrigemDesc(UUID empresaId, StatusInconsistencia situacao, LocalDate dataInicio, LocalDate dataFim)`
  - `long countByEmpresaIdAndSituacao(UUID empresaId, StatusInconsistencia situacao)`
  - `long countByEmpresaIdAndSituacaoAndTipo(UUID empresaId, StatusInconsistencia situacao, TipoInconsistencia tipo)`

- [x] **[CRIAR] `repository/FechamentoPeriodoRepository.java`**
  - `List<FechamentoPeriodo> findByEmpresaIdOrderByDataFechamentoDesc(UUID empresaId)`
  - `Optional<FechamentoPeriodo> findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(UUID empresaId, String mesReferencia)`

- [x] **[EDITAR] `repository/BatidaRepository.java`**
  - Adicionada query `findAtivasByFuncionarioIdAndDataHoraBetweenOrderByDataHoraAsc(UUID funcionarioId, LocalDateTime inicio, LocalDateTime fim)` e filtro de `StatusBatida.DESCARTADA` em `findByFuncionarioIdOrderByDataHoraDesc`.

### ✅ 3.2. Arquivos de DTO e Exceções (`dto/` e `service/exceptions/`) — [CONCLUÍDO]
- [x] **[CRIAR] `dto/domaindtos/InconsistenciaDTO.java`**
  - `record Response(...)`: Dados completos da inconsistência (id, tipo, situacao, dataReferencia, dataHoraOrigem, descricaoOrigem, id/nome/matrícula do funcionário, id/identificação/código do equipamento, identificadorRecebido, nomeArquivo, batidaOrigemId, batidaSecundariaId, acaoTomada, tratadoPor, tratadoEm, motivoTratamento, detalheResolucao).
  - `record TratarRequest(...)`:
    - `@NotNull AcaoTratamento acao`
    - `@NotBlank String motivo`
    - `UUID funcionarioId` (usado quando `acao == VINCULAR` para batida órfã)
    - `UUID equipamentoId` (usado quando `acao == VINCULAR` para equipamento desconhecido)
    - `LocalTime horarioAjuste` e `TipoRegistro tipoRegistroAjuste` (usados quando `acao == INCLUIR_MARCACAO_AJUSTE`)
  - `record PainelContagemResponse(...)`:
    - `long totalAbertas`
    - `long totalResolvidas`
    - `Map<TipoInconsistencia, Long> abertasPorTipo`
  - `record FechamentoRequest(...)`:
    - `@NotBlank String mesReferencia` (formato `YYYY-MM`)
    - `LocalDate dataInicio` (opcional; se nulo, assume dia 1 do `mesReferencia`)
    - `LocalDate dataFim` (opcional; se nulo, assume último dia do `mesReferencia`)
    - `Boolean confirmarComPendencias` (default `false`)
  - `record FechamentoResponse(...)`:
    - Dados do `FechamentoPeriodo` salvo (`id`, `mesReferencia`, `dataInicio`, `dataFim`, `dataFechamento`, `fechadoPor`, `status`, `pendenciasAbertasNoFechamento`, `avisoPendencias`, `fechadoComRessalva`).

- [x] **[EDITAR] `dto/domaindtos/BatidaDTO.java`**
  - Em `BatidaDTO.Request`, adicionar sobrecarga opcional `LocalDateTime dataHoraOpcional` mantendo o construtor de 6 parâmetros original intacto (para os testes existentes continuarem compilando sem alterações e os novos testes da T12 poderem simular batidas em datas/horários específicos).

- [x] **[CRIAR] `service/exceptions/FechamentoComPendenciasException.java`**
  - Exceção lançada quando o RH tenta fechar o período com pendências abertas na fila sem marcar `confirmarComPendencias = true`.

- [x] **[EDITAR] `service/exceptions/GlobalExceptionHandler.java`**
  - Adicionar `@ExceptionHandler(FechamentoComPendenciasException.class)` retornando `HttpStatus.CONFLICT` (409) com a mensagem detalhada do que impediu o fechamento.

### ✅ 3.3. Motor de Detecção Automática (`service/`) — [CONCLUÍDO]
- [x] **[CRIAR] `service/InconsistenciaRegistroAutonomoService.java`**
  - Componente auxiliar com `@Transactional(propagation = Propagation.REQUIRES_NEW)` para gravar pendências que precisam sobreviver mesmo quando a transação chamadora sofre rollback (especificamente `EQUIPAMENTO_DESCONHECIDO` quando `ImportacaoService` lança `EquipamentoNaoEncontradoException`).

- [x] **[CRIAR] `service/InconsistenciaService.java` (Parte 1 — Métodos de Detecção)**
  - `registrarInconsistenciaOrfa(Batida batidaOrfa, String nomeArquivo)` -> Cria pendência `ORFA`.
  - `registrarEquipamentoDesconhecido(UUID empresaId, String nomeArquivo, String identificadorEquipamento)` -> Delega ao `InconsistenciaRegistroAutonomoService` para criar pendência `EQUIPAMENTO_DESCONHECIDO`.
  - `registrarDuplicadaNoMinuto(Funcionario func, LocalDateTime dataHora, Long nsr, Equipamento eq, String nomeArquivo, UUID batidaOrigemId, UUID batidaSecundariaId)` -> Cria pendência `DUPLICADA_MINUTO`.
  - `analisarDiaColaborador(Funcionario funcionario, LocalDate data)` -> Avalia todas as batidas ativas do colaborador na data e detecta automaticamente:
    1. `DUPLICADA_MINUTO` (duas batidas ativas no mesmo minuto)
    2. `SEQUENCIA_INVALIDA` (primeira batida do dia sendo `SAIDA`, ou duas batidas seguidas do mesmo tipo em minutos distintos)
    3. `DIA_IMPAR` (quantidade ímpar de batidas no dia com `ENTRADA` sem `SAIDA` correspondente; se o dia passar a ficar par naturalmente antes de tratamento do RH, remove a pendência automática aberta; se já foi `RESOLVIDA`, preserva no histórico)
    4. `FORA_DA_JORNADA` (batida em dia de `Feriado` ativo ou dia em que `RegimeTrabalhoService.obterHorarioPrevisto` indica `diaTrabalho == false`)

- [x] **[EDITAR] `service/ImportacaoService.java` (Conectar gatilhos automáticos)**
  - Em `buscarEValidarEquipamento`: antes de lançar `EquipamentoNaoEncontradoException`, registrar `EQUIPAMENTO_DESCONHECIDO`.
  - Em `confirmarImportacao`:
    - Quando gravar `batidaOrfa` (`ORFA_PENDENTE`), acionar `inconsistenciaService.registrarInconsistenciaOrfa(...)`.
    - Quando encontrar batida no mesmo minuto para o mesmo colaborador com NSR diferente, acionar `inconsistenciaService.registrarDuplicadaNoMinuto(...)`.
    - Alternar `TipoRegistro.ENTRADA` e `TipoRegistro.SAIDA` nas batidas válidas importadas por colaborador/dia.
    - Ao final do loop de importação, chamar `inconsistenciaService.analisarDiaColaborador(func, data)` para cada par `(colaborador, dia)` tocado pelo arquivo (detectando `DIA_IMPAR` e `FORA_DA_JORNADA`).

- [x] **[EDITAR] `service/BatidaService.java` (Conectar gatilhos automáticos)**
  - Em `registrarBatida`: após salvar a batida, chamar `inconsistenciaService.analisarDiaColaborador(funcionario, batida.getDataHora().toLocalDate())` (protegido contra falhas de concorrência para não afetar o teste de threads simultâneas da T08).
  - Em `vincularOrfa`: ao vincular uma batida órfã pelo fluxo da T10, atualizar também a `Inconsistencia` correspondente do tipo `ORFA` para `RESOLVIDA` com `acaoTomada = VINCULAR`, autor, data e motivo.

---

## ✅ 4. Backend — Resolução Auditável, Filtros, Painel, Fechamento e Controller (Critérios 2, 3, 4, 5 e 6) — [CONCLUÍDO]

### ✅ 4.1. Métodos de Negócio em `service/InconsistenciaService.java` (Parte 2) — [CONCLUÍDO]
- [x] **`listar(TipoInconsistencia tipo, String situacaoStr, UUID funcionarioId, LocalDate dataInicio, LocalDate dataFim, String mesReferencia)`**
  - Filtra por empresa logada e aplica todos os filtros combinados (`tipo`, `situacao` aceitando `ABERTA`/`PENDENTE`/`RESOLVIDA`, `funcionarioId` e período por `dataInicio`/`dataFim` ou `mesReferencia`).
- [x] **`tratar(UUID inconsistenciaId, InconsistenciaDTO.TratarRequest dto)`**
  - Valida que a pendência está `ABERTA` e que `acao` e `motivo` foram preenchidos.
  - Executa a regra específica de cada `AcaoTratamento`:
    - `VINCULAR`: vincula colaborador (e atualiza a `Batida` órfã para `VINCULADA`) ou vincula equipamento.
    - `INCLUIR_MARCACAO_AJUSTE`: cria e salva nova `Batida` (`AJUSTE_MANUAL`, `NORMAL`, com `nsr` da sequence) no horário indicado para fechar o par do dia.
    - `DESCARTAR_DUPLICADA`: marca a batida duplicada/indevida como `StatusBatida.DESCARTADA` mantendo-a no banco para auditoria.
    - `ABONAR_DIA`: registra o abono do dia de referência.
  - Grava `situacao = RESOLVIDA`, `acaoTomada`, `tratadoPor`, `tratadoPorId`, `tratadoEm = LocalDateTime.now()`, `motivoTratamento` e `detalheResolucao`.
- [x] **`obterPainelContagem(LocalDate dataInicio, LocalDate dataFim, String mesReferencia)`**
  - Retorna `PainelContagemResponse` com `totalAbertas`, `totalResolvidas` e o mapa `abertasPorTipo` preenchido para todos os 6 tipos do enum `TipoInconsistencia`.
- [x] **`fecharPeriodo(InconsistenciaDTO.FechamentoRequest dto)`** e **`listarFechamentos()`**
  - Busca pendências `ABERTA` no período informado:
    - Se houver pendências abertas e `!Boolean.TRUE.equals(dto.confirmarComPendencias())` -> lança `FechamentoComPendenciasException` (bloqueia o fechamento).
    - Se houver pendências abertas e `Boolean.TRUE.equals(dto.confirmarComPendencias())` -> grava `FechamentoPeriodo` com `status = CONCLUIDO_COM_PENDENCIAS`, `pendenciasAbertasNoFechamento = qtd` e `avisoPendencias` detalhando o que ficou aberto por tipo.
    - Se não houver pendências abertas -> grava `FechamentoPeriodo` com `status = CONCLUIDO` e `pendenciasAbertasNoFechamento = 0`.

### ✅ 4.2. Controller REST (`resource/`) — [CONCLUÍDO]
- [x] **[CRIAR] `resource/InconsistenciaController.java`** (`@RequestMapping({"/api/v1/inconsistencias", "/api/inconsistencias"})`, `@PreAuthorize("hasRole('RH')")`)
  - `GET /` -> `listar(...)` (filtros: `tipo`, `situacao`, `funcionarioId`, `dataInicio`, `dataFim`, `mesReferencia`)
  - `GET /painel` -> `obterPainelContagem(...)`
  - `POST /{id}/tratar` -> `tratar(id, dto)`
  - `POST /fechamento` -> `fecharPeriodo(dto)`
  - `GET /fechamentos` -> `listarFechamentos()`

---

## ✅ 5. Frontend (React) — [CONCLUÍDO]

- [x] **[CRIAR] `client/src/services/inconsistenciaService.js`**
  - Funções: `listarInconsistencias(filtros)`, `obterPainel(filtros)`, `tratarInconsistencia(id, payload)`, `fecharPeriodo(payload)`, `listarFechamentos()`.
- [x] **[CRIAR] `client/src/components/FilaInconsistenciasSection.jsx`**
  - Painel superior com os 6 cards de contagem por tipo + total abertas/resolvidas.
  - Barra de filtros (Tipo, Situação: Fila Aberta vs Histórico de Resolvidas, Colaborador, Período/Mês).
  - Tabela da fila com detalhes do que originou e auditoria completa nas resolvidas.
  - Modal de tratamento com as 4 ações (`VINCULAR`, `INCLUIR_MARCACAO_AJUSTE`, `DESCARTAR_DUPLICADA`, `ABONAR_DIA`) e motivo obrigatório.
  - Bloco/Modal de Fechamento de Período com bloqueio automático e opção de fechar com registro do aviso do que ficou aberto.
- [x] **[EDITAR] `client/src/pages/RhDashboard.jsx`**
  - Adicionar aba **"Inconsistências & Fechamento (T12)"** integrando `FilaInconsistenciasSection`.

---

## ✅ 6. Testes Automatizados (`PontoCertoApplicationTests.java`) — [CONCLUÍDO]

- [x] **[EDITAR] `server/pontoCerto/src/test/java/com/example/projetoPBD/pontoCerto/PontoCertoApplicationTests.java`**
  - Teste 1: Detecção automática dos 6 tipos (`ORFA`, `DIA_IMPAR`, `DUPLICADA_MINUTO`, `FORA_DA_JORNADA`, `SEQUENCIA_INVALIDA`, `EQUIPAMENTO_DESCONHECIDO`).
  - Teste 2: Filtros da fila (por tipo, situação, colaborador e período) e painel de contagem por tipo.
  - Teste 3: Tratamento com cada ação (`VINCULAR`, `INCLUIR_MARCACAO_AJUSTE`, `DESCARTAR_DUPLICADA`, `ABONAR_DIA`), validação de motivo obrigatório, saída da fila aberta e permanência no histórico de resolvidas.
  - Teste 4: Fechamento de período bloqueando quando há pendência aberta e permitindo fechar com aviso registrado quando confirmado.

---

## ✅ Checklist de Critérios de Aceite (T12)
- [x] A pendência é criada sozinha quando o sistema detecta cada um dos 6 tipos previstos (`ORFA`, `DIA_IMPAR`, `DUPLICADA_MINUTO`, `FORA_DA_JORNADA`, `SEQUENCIA_INVALIDA`, `EQUIPAMENTO_DESCONHECIDO`).
- [x] Fila com filtro funcional por tipo, situação (`ABERTA`/`RESOLVIDA`), colaborador e período.
- [x] Cada tratamento exige e grava autor, data/hora, ação tomada (`VINCULAR`, `INCLUIR_MARCACAO_AJUSTE`, `DESCARTAR_DUPLICADA`, `ABONAR_DIA`) e motivo.
- [x] Pendência resolvida sai da fila aberta, mas continua consultável no histórico com todos os dados de auditoria.
- [x] O período não fecha com pendência aberta, ou fecha registrando o aviso explícito do que ficou pendente.
- [x] Painel exibe a contagem exata de pendências abertas por cada um dos 6 tipos.
