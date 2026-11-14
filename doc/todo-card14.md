# TODO: T14 · Pareamento das Marcações e Apuração do Dia

## 1. Banco de Dados & Modelagem (PostgreSQL / JPA)
- [x] **Criar Enum `StatusApuracaoDiaria`:**
    - `PENDENTE_INCONSISTENCIA` (ex.: dia ímpar, não calculado)
    - `APURADO` (pares válidos calculados)
    - `FALTA` (sem marcação e sem abono em dia previsto de trabalho)
    - `ABONADO` / `FOLGA` (dia sem necessidade de marcação ou justificado)
- [x] **Criar Entidade `ApuracaoDiaria`:**
    - `id` (Long / UUID)
    - `funcionario` (`@ManyToOne Funcionario`, obrigatório)
    - `dataReferencia` (LocalDate, restrição única composta `uk_apuracao_func_data` sobre `funcionario_id` + `dataReferencia`)
    - `status` (`StatusApuracaoDiaria`)
    - **Totalizadores em Minutos (Integer/Long para evitar erro de ponto flutuante):**
        - `minutosPrevistos` (da jornada vigente na data)
        - `minutosTrabalhados` (soma dos pares válidos)
        - `minutosIntervalo` (soma dos vãos entre pares)
        - `minutosAtraso` (descontada a tolerância)
        - `minutosSaidaAntecipada` (descontada a tolerância)
        - `minutosAdicionalNoturno` (já aplicada a redução da hora noturna)
        - `minutosFalta` (preenchido quando `status == FALTA`)
    - **Detalhamento de Pares e Horas Extras:**
        - `paresJson` ou entidade filha `ParMarcacao` (`entrada`, `saida`, `duracaoMinutos`, `origemEntrada`, `origemSaida`)
        - `horasExtrasPorFaixa` (JSONB ou tabela filha `ApuracaoHoraExtra` com `percentual`, `minutos`)
    - `calculadoEm` (LocalDateTime)

---

## 2. Backend — Motor de Pareamento e Cálculo (Spring Boot)
- [x] **Filtro e Ordenação de Batidas Válidas (`ApuracaoDiariaService`):**
    - [x] Buscar todas as batidas do colaborador na `dataReferencia` cujo status seja válido para cálculo (`NORMAL`, `VINCULADA`, `AJUSTE` — ignorando `ORFA_PENDENTE` e `DESCARTADA`).
    - [x] Ordenar cronologicamente por `dataHora` ascendente.
- [x] **Regra Central de Pareamento (Sem Inferência):**
    - [x] **Caso 0 batidas:**
        - Verificar escala/jornada vigente do colaborador na `dataReferencia` e eventual abono aprovado.
        - Se era dia previsto de trabalho e não há abono: gravar `ApuracaoDiaria` com status `FALTA` e `minutosFalta = minutosPrevistos`.
        - Se era folga/DSR ou abonado: gravar com status `FOLGA` / `ABONADO` e contadores zerados.
    - [x] **Caso Número Ímpar de batidas (`batidas.size() % 2 != 0`):**
        - **Não completar nem arredondar** nenhum horário.
        - Acionar `InconsistenciaService` para garantir pendência aberta do tipo `DIA_IMPAR` na data.
        - Salvar/atualizar `ApuracaoDiaria` com status `PENDENTE_INCONSISTENCIA` e zerar totalizadores até resolução.
    - [x] **Caso Número Par de batidas (`batidas.size() >= 2` e par):**
        - Formar pares sequenciais: `(0, 1)`, `(2, 3)`, `(4, 5)`...
        - `minutosTrabalhados`: soma de `Duration.between(entrada, saida).toMinutes()` de cada par.
        - `minutosIntervalo`: soma de `Duration.between(saidaParAnterior, entradaParAtual).toMinutes()` entre pares consecutivos.
- [x] **Cálculo de Atrasos, Saídas Antecipadas e Tolerância:**
    - [x] Carregar a jornada e regra de tolerância vigentes na `dataReferencia` (ex.: tolerância CLT de 5 min por batida até 10 min diários, ou regra configurada na jornada).
    - [x] Calcular diferença entre primeira entrada e início previsto, e última saída e fim previsto (além de excedente de intervalo, se aplicável).
    - [x] Se a soma das variações estiver dentro da tolerância da jornada, zerar atraso/saída antecipada e hora extra residual.
- [x] **Cálculo de Hora Extra por Faixa:**
    - [x] Apurar saldo excedente acima de `minutosPrevistos` (após tolerância).
    - [x] Distribuir os minutos excedentes nas faixas percentuais vigentes na data (ex.: até X horas a 50%, excedente a 100%, ou 100% integral em domingos/feriados).
- [x] **Cálculo de Adicional Noturno com Hora Reduzida:**
    - [x] Identificar intersecção de cada par trabalhado com a janela noturna vigente (padrão urbano: `22:00` às `05:00`, estendendo-se se cumprida integralmente).
    - [x] Aplicar o fator de redução da hora noturna (`52m30s` = fator `60 / 52.5` ≈ `1.14285714`), convertendo minutos noturnos relógio em minutos noturnos computados.
- [x] **Gatilhos de Recálculo e Endpoints (`ApuracaoController`):**
    - [x] Disparar recálculo automático do dia sempre que: importar arquivo, aprovar ajuste (T13) ou tratar inconsistência (T12) naquela data (desde que o período não esteja fechado).
    - [x] `POST /api/apuracao/recalcular`: Recálculo sob demanda por colaborador e período (`dataInicio`, `dataFim`).
    - [x] `GET /api/apuracao`: Consulta dos dias apurados por colaborador e intervalo de datas.

---

## 3. Frontend (React)
- [x] **Visualização dos Pares e Resumo Diário:**
    - [x] Exibir na grade diária os pares formados (`Entrada 1 - Saída 1`, `Entrada 2 - Saída 2`...) e o tempo total de intervalo.
    - [x] Destacar visualmente dias com status `PENDENTE_INCONSISTENCIA` ("Dia ímpar — aguardando tratamento, não apurado") sem exibir horas inventadas.
    - [x] Destacar dias com status `FALTA` em vermelho/alerta.
    - [x] Exibir colunas/badges com: Trabalhadas, Intervalo, Atrasos/Saídas Antecipadas, Horas Extras (detalhadas por faixa %) e Adicional Noturno.
- [x] **Ação de Recálculo Manual (RH):**
    - [x] Botão "Recalcular Período" na tela de apuração/espelho para forçar reprocessamento de dias em aberto.

---

## 4. Testes e Validações (QA)
- [x] **Teste de Pareamento Normal:** 4 batidas no dia (`08:00, 12:00, 13:00, 17:00`) -> 2 pares, `480m` trabalhados, `60m` intervalo.
- [x] **Teste de Dia Ímpar:** 3 batidas no dia -> status `PENDENTE_INCONSISTENCIA`, zero horas apuradas e pendência `DIA_IMPAR` criada.
- [x] **Teste de Tolerância:** Entrada com 4 min de atraso (dentro do limite de 5/10 min) -> `minutosAtraso = 0`.
- [x] **Teste de Faixas de Hora Extra:** Excedente de 3h em regra com faixa de 2h a 50% e restante a 100% -> 120 min na faixa 50% e 60 min na faixa 100%.
- [x] **Teste de Hora Noturna Reduzida:** Trabalho das `22:00` às `05:00` (7h relógio = 420 min) -> convertido para `480 min` (8h noturnas computadas).
- [x] **Teste de Falta:** Dia útil previsto sem batidas e sem abono -> status `FALTA` e `minutosFalta` igual à carga prevista do dia.