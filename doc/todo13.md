# TODO: T13 · Solicitação de Ajuste e Análise do Gestor

## 1. Banco de Dados & Modelagem (PostgreSQL / JPA)
- [x] **Ajuste na Entidade `Batida`:**
    - [x] Garantir enum `TipoOrigem`: manter compatibilidade com `AJUSTE_MANUAL` (já utilizado no T12).
    - [x] Adicionar campos de auditoria de ajuste na `Batida`:
        - `solicitacaoAjusteId` / `pedidoAjusteId` (UUID opcional para rastreabilidade)
        - `autorAjuste` (nome/identificação do gestor que aprovou)
        - `motivoAjuste` (justificativa do ajuste registrada)
    - [x] Blindagem de Imutabilidade no JPA/Hibernate:
        - `@PreRemove`: lançar exceção se tentar remover batidas cuja origem seja `ESTACAO` ou `ARQUIVO_IMPORTADO` (Portaria 671).
- [x] **Criar Enum `StatusSolicitacaoAjuste`:**
    - `PENDENTE`, `DEFERIDO`, `INDEFERIDO`
- [x] **Criar Entidade `SolicitacaoAjuste`:**
    - `id` (UUID, PK)
    - `colaborador` (`@ManyToOne Funcionario`, obrigatório)
    - `dataAlvo` (LocalDate, dia que esqueceu ou precisa de ajuste)
    - `horarioPretendido` (LocalTime, horário da marcação)
    - `tipoRegistro` (`TipoRegistro`, ENTRADA ou SAIDA)
    - `motivo` (String / TEXT, preenchido pelo colaborador)
    - `status` (`StatusSolicitacaoAjuste`, default `PENDENTE`)
    - `solicitadoEm` (LocalDateTime, default `now()`)
    - **Dados da Análise do Gestor:**
        - `gestor` (`@ManyToOne Funcionario`, nullable até análise)
        - `analisadoEm` (LocalDateTime, nullable)
        - `justificativaGestor` (String / TEXT, obrigatório no deferimento e no indeferimento)
        - `batidaCriada` (`@OneToOne Batida`, nullable, setada quando deferida)

---

## 2. Backend (Spring Boot)
- [x] **Validação de Fechamento de Período:**
    - [x] Injetar consulta ao `FechamentoPeriodoRepository` para checar se a `dataAlvo` da solicitação pertence a uma competência (`YYYY-MM`) com status `CONCLUIDO` ou `CONCLUIDO_COM_PENDENCIAS`.
    - [x] Se o período estiver fechado: bloquear a abertura com HTTP `422 Unprocessable Entity` ou `400 Bad Request` ("Não é permitido solicitar ajuste para período já fechado").
- [x] **Repository (`SolicitacaoAjusteRepository`):**
    - [x] `findByColaboradorIdOrderBySolicitadoEmDesc(...)`
    - [x] `findByColaboradorGestorIdAndStatusOrderBySolicitadoEmDesc(...)`
    - [x] `findByColaboradorGestorIdOrderBySolicitadoEmDesc(...)`
- [x] **Service de Solicitação (`SolicitacaoAjusteService`):**
    - [x] `abrirSolicitacao(request)`:
        - Valida período fechado e data alvo.
        - Grava com status `PENDENTE`.
    - [x] `listarPorEquipe(gestorId, status)`:
        - Retorna solicitações dos liderados diretos do gestor autenticado.
    - [x] `listarPorColaborador(colaboradorId)`:
        - Histórico das solicitações do próprio usuário.
    - [x] `analisarSolicitacao(solicitacaoId, gestorId, status, justificativa)`:
        - Valida se status atual é `PENDENTE`.
        - Valida se `justificativa` não está em branco.
        - **Se INDEFERIDO:** atualiza status para `INDEFERIDO`, grava dados do gestor e justificativa.
        - **Se DEFERIDO:**
            - Cria uma nova instância de `Batida` com:
                - `tipoOrigem = TipoOrigem.AJUSTE_MANUAL`
                - `dataHora = LocalDateTime.of(dataAlvo, horarioPretendido)`
                - `funcionario = solicitacao.getColaborador()`
                - `autorAjuste = gestor.getNomeCompleto()`
                - `motivoAjuste = solicitacao.getMotivo()`
                - `solicitacaoAjusteId = solicitacao.getId()`
                - Geração de NSR sequencial via `seq_nsr_batida`.
            - Salva a batida.
            - Vincula `batidaCriada` na solicitação e atualiza para `DEFERIDO`.
            - Dispara `inconsistenciaService.analisarDiaColaborador(...)` para reavaliar o dia.
- [x] **Endpoints (`SolicitacaoAjusteController`):**
    - [x] `POST /api/solicitacoes-ajuste`: Colaborador abre solicitação.
    - [x] `GET /api/solicitacoes-ajuste/minhas`: Colaborador consulta seu histórico.
    - [x] `GET /api/solicitacoes-ajuste/equipe`: Gestor lista solicitações pendentes/analisadas de sua equipe.
    - [x] `POST /api/solicitacoes-ajuste/{id}/analisar`: Gestor defere ou indefere com justificativa.

---

## 3. Frontend (React)
- [x] **Fluxo do Colaborador (Meu Ponto / Painel Colaborador):**
    - [x] Botão ou ação no painel: "Solicitar Ajuste de Ponto".
    - [x] Modal de solicitação com data, horário, tipo e motivo (`SolicitarAjusteModal.jsx`).
    - [x] Card / aba "Minhas Solicitações" para acompanhar status e justificativa do gestor (`MinhasSolicitacoesSection.jsx`).
- [x] **Fluxo do Gestor (Painel da Equipe):**
    - [x] Seção "Aprovações de Ponto" com dados reais da API e contagem de pendências.
    - [x] Modal de análise com justificativa obrigatória do gestor antes de Deferir/Indeferir.
- [x] **Destaque Visual no Espelho de Ponto:**
    - [x] Marcações de origem `AJUSTE_MANUAL` com badge visual e tooltip com autor e motivo.

---

## 4. Testes e Validações (QA)
- [x] Teste de imutabilidade de batidas fiscais (`testT13_ImutabilidadeBatidasFiscais`).
- [x] Teste de recusa em período fechado (`testT13_RecusaEmPeriodoFechado`).
- [x] Teste de deferimento com geração de batida/NSR (`testT13_DeferimentoGeraBatidaENsr`).
- [x] Teste de indeferimento sem gerar batida (`testT13_IndeferimentoSemGerarBatida`).
