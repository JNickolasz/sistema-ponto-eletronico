# Layout do Arquivo de Exportação para a Folha de Pagamento (T20)

## 1. Visão Geral e Padrão Técnico
- **Formato:** CSV estruturado (`.csv`)
- **Codificação:** `UTF-8` com BOM (`\uFEFF`) para compatibilidade direta com Excel e sistemas legados de folha
- **Separador de Campos:** Ponto e vírgula (`;`)
- **Quebra de Linha:** `\n` (LF)
- **Ordenação Determinística:** Registros ordenados por `Matricula` (ascendente, nulo por último) e desempate por `FuncionarioId` (UUID ascendente).
- **Idempotência / Reexportação:** O arquivo **não** contém metadados variáveis de tempo de geração (como `geradoEm`) no seu corpo. Reexportar uma mesma competência fechada produz exatamente os mesmos bytes e o mesmo hash `SHA-256`.
- **Pré-requisito obrigatório:** A competência (`YYYY-MM`) precisa estar com status `CONCLUIDO` ou `CONCLUIDO_COM_PENDENCIAS` em `FechamentoPeriodo`. Competências abertas são recusadas com HTTP `409 Conflict`.

---

## 2. Especificação Campo a Campo

| Ordem | Nome da Coluna | Tipo / Formato | Unidade / Domínio | Descrição |
| :---: | :--- | :--- | :--- | :--- |
| 1 | `Competencia` | Texto (`YYYY-MM`) | Ex: `2026-10` | Competência fechada exportada. |
| 2 | `Matricula` | Texto (até 20 chars) | Ex: `COL-001` | Matrícula funcional do colaborador na empresa. |
| 3 | `CPF` | Texto (`000.000.000-00` ou dígitos) | Ex: `123.456.789-00` | CPF do colaborador para chave de integração na folha. |
| 4 | `NomeColaborador` | Texto | Ex: `João Silva` | Nome completo do colaborador. |
| 5 | `Cargo` | Texto | Ex: `Analista` | Cargo cadastrado no sistema. |
| 6 | `HorasNormaisMin` | Inteiro (>= 0) | Minutos | Total de minutos trabalhados normais no período (`totalMinutosTrabalhados - totalMinutosHoraExtra`). |
| 7 | `HorasExtras50Min` | Inteiro (>= 0) | Minutos | Total de minutos de hora extra na faixa `50%`. |
| 8 | `HorasExtras100Min` | Inteiro (>= 0) | Minutos | Total de minutos de hora extra na faixa `100%`. |
| 9 | `OutrasExtrasPorFaixa` | Texto (`faixa=min\|...`) | Ex: `75%=30` | Demais faixas de hora extra parametrizadas pela empresa (ex: `75%`, `120%`) separadas por `\|` em ordem alfabética crescente de faixa. |
| 10 | `AdicionalNoturnoMin` | Inteiro (>= 0) | Minutos | Total de minutos trabalhados em horário noturno (22h às 05h ou regra vigente). |
| 11 | `FaltasMin` | Inteiro (>= 0) | Minutos | Total de minutos de faltas não abonadas no período. |
| 12 | `AtrasosMin` | Inteiro (>= 0) | Minutos | Total de minutos de atrasos + saídas antecipadas não abonadas no período (`minutosAtraso + minutosSaidaAntecipada`). |
| 13 | `SaldoBancoHorasMin` | Inteiro (com sinal) | Minutos | Saldo final do banco de horas do colaborador na competência fechada (positivo = crédito, negativo = débito). |

---

## 3. Exemplo de Arquivo Gerado

```csv
Competencia;Matricula;CPF;NomeColaborador;Cargo;HorasNormaisMin;HorasExtras50Min;HorasExtras100Min;OutrasExtrasPorFaixa;AdicionalNoturnoMin;FaltasMin;AtrasosMin;SaldoBancoHorasMin
2026-10;COL-001;111.222.333-44;Ana Paula;Desenvolvedora;9600;120;60;;30;0;15;165
2026-10;COL-002;555.666.777-88;Bruno Costa;Analista;8640;0;0;;0;480;45;-525
```

---

## 4. Auditoria e Batimento com Espelho de Ponto
- Toda execução do endpoint `GET /api/folha/exportar` grava um registro na tabela `historico_exportacao_folha` contendo: `empresa_id`, `competencia`, `autor`, `autor_id`, `data_hora`, `nome_arquivo`, `quantidade_colaboradores` e `hash_sha256`.
- A soma das colunas numéricas por colaborador e do arquivo completo corresponde exatamente aos totalizadores exibidos no **Espelho de Ponto (T18)** da mesma competência fechada.
