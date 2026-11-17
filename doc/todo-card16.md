# TODO: T16 · Banco de Horas e Extrato de Compensação

## 1. Banco de Dados & Modelagem (PostgreSQL / JPA)
- [x] **Criar Enum `TipoMovimentacaoBancoHoras`:**
    - `CREDITO_APURACAO` (derivado automaticamente das horas excedentes/extras do dia)
    - `DEBITO_APURACAO` (derivado automaticamente de atrasos, saídas antecipadas ou faltas não abonadas)
    - `COMPENSACAO` (abatimento registrado de folga/saída antecipada compensada)
    - `FECHAMENTO_PERIODO` (snapshot imutável de saldo consolidado no encerramento da competência)
- [x] **Criar Entidade `MovimentacaoBancoHoras`:**
    - `id` (Long / UUID)
    - `funcionario` (`@ManyToOne Funcionario`, obrigatório)
    - `dataReferencia` (LocalDate, obrigatório)
    - `tipo` (`TipoMovimentacaoBancoHoras`, obrigatório)
    - `minutos` (Integer, obrigatório — positivo para crédito, negativo para débito/compensação)
    - `apuracaoDiariaId` (UUID, nullable — preenchido quando derivado da `ApuracaoDiaria`, garantindo vínculo único `uk_bh_apuracao_tipo` para evitar duplicidade ao recalcular)
    - `descricao` (String, ex.: "Crédito diário apurado", "Débito por atraso", "Compensação de folga - Motivo: ...")
    - `periodoFechado` (Boolean, default `false` — marca registros congelados pelo fechamento de competência)
    - **Auditoria (para compensações e fechamentos):**
        - `registradoPor` (String, nullable)
        - `registradoEm` (LocalDateTime)
- [x] **Criar Entidade `FechamentoBancoHoras` (Snapshot de Imutabilidade):**
    - `id` (Long / UUID)
    - `funcionario` (`@ManyToOne Funcionario`, obrigatório)
    - `mesReferencia` (String `YYYY-MM`, restrição única composta `uk_bh_fechamento_func_mes`)
    - `saldoAnteriorMinutos` (Integer)
    - `creditosPeriodoMinutos` (Integer)
    - `debitosPeriodoMinutos` (Integer)
    - `compensacoesPeriodoMinutos` (Integer)
    - `saldoFinalCongeladoMinutos` (Integer — valor travado no ato do fechamento, imune a alterações posteriores de regras)
    - `fechadoEm` (LocalDateTime)

---

## 2. Backend — Cálculo Derivado, Extrato e Imutabilidade (Spring Boot)
- [x] **Derivação Automática a partir da Apuração (`BancoHorasService` + Hook na `ApuracaoDiariaService`):**
    - [x] Bloquear qualquer endpoint ou método que tente inserir crédito/débito manual livre (apenas o motor de apuração gera `CREDITO_APURACAO` e `DEBITO_APURACAO`).
    - [x] Ao concluir a apuração de um dia (`status == APURADO` ou `FALTA`):
        - Verificar se a `dataReferencia` pertence a um período já fechado. Se fechado, **não alterar** movimentações congeladas.
        - Calcular crédito líquido do dia (excedente elegível para banco conforme regra vigente na data) e débito líquido (`minutosAtraso + minutosSaidaAntecipada + minutosFalta`).
        - Atualizar ou substituir de forma idempotente as movimentações `CREDITO_APURACAO` / `DEBITO_APURACAO` vinculadas àquela `apuracaoDiariaId` (removendo-as caso o recálculo zere o saldo do dia ou o dia volte para `PENDENTE_INCONSISTENCIA`).
- [x] **Registro de Compensação (`registrarCompensacao`):**
    - [x] Validar que a data da compensação não pertence a período fechado.
    - [x] Validar motivo obrigatório e quantidade de minutos a compensar (`> 0`).
    - [x] Criar movimentação do tipo `COMPENSACAO` com valor negativo (`-minutos`), abatendo do saldo acumulado e ficando visível no extrato dia a dia.
- [x] **Consulta de Extrato por Período (`consultarExtrato`):**
    - [x] Receber `funcionarioId`, `dataInicio` e `dataFim`.
    - [x] **Saldo Inicial:** Somar todas as movimentações anteriores a `dataInicio` (respeitando snapshots congelados de períodos fechados).
    - [x] **Movimentações do Período:** Listar cronologicamente dia a dia os créditos, débitos e compensações dentro de `[dataInicio, dataFim]`, calculando o saldo progressivo linha a linha.
    - [x] **Saldo Final:** `saldoInicial + somaMovimentacoesNoPeriodo`.
- [x] **Congelamento no Fechamento de Período (Integração com `FechamentoPeriodo`):**
    - [x] Ao fechar uma competência (`YYYY-MM`), marcar todas as movimentações do mês como `periodoFechado = true` e gravar o `FechamentoBancoHoras` com o `saldoFinalCongeladoMinutos`.
    - [x] Garantir que alterações futuras nas regras de jornada, tolerância ou multiplicadores de hora extra nunca recalculam ou alteram saldos de meses já fechados.
- [x] **Relatório Consolidado por Equipe (`consultarSaldoEquipe`):**
    - [x] Filtrar colaboradores subordinados ao gestor autenticado (ou departamento/equipe).
    - [x] Retornar por colaborador: saldo anterior, créditos no período, débitos no período, compensações e saldo acumulado atual.
- [x] **Endpoints (`BancoHorasController`):**
    - [x] `GET /api/banco-horas/extrato`: Extrato individual com `funcionarioId`, `dataInicio`, `dataFim`, retornando `saldoInicial`, lista diária de eventos com saldo progressivo e `saldoFinal`.
    - [x] `POST /api/banco-horas/compensacoes`: Registra compensação de horas (abatimento com motivo).
    - [x] `GET /api/banco-horas/equipe`: Relatório consolidado de saldos da equipe para o gestor.

---

## 3. Frontend (React)
- [x] **Tela de Extrato de Banco de Horas (Colaborador / RH):**
    - [x] Filtro de período (`dataInicio` e `dataFim`) e seletor de colaborador (para RH/Gestor).
    - [x] Cards resumo no topo: **Saldo Inicial**, **Créditos (+)**, **Débitos (-)**, **Compensações (-)** e **Saldo Final**.
    - [x] Tabela de extrato dia a dia exibindo: Data, Tipo de Evento (Crédito Apuração, Débito Apuração, Compensação), Descrição/Origem, Movimentação (`+01:30` / `-00:45`) e Saldo Acumulado após o evento.
    - [x] Indicação visual (cadeado/tag) nos períodos já fechados e congelados.
- [x] **Modal de Registro de Compensação (RH / Gestor):**
    - [x] Formulário com Colaborador, Data da Compensação, Horas/Minutos abatidos e Motivo obrigatório (bloqueando datas de períodos fechados).
- [x] **Visão de Banco de Horas da Equipe (Painel do Gestor):**
    - [x] Tabela consolidada listando cada membro da equipe, saldo anterior, movimentações do período e saldo acumulado atual, com atalho para abrir o extrato detalhado do colaborador.

---

## 4. Testes e Validações (QA)
- [x] **Teste de Derivação Automática:** Apurar dia com 1h extra e outro dia com 30m de atraso -> extrato deve refletir `+01:00` e `-00:30` com saldo acumulado de `+00:30` sem intervenção manual.
- [x] **Teste de Extrato com Saldo Inicial e Final:** Filtrar período intermediário (ex.: dia 10 a dia 20) e validar se o saldo inicial reflete exatamente o acumulado até o dia 09 e o saldo final bate com o acumulado até o dia 20.
- [x] **Teste de Compensação:** Registrar compensação de `02:00` com motivo -> verificar abatimento imediato no saldo acumulado e exibição da linha no extrato.
- [x] **Teste de Imutabilidade de Período Fechado:** Fechar o mês de outubro com saldo `+10:00`, alterar a regra da jornada/tolerância no cadastro e forçar recálculo -> saldo congelado de outubro deve permanecer intacto em `+10:00`.
- [x] **Teste de Relatório de Equipe:** Consultar endpoint de equipe como gestor e validar retorno apenas dos membros da sua equipe com os respectivos saldos atualizados.