# Dicionário de Dados — Sistema Ponto Certo

Documento técnico descrevendo todas as tabelas, colunas, tipos de dados, restrições de integridade (`PK`, `FK`, `UNIQUE`, `INDEX`) e regras de auditoria persistidas no banco de dados **PostgreSQL (Neon.tech)** do **Ponto Certo**.

---

## Sumário das Entidades (27 Tabelas + 1 Sequence)

1. **Multi-Tenant & Identidade Visual:** [`empresa`](#1-empresa)
2. **Estrutura Organizacional & Acessos:** [`funcionario`](#2-funcionario), [`local_trabalho`](#3-local_trabalho)
3. **Equipamentos & Terminais (Herança `JOINED`):** [`equipamento`](#4-equipamento), [`estacoes`](#5-estacoes), [`relogios`](#6-relogios), [`pairing_codes`](#7-pairing_codes)
4. **Jornadas, Escalas & Calendário:** [`jornada`](#8-jornada), [`jornada_dia`](#9-jornada_dia), [`escala`](#10-escala), [`regime_trabalho`](#11-regime_trabalho), [`feriado`](#12-feriado), [`ponto_facultativo`](#13-ponto_facultativo)
5. **Regras Parametrizáveis de Apuração:** [`regras_apuracao`](#14-regras_apuracao), [`regra_faixa_hora_extra`](#15-regra_faixa_hora_extra), [`regra_adicional_noturno`](#16-regra_adicional_noturno)
6. **Coleta de Ponto, AFD & Inconsistências (Portaria 671):** [`batida`](#17-batida) (`seq_nsr_batida`), [`historico_importacao`](#18-historico_importacao), [`inconsistencia`](#19-inconsistencia), [`solicitacao_ajuste`](#20-solicitacao_ajuste)
7. **Ocorrências, Abonos & Apuração Diária:** [`tipo_ocorrencia`](#21-tipo_ocorrencia), [`ocorrencias_colaborador`](#22-ocorrencias_colaborador), [`apuracao_diaria`](#23-apuracao_diaria)
8. **Banco de Horas, Fechamento & Folha de Pagamento:** [`movimentacao_banco_horas`](#24-movimentacao_banco_horas), [`fechamento_banco_horas`](#25-fechamento_banco_horas), [`fechamento_periodo`](#26-fechamento_periodo), [`historico_fechamento_periodo`](#27-historico_fechamento_periodo), [`historico_exportacao_folha`](#28-historico_exportacao_folha)

---

## 1. `empresa`
**Descrição:** Armazena os clientes corporativos do SaaS (tenants), garantindo isolamento multi-tenant por subdomínio e persistência de identidade visual White-Label em `JSONB`.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da empresa. | `PK` |
| `razao_social` | `VARCHAR(255)` | Razão social ou nome empresarial. | `NOT NULL` |
| `subdominio` | `VARCHAR(255)` | Identificador de subdomínio do tenant (ex: `vertice` para `vertice.pontocerto.com`). | `NOT NULL`, `UNIQUE` |
| `cnpj` | `VARCHAR(255)` | CNPJ da empresa (validado com dígitos verificadores). | `NOT NULL`, `UNIQUE` |
| `logo_url` | `VARCHAR(255)` | Caminho ou URL da logomarca customizada da empresa. | `NULLABLE` |
| `endereco` | `VARCHAR(255)` | Endereço da sede da empresa. | `NULLABLE` |
| `config_whitelabel` | `JSONB` | Objeto JSON (`WhiteLabel`: `url`, `urlLogo`, `primaryColor`, `secundaryColor`) com a paleta e marca do cliente. | `NULLABLE` |

---

## 2. `funcionario`
**Descrição:** Entidade central de pessoas e credenciais. `COLABORADOR`, `GESTOR` e `RH` compartilham a mesma tabela diferenciados por `perfil_acesso`, com auto-relacionamento hierárquico via `gestor_id` e unicidade de documentos escopada por `empresa_id`.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do funcionário. | `PK` |
| `empresa_id` | `UUID` | Empresa (tenant) à qual o funcionário pertence. | `FK (empresa.id)`, `NOT NULL` |
| `gestor_id` | `UUID` | Auto-relacionamento indicando o gestor imediato do colaborador. | `FK (funcionario.id)`, `NULLABLE` |
| `local_trabalho_id` | `UUID` | Local físico principal de lotação do colaborador. | `FK (local_trabalho.id)`, `NULLABLE` |
| `perfil_acesso` | `VARCHAR(30)` | Papel de autorização no sistema (`PerfilAcesso`). | `NOT NULL` (`COLABORADOR`, `GESTOR`, `RH`) |
| `matricula` | `VARCHAR(20)` | Matrícula funcional interna na empresa. | `UNIQUE (empresa_id, matricula)` |
| `cpf` | `VARCHAR(14)` | CPF validado por algoritmo de dígitos verificadores. | `UNIQUE (empresa_id, cpf)` |
| `pis_pasep` | `VARCHAR(14)` | NIS/PIS/PASEP oficial (utilizado no espelho e leitura AFD posicional). | `UNIQUE (empresa_id, pis_pasep)` |
| `nome_completo` | `VARCHAR(255)` | Nome civil completo do colaborador. | `NOT NULL` |
| `email` | `VARCHAR(255)` | E-mail corporativo do colaborador. | `UNIQUE (empresa_id, email)` |
| `telefone` | `VARCHAR(255)` | Telefone de contato. | `NULLABLE` |
| `usuario` | `VARCHAR(255)` | Nome de usuário para autenticação no subdomínio da empresa. | `NOT NULL`, `UNIQUE (empresa_id, usuario)` |
| `senha_hash` | `VARCHAR(255)` | Hash BCrypt da senha de acesso. | `NOT NULL` |
| `jornada_id` | `VARCHAR(255)` | Campo legado textual; o vínculo formal com vigência ocorre em `regime_trabalho`. | `NULLABLE` |
| `cargo` | `VARCHAR(255)` | Cargo ou função exercida. | `NULLABLE` |
| `data_admissao` | `DATE` | Data de admissão do colaborador. | `NOT NULL` |
| `data_desligamento` | `DATE` | Data de desligamento (quando preenchida e `<= hoje`, revoga o acesso imediatamente). | `NULLABLE` |

---

## 3. `local_trabalho`
**Descrição:** Unidades físicas da empresa (Matriz, Filiais, Obras) onde equipamentos são instalados e colaboradores são alocados, com suporte a cerca geográfica e validação de IP.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do local de trabalho. | `PK` |
| `empresa_id` | `UUID` | Empresa proprietária do local. | `FK (empresa.id)`, `NOT NULL` |
| `nome` | `VARCHAR(255)` | Nome identificador (ex: `Matriz — Recife`, `Filial — São Paulo`). | `NOT NULL` |
| `latitude` | `NUMERIC(8,6)` | Coordenada geográfica de latitude. | `NULLABLE` |
| `longitude` | `NUMERIC(9,6)` | Coordenada geográfica de longitude. | `NULLABLE` |
| `raio_metros` | `INTEGER` | Raio máximo permitido em metros para geolocalização. | Default: `0` |
| `ip_esperado` | `VARCHAR(45)` | Endereço IPv4/IPv6 esperado na rede local. | `NULLABLE` |
| `endereco` | `VARCHAR(255)` | Logradouro, número e bairro. | `NOT NULL` |
| `municipio` | `VARCHAR(255)` | Nome do município (usado na resolução de feriados municipais). | `NOT NULL` |
| `uf` | `VARCHAR(2)` | Sigla da Unidade Federativa (usada na resolução de feriados estaduais). | `NOT NULL` |

---

## 4. `equipamento`
**Descrição:** Superclasse (`InheritanceType.JOINED`) que unifica o cadastro de terminais de coleta de ponto (`RELOGIO` físico AFD e `ESTACAO` web).

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do equipamento. | `PK` |
| `empresa_id` | `UUID` | Empresa proprietária do equipamento. | `FK (empresa.id)`, `NOT NULL` |
| `local_trabalho_id` | `UUID` | Local físico onde o equipamento está instalado. | `FK (local_trabalho.id)`, `NOT NULL` |
| `codigo` | `VARCHAR(20)` | Código interno do equipamento na empresa (ex: `EST-REC-01`, `REP-REC-01`). | `NOT NULL`, `UNIQUE (empresa_id, codigo)` |
| `tipo` | `VARCHAR(30)` | Tipo do equipamento (`TipoEquipamento`). | `NOT NULL`, Imutável (`RELOGIO`, `ESTACAO`) |
| `identificacao` | `VARCHAR(255)` | Nome descritivo da instalação (ex: `Terminal Recepção Matriz`). | `NOT NULL` |
| `status` | `VARCHAR(30)` | Status operacional (`StatusEquipamento`). | `NOT NULL` (`ATIVO`, `INATIVO`, `MANUTENCAO`) |
| `created_by_id` | `UUID` | Usuário de RH que cadastrou o equipamento. | `FK (funcionario.id)`, `NULLABLE` |
| `updated_by_id` | `UUID` | Último usuário de RH que atualizou o equipamento. | `FK (funcionario.id)`, `NULLABLE` |
| `created_at` | `TIMESTAMPTZ` | Data/hora de criação do registro. | `NOT NULL`, `updatable = false` |
| `updated_at` | `TIMESTAMPTZ` | Data/hora da última atualização. | `NULLABLE` |

---

## 5. `estacoes`
**Descrição:** Subclasse (`JOINED`) de `equipamento` para terminais web de recepção/corredor (`tipo = ESTACAO`), armazenando credenciais e status de pareamento do terminal.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Mesmo identificador do registro pai em `equipamento`. | `PK`, `FK (equipamento.id)` |
| `credential_id` | `VARCHAR(255)` | Identificador da credencial criptográfica do terminal pareado. | `NULLABLE` |
| `public_key` | `VARCHAR(255)` | Chave pública associada à estação. | `NULLABLE` |
| `status_estacao` | `VARCHAR(30)` | Estado do vínculo da estação (`StatusEstacao`). | `NOT NULL` (`ATIVO`, `PENDENTE`, `REVOGADO`) |
| `revoked_at` | `TIMESTAMP` | Data/hora de revogação da estação. | `NULLABLE` |
| `revoked_by_id` | `UUID` | Funcionário que revogou a estação. | `FK (funcionario.id)`, `NULLABLE` |

---

## 6. `relogios`
**Descrição:** Subclasse (`JOINED`) de `equipamento` para Registradores Eletrônicos de Ponto físicos (`tipo = RELOGIO`) que exportam arquivos posicionais AFD.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Mesmo identificador do registro pai em `equipamento`. | `PK`, `FK (equipamento.id)` |
| `numeros_fabricante` | `VARCHAR(100)` | Número de série de fabricação gravado no cabeçalho AFD. | `NOT NULL`, `UNIQUE` |
| `linhas_importadas` | `BIGINT` | Total acumulado de registros já importados deste relógio. | `NULLABLE` |

---

## 7. `pairing_codes`
**Descrição:** Códigos temporários (com hash e TTL) emitidos para pareamento seguro de uma estação web com o servidor.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do código de pareamento. | `PK` |
| `estacao_id` | `UUID` | Estação alvo do pareamento. | `FK (estacoes.id)`, `NOT NULL`, `UNIQUE` |
| `pairing_code_hash` | `VARCHAR(255)` | Hash criptográfico do código de pareamento gerado. | `NOT NULL` |
| `status` | `VARCHAR(30)` | Estado de validade (`StatusPairingCode`). | `NOT NULL` (`VALIDO`, `EXPIRADO`) |
| `expires_at` | `TIMESTAMPTZ` | Instante de expiração do código. | `NOT NULL` |

---

## 8. `jornada`
**Descrição:** Quadro de horário semanal fixo (ex: `Engenharia & Projetos 40h`), composto pelos dias da semana em `jornada_dia`.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da jornada. | `PK` |
| `empresa_id` | `UUID` | Empresa proprietária da jornada. | `FK (empresa.id)`, `NOT NULL` |
| `nome` | `VARCHAR(100)` | Nome descritivo da jornada. | `NULLABLE` |
| `tolerancia_minutos` | `INTEGER` | Tolerância diária em minutos para atrasos/extras (CLT art. 58 §1º). | `NOT NULL`, Default: `10` |
| `carga_horaria_semanal_minutos` | `INTEGER` | Soma da carga horária semanal prevista em minutos. | `NULLABLE` |

---

## 9. `jornada_dia`
**Descrição:** Configuração individual de cada dia da semana (`DOMINGO` a `SÁBADO`) pertencente a uma `jornada`.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da configuração do dia. | `PK` |
| `jornada_id` | `UUID` | Jornada semanal à qual o dia pertence. | `FK (jornada.id)`, `NOT NULL` |
| `dia_semana` | `VARCHAR(15)` | Dia da semana (`DiaSemana`). | `NOT NULL` (`DOMINGO`..`SÁBADO`) |
| `dia_trabalho` | `BOOLEAN` | Indica se há expediente previsto (`true`) ou DSR/folga (`false`). | `NOT NULL` |
| `hora_entrada` | `TIME` | Horário previsto de entrada. | `NULLABLE` |
| `hora_saida` | `TIME` | Horário previsto de saída. | `NULLABLE` |
| `intervalo_inicio` | `TIME` | Horário previsto de saída para almoço/intervalo. | `NULLABLE` |
| `intervalo_fim` | `TIME` | Horário previsto de retorno do intervalo. | `NULLABLE` |
| `carga_diaria_minutos` | `INTEGER` | Carga líquida prevista no dia em minutos. | `NULLABLE` |

---

## 10. `escala`
**Descrição:** Regime cíclico de revezamento (ex: `12x36`, `6x1`, `5x2`) baseado em contagem contínua de dias de trabalho e dias de folga a partir do início de vigência.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da escala. | `PK` |
| `empresa_id` | `UUID` | Empresa proprietária da escala. | `FK (empresa.id)`, `NOT NULL` |
| `nome` | `VARCHAR(100)` | Nome descritivo da escala (ex: `Plantão Campo 12x36`). | `NOT NULL` |
| `dias_trabalho` | `INTEGER` | Quantidade de dias seguidos trabalhados no ciclo. | `NOT NULL` |
| `dias_folga` | `INTEGER` | Quantidade de dias seguidos de folga no ciclo. | `NOT NULL` |
| `hora_entrada` | `TIME` | Horário previsto de início do turno. | `NOT NULL` |
| `hora_saida` | `TIME` | Horário previsto de término do turno. | `NOT NULL` |
| `intervalo_inicio` | `TIME` | Início do intervalo intrajornada. | `NOT NULL` |
| `intervalo_fim` | `TIME` | Fim do intervalo intrajornada. | `NOT NULL` |
| `tolerancia_minutos` | `INTEGER` | Minutos de tolerância na apuração do turno. | `NOT NULL`, Default: `10` |
| `carga_diaria_minutos` | `INTEGER` | Carga líquida diária em minutos nos dias de trabalho. | `NOT NULL` |

---

## 11. `regime_trabalho`
**Descrição:** Tabela de vínculo temporal entre `funcionario` e seu regime (`JORNADA` ou `ESCALA`), mantendo histórico completo de vigências sem sobrescrever períodos passados.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do vínculo de vigência. | `PK` |
| `funcionario_id` | `UUID` | Colaborador vinculado ao regime. | `FK (funcionario.id)`, `NOT NULL`, Indexado (`idx_regime_vigencia`) |
| `tipo_regime` | `VARCHAR(15)` | Tipo de regime ativo (`TipoRegime`). | `NOT NULL` (`JORNADA`, `ESCALA`) |
| `jornada_id` | `UUID` | Jornada semanal associada (quando `tipo_regime = JORNADA`). | `FK (jornada.id)`, `NULLABLE` |
| `escala_id` | `UUID` | Escala cíclica associada (quando `tipo_regime = ESCALA`). | `FK (escala.id)`, `NULLABLE` |
| `data_inicio_vigencia` | `DATE` | Data inicial em que o regime passa a valer para a apuração. | `NOT NULL`, Indexado (`idx_regime_vigencia`) |
| `data_fim_vigencia` | `DATE` | Data final da vigência (`NULL` indica regime atualmente vigente). | `NULLABLE`, Indexado (`idx_regime_vigencia`) |

---

## 12. `feriado`
**Descrição:** Calendário de feriados com escopo `NACIONAL` (global do SaaS, `empresa_id = NULL`), `ESTADUAL` (por `uf`) ou `MUNICIPAL` (por `uf` + `municipio` do `local_trabalho` do colaborador).

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do feriado. | `PK` |
| `empresa_id` | `UUID` | Empresa proprietária (`NULL` para feriados nacionais globais cadastrados pelo Admin SaaS). | `FK (empresa.id)`, `NULLABLE` |
| `descricao` | `VARCHAR(255)` | Nome do feriado (ex: `Confraternização Universal`, `São João`). | `NOT NULL` |
| `data` | `DATE` | Data do feriado. | `NOT NULL` |
| `alcance` | `VARCHAR(30)` | Abrangência geográfica (`Alcance`). | `NOT NULL` (`NACIONAL`, `ESTADUAL`, `MUNICIPAL`) |
| `uf` | `VARCHAR(255)` | Sigla da UF (obrigatória para `ESTADUAL` e `MUNICIPAL`). | `NULLABLE` |
| `municipio` | `VARCHAR(255)` | Nome do município (obrigatório para `MUNICIPAL`). | `NULLABLE` |
| `ativo` | `BOOLEAN` | Indica se o feriado está ativo para o motor de apuração. | `NOT NULL`, Default: `true` |

---

## 13. `ponto_facultativo`
**Descrição:** Datas de ponto facultativo por empresa e alcance geográfico, nas quais a ausência não gera falta, mas o trabalho realizado é contabilizado normalmente sem adicional de feriado.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do ponto facultativo. | `PK` |
| `empresa_id` | `UUID` | Empresa à qual o ponto facultativo se aplica. | `FK (empresa.id)`, `NULLABLE` |
| `descricao` | `VARCHAR(255)` | Descrição da data (ex: `Carnaval`, `Corpus Christi`). | `NOT NULL` |
| `data` | `DATE` | Data do ponto facultativo. | `NOT NULL` |
| `alcance` | `VARCHAR(30)` | Abrangência geográfica (`Alcance`). | `NOT NULL` (`NACIONAL`, `ESTADUAL`, `MUNICIPAL`) |
| `uf` | `VARCHAR(255)` | Sigla da UF (quando aplicável). | `NULLABLE` |
| `municipio` | `VARCHAR(255)` | Município (quando aplicável). | `NULLABLE` |
| `ativo` | `BOOLEAN` | Indica se o registro está ativo. | `NOT NULL`, Default: `true` |

---

## 14. `regras_apuracao`
**Descrição:** Parametrização versionada por data de vigência das regras trabalhistas de apuração da empresa (limites de horas extras, faixas e adicional noturno).

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da regra de apuração. | `PK` |
| `empresa_id` | `UUID` | Empresa à qual a regra pertence. | `FK (empresa.id)`, `NOT NULL`, `UNIQUE (empresa_id, inicio_vigencia)` |
| `inicio_vigencia` | `DATE` | Data a partir da qual esta regra passa a vigorar. | `NOT NULL`, `UNIQUE (empresa_id, inicio_vigencia)` |
| `limite_diario_extra_em_minutos` | `INTEGER` | Teto diário de horas extras em minutos (ex: `120` min = 2h). | `NOT NULL` |
| `created_by` | `UUID` | Funcionário que cadastrou a regra. | `FK (funcionario.id)`, `NULLABLE` |
| `updated_by` | `UUID` | Funcionário que atualizou a regra. | `FK (funcionario.id)`, `NULLABLE` |
| `created_at` | `TIMESTAMPTZ` | Data/hora de criação. | `NULLABLE` |
| `updated_at` | `TIMESTAMPTZ` | Data/hora de atualização. | `NULLABLE` |

---

## 15. `regra_faixa_hora_extra`
**Descrição:** Faixas progressivas de percentual de hora extra (ex: 1ª faixa de até 120 min a 50%, 2ª faixa acima disso a 100%) vinculadas a uma `regras_apuracao`.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da faixa. | `PK` |
| `regra_apuracao_id` | `UUID` | Regra de apuração pai. | `FK (regras_apuracao.id)`, `NOT NULL`, `UNIQUE (regra_apuracao_id, ordem)` |
| `ordem` | `INTEGER` | Ordem de aplicação da faixa (`1`, `2`, ...). | `NOT NULL`, `UNIQUE (regra_apuracao_id, ordem)` |
| `duracao_em_minutos` | `INTEGER` | Limite de minutos desta faixa (`NULL` para a última faixa sem teto). | `NULLABLE` |
| `percentual` | `NUMERIC(5,2)` | Percentual de acréscimo da faixa (ex: `50.00`, `100.00`). | `NOT NULL` |

---

## 16. `regra_adicional_noturno`
**Descrição:** Parâmetros do horário noturno e redução da hora noturna (ex: das `22:00` às `05:00`, hora reduzida de `3150` segundos = 52m30s, adicional de `20%`) vinculados 1:1 com `regras_apuracao`.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da configuração noturna. | `PK` |
| `regra_apuracao_id` | `UUID` | Regra de apuração associada. | `FK (regras_apuracao.id)`, `NOT NULL`, `UNIQUE` |
| `hora_inicio` | `TIME` | Início da janela noturna (ex: `22:00:00`). | `NOT NULL` |
| `hora_fim` | `TIME` | Fim da janela noturna (ex: `05:00:00`). | `NOT NULL` |
| `duracao_hora_noturna_segundos` | `INTEGER` | Duração da hora noturna reduzida em segundos (padrão CLT: `3150`). | `NOT NULL` |
| `percentual` | `NUMERIC(5,2)` | Percentual do adicional noturno (padrão CLT: `20.00`). | `NOT NULL` |

---

## 17. `batida`
**Descrição:** Registro individual e imutável de marcação de ponto (Portaria 671/MTP). Possui numeração sequencial monotônica (`nsr`) via sequence `seq_nsr_batida` e bloqueio de exclusão (`@PreRemove`) para marcações de estação/relógio.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da batida. | `PK`, `updatable = false` |
| `nsr` | `BIGINT` | Número Sequencial de Registro emitido pelo sistema/equipamento. | `NOT NULL`, `updatable = false`, Default `nextval('seq_nsr_batida')`, `UNIQUE (equipamento_id, nsr)` |
| `funcionario_id` | `UUID` | Colaborador identificado na marcação. | `FK (funcionario.id)`, `NULLABLE` (nulo enquanto `ORFA_PENDENTE`) |
| `identificador_original` | `VARCHAR(255)` | Identificador bruto lido no arquivo AFD (PIS, CPF ou matrícula). | `NULLABLE`, `updatable = false` |
| `data_hora` | `TIMESTAMP` | Data e hora oficiais do registro da batida. | `NOT NULL`, `updatable = false` |
| `tipo_registro` | `VARCHAR(30)` | Sentido da marcação (`TipoRegistro`). | `NOT NULL`, `updatable = false` (`ENTRADA`, `SAIDA`) |
| `status_batida` | `VARCHAR(30)` | Situação para o motor de apuração (`StatusBatida`). | `NOT NULL`, Default `'NORMAL'` (`NORMAL`, `ORFA_PENDENTE`, `VINCULADA`, `DESCARTADA`) |
| `tipo_origem` | `VARCHAR(30)` | Canal de origem (`TipoOrigem`). | `NOT NULL`, `updatable = false` (`ESTACAO`, `ARQUIVO_IMPORTADO`, `AJUSTE_MANUAL`, `APLICATIVO_WEB`) |
| `equipamento_id` | `UUID` | Estação web ou relógio físico onde ocorreu a marcação. | `FK (equipamento.id)`, `NULLABLE`, `updatable = false` |
| `arquivo_importado_id` | `UUID` | Lote de importação AFD de origem (`historico_importacao.id`). | `NULLABLE`, `updatable = false` |
| `pedido_ajuste_id` | `UUID` | Referência de ajuste manual (legado). | `NULLABLE`, `updatable = false` |
| `solicitacao_ajuste_id` | `UUID` | Solicitação de ajuste deferida (`solicitacao_ajuste.id`) que gerou a batida. | `NULLABLE`, `updatable = false` |
| `autor_ajuste` | `VARCHAR(255)` | Login/nome do gestor ou RH que autorizou a inclusão manual. | `NULLABLE`, `updatable = false` |
| `motivo_ajuste` | `TEXT` | Justificativa auditável da marcação incluída manualmente. | `NULLABLE`, `updatable = false` |
| `vinculado_por` | `VARCHAR(255)` | Usuário de RH que vinculou batida órfã ou descartou duplicada. | `NULLABLE` |
| `data_vinculo` | `TIMESTAMP` | Data/hora do tratamento manual de vínculo ou descarte. | `NULLABLE` |
| `motivo_vinculo` | `VARCHAR(255)` | Justificativa informada no tratamento da batida órfã/duplicada. | `NULLABLE` |
| `foto_url` | `VARCHAR(255)` | Foto capturada na estação de ponto no ato da batida (Data URL/URI). | `NULLABLE`, `updatable = false` |
| `latitude_recebida` | `NUMERIC` | Latitude capturada na estação no ato da batida. | `NULLABLE`, `updatable = false` |
| `longitude_recebida` | `NUMERIC` | Longitude capturada na estação no ato da batida. | `NULLABLE`, `updatable = false` |
| `ip_recebido` | `VARCHAR(255)` | Endereço IP do cliente no momento da marcação. | `NULLABLE`, `updatable = false` |

---

## 18. `historico_importacao`
**Descrição:** Auditoria de lotes de arquivos posicionais AFD (`.txt`) processados pelo sistema, incluindo totalizadores e cópia integral do arquivo para rastreabilidade.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do lote de importação. | `PK` |
| `data_importacao` | `TIMESTAMP` | Data e hora da execução da importação. | `NOT NULL`, `updatable = false` |
| `autor_id` | `UUID` | Usuário de RH que realizou a importação. | `FK (funcionario.id)`, `NULLABLE` |
| `equipamento_id` | `UUID` | Relógio de ponto selecionado como origem do arquivo. | `FK (equipamento.id)`, `NOT NULL` |
| `nome_arquivo` | `VARCHAR(255)` | Nome original do arquivo `.txt` enviado. | `NOT NULL` |
| `conteudo_arquivo` | `TEXT` | Conteúdo bruto do arquivo AFD (limitado a 5 MB). | `NULLABLE` |
| `total_lidas` | `INTEGER` | Total de linhas de marcação (tipo `3`) processadas. | Default: `0` |
| `total_validas` | `INTEGER` | Total de batidas inéditas vinculadas a colaboradores ativos. | `NOT NULL`, Default: `0` |
| `total_repetidas` | `INTEGER` | Total de batidas ignoradas por deduplicação (`equipamento + nsr` ou mesmo horário). | `NOT NULL`, Default: `0` |
| `total_recusadas` | `INTEGER` | Total de batidas sem colaborador identificado (gravadas como `ORFA_PENDENTE`). | `NOT NULL`, Default: `0` |
| `status` | `VARCHAR(30)` | Resultado da transação (`StatusImportacao`). | `NOT NULL` (`PENDENTE`, `CONCLUIDA`, `CANCELADA`, `FALHA`) |

---

## 19. `inconsistencia`
**Descrição:** Fila centralizada de pendências geradas automaticamente pelo motor de importação e apuração, exigindo tratamento formal e auditado pelo RH.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da inconsistência. | `PK` |
| `empresa_id` | `UUID` | Empresa à qual a pendência pertence. | `FK (empresa.id)`, `NOT NULL` |
| `tipo` | `VARCHAR(40)` | Categoria da anomalia (`TipoInconsistencia`). | `NOT NULL` (`ORFA`, `DIA_IMPAR`, `DUPLICADA_MINUTO`, `FORA_DA_JORNADA`, `SEQUENCIA_INVALIDA`, `EQUIPAMENTO_DESCONHECIDO`, `MARCACAO_PERIODO_FECHADO`) |
| `situacao` | `VARCHAR(30)` | Estado atual na fila (`StatusInconsistencia`). | `NOT NULL`, Default `'ABERTA'` (`ABERTA`, `RESOLVIDA`) |
| `funcionario_id` | `UUID` | Colaborador afetado (nulo enquanto `ORFA` não vinculada ou `EQUIPAMENTO_DESCONHECIDO`). | `FK (funcionario.id)`, `NULLABLE` |
| `equipamento_id` | `UUID` | Equipamento de origem da marcação. | `FK (equipamento.id)`, `NULLABLE` |
| `batida_origem_id` | `UUID` | Marcação principal que gerou a pendência (`batida.id`). | `NULLABLE` |
| `batida_secundaria_id` | `UUID` | Segunda marcação relacionada (duplicada ou batida de ajuste criada). | `NULLABLE` |
| `arquivo_importado_id` | `UUID` | Lote AFD de origem (`historico_importacao.id`). | `NULLABLE` |
| `nome_arquivo` | `VARCHAR(255)` | Nome do arquivo `.txt` que originou a pendência. | `NULLABLE` |
| `identificador_recebido` | `VARCHAR(255)` | PIS/CPF/Matrícula ou serial bruto não reconhecido na leitura. | `NULLABLE` |
| `data_referencia` | `DATE` | Data de competência da ocorrência para apuração. | `NOT NULL` |
| `data_ocorrencia_original` | `DATE` | Data original da batida tardia recebida após fechamento de período. | `NULLABLE` |
| `competencia_alocacao` | `VARCHAR(7)` | Competência (`YYYY-MM`) aberta na qual uma batida de período fechado foi alocada. | `NULLABLE` |
| `data_hora_origem` | `TIMESTAMP` | Data/hora do evento gerador da inconsistência. | `NOT NULL` |
| `descricao_origem` | `TEXT` | Diagnóstico automático detalhando a causa da pendência. | `NOT NULL` |
| `chave_deduplicacao` | `VARCHAR(255)` | Chave determinística para evitar duplicação da mesma pendência aberta. | `NULLABLE` |
| `criado_em` | `TIMESTAMP` | Data/hora de criação da pendência. | `NOT NULL`, `updatable = false` |
| `acao_tomada` | `VARCHAR(40)` | Ação executada pelo RH (`AcaoTratamento`). | `NULLABLE` (`VINCULAR`, `INCLUIR_MARCACAO_AJUSTE`, `DESCARTAR_DUPLICADA`, `ABONAR_DIA`) |
| `tratado_por` | `VARCHAR(255)` | Nome/usuário do RH responsável pela resolução. | `NULLABLE` |
| `tratado_por_id` | `UUID` | ID do funcionário de RH responsável pela resolução. | `NULLABLE` |
| `tratado_em` | `TIMESTAMP` | Data/hora em que a pendência foi resolvida. | `NULLABLE` |
| `motivo_tratamento` | `TEXT` | Justificativa obrigatória registrada pelo RH. | `NULLABLE` |
| `detalhe_resolucao` | `TEXT` | Resumo auditável do efeito gerado pela resolução. | `NULLABLE` |

---

## 20. `solicitacao_ajuste`
**Descrição:** Pedidos de inclusão de marcação esquecida submetidos pelo próprio colaborador e analisados por seu Gestor imediato (ou RH). Quando deferida, gera automaticamente uma `batida` com `tipo_origem = AJUSTE_MANUAL` e recalcula o dia.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da solicitação. | `PK` |
| `colaborador_id` | `UUID` | Colaborador autor do pedido. | `FK (funcionario.id)`, `NOT NULL` |
| `data_alvo` | `DATE` | Data do espelho de ponto que se deseja ajustar. | `NOT NULL` |
| `horario_pretendido` | `TIME` | Horário pleiteado para a marcação. | `NOT NULL` |
| `tipo_registro` | `VARCHAR(30)` | Tipo da marcação solicitada (`TipoRegistro`). | `NOT NULL` (`ENTRADA`, `SAIDA`) |
| `motivo` | `TEXT` | Justificativa do colaborador para o esquecimento/ajuste. | `NOT NULL` |
| `status` | `VARCHAR(30)` | Estado do fluxo (`StatusSolicitacaoAjuste`). | `NOT NULL`, Default `'PENDENTE'` (`PENDENTE`, `DEFERIDO`, `INDEFERIDO`) |
| `solicitado_em` | `TIMESTAMP` | Data/hora de abertura da solicitação. | `NOT NULL` |
| `gestor_id` | `UUID` | Gestor ou RH que avaliou o pedido. | `FK (funcionario.id)`, `NULLABLE` |
| `analisado_em` | `TIMESTAMP` | Data/hora da decisão (deferimento ou indeferimento). | `NULLABLE` |
| `justificativa_gestor` | `TEXT` | Parecer registrado pelo gestor/RH na análise. | `NULLABLE` |
| `batida_criada_id` | `UUID` | Marcação oficial gerada em `batida` após o deferimento. | `FK (batida.id)`, `UNIQUE`, `NULLABLE` |

---

## 21. `tipo_ocorrencia`
**Descrição:** Catálogo configurável por empresa de motivos de ausência ou justificativa (ex: `Atestado Médico`, `Declaração de Comparecimento`, `Licença Paternidade`).

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do tipo de ocorrência. | `PK` |
| `empresa_id` | `UUID` | Empresa proprietária do catálogo. | `FK (empresa.id)`, `NOT NULL`, `UNIQUE (empresa_id, nome)` |
| `nome` | `VARCHAR(100)` | Nome da ocorrência. | `NOT NULL`, `UNIQUE (empresa_id, nome)` |
| `descricao` | `TEXT` | Explicação e regra de uso do tipo de ocorrência. | `NULLABLE` |
| `tipo_abono` | `VARCHAR(20)` | Impacto na apuração (`TipoAbonoOcorrencia`). | `NOT NULL` (`INTEGRAL`, `PARCIAL`, `APENAS_REGISTRO`) |
| `minutos_abono_padrao` | `INTEGER` | Quantidade padrão de minutos abonados (quando `PARCIAL`). | `NULLABLE` |
| `exige_anexo` | `BOOLEAN` | Indica se o lançamento exige upload de comprovante/atestado. | `NOT NULL`, Default: `false` |
| `ativo` | `BOOLEAN` | Indica se o tipo de ocorrência está disponível para novos lançamentos. | `NOT NULL`, Default: `true` |

---

## 22. `ocorrencias_colaborador`
**Descrição:** Lançamentos de ocorrências e abonos aplicados a um período (`data_inicio` a `data_fim`) de um colaborador, com suporte a anexo comprobatório e recálculo automático da apuração diária.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do lançamento. | `PK` |
| `funcionario_id` | `UUID` | Colaborador beneficiado pela ocorrência. | `FK (funcionario.id)`, `NOT NULL`, Indexado (`idx_ocorrencia_func_periodo`) |
| `tipo_ocorrencia_id` | `UUID` | Tipo de ocorrência aplicado. | `FK (tipo_ocorrencia.id)`, `NOT NULL` |
| `data_inicio` | `DATE` | Data inicial da cobertura da ocorrência. | `NOT NULL`, Indexado (`idx_ocorrencia_func_periodo`) |
| `data_fim` | `DATE` | Data final da cobertura da ocorrência. | `NOT NULL`, Indexado (`idx_ocorrencia_func_periodo`) |
| `minutos_abono_por_dia` | `INTEGER` | Minutos abonados por dia (quando `tipo_abono = PARCIAL`). | `NULLABLE` |
| `observacao` | `TEXT` | Observação clínica/administrativa registrada pelo RH. | `NULLABLE` |
| `nome_arquivo_anexo` | `VARCHAR(255)` | Nome original do arquivo comprobatório anexado. | `NULLABLE` |
| `caminho_anexo` | `VARCHAR(500)` | Caminho interno seguro no servidor onde o anexo foi salvo. | `NULLABLE` |
| `content_type_anexo` | `VARCHAR(100)` | MIME type validado do anexo (`application/pdf`, `image/png`, `image/jpeg`). | `NULLABLE` |
| `lancado_por` | `VARCHAR(150)` | Usuário de RH que registrou a ocorrência. | `NOT NULL` |
| `lancado_em` | `TIMESTAMP` | Data/hora do lançamento. | `NOT NULL` |

---

## 23. `apuracao_diaria`
**Descrição:** Consolidação diária determinística do espelho de ponto de cada colaborador, contendo o pareamento de batidas (`ENTRADA`/`SAIDA`), horas previstas, trabalhadas, intervalo, atrasos, saídas antecipadas, faltas, abonos, adicional noturno e horas extras por faixa.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da apuração do dia. | `PK` |
| `funcionario_id` | `UUID` | Colaborador titular do dia apurado. | `FK (funcionario.id)`, `NOT NULL`, `UNIQUE (funcionario_id, data_referencia)` |
| `data_referencia` | `DATE` | Data calendário apurada. | `NOT NULL`, `UNIQUE (funcionario_id, data_referencia)` |
| `status` | `VARCHAR(40)` | Situação consolidada do dia (`StatusApuracaoDiaria`). | `NOT NULL` (`PENDENTE_INCONSISTENCIA`, `APURADO`, `FALTA`, `FOLGA`, `ABONADO`) |
| `minutos_previstos` | `INTEGER` | Carga horária prevista para o dia segundo jornada/escala/calendário. | `NOT NULL`, Default: `0` |
| `minutos_trabalhados` | `INTEGER` | Total líquido de minutos efetivamente trabalhados nos pares fechados. | `NOT NULL`, Default: `0` |
| `minutos_intervalo` | `INTEGER` | Total de minutos de intervalo intrajornada realizado. | `NOT NULL`, Default: `0` |
| `minutos_atraso` | `INTEGER` | Minutos de atraso na entrada acima da tolerância. | `NOT NULL`, Default: `0` |
| `minutos_saida_antecipada` | `INTEGER` | Minutos de saída antecipada acima da tolerância. | `NOT NULL`, Default: `0` |
| `minutos_adicional_noturno` | `INTEGER` | Minutos trabalhados na janela noturna (já convertidos pela hora noturna reduzida). | `NOT NULL`, Default: `0` |
| `minutos_falta` | `INTEGER` | Minutos de ausência injustificada no dia. | `NOT NULL`, Default: `0` |
| `minutos_hora_extra_total` | `INTEGER` | Total de minutos excedentes trabalhados acima da jornada e tolerância. | `NOT NULL`, Default: `0` |
| `minutos_abonados` | `INTEGER` | Total de minutos abonados por ocorrência/atestado no dia. | `NOT NULL`, Default: `0` |
| `ocorrencia_id` | `UUID` | Referência à ocorrência (`ocorrencias_colaborador.id`) aplicada no dia. | `NULLABLE` |
| `tipo_ocorrencia_nome` | `VARCHAR(100)` | Nome da ocorrência desnormalizado para exibição rápida no espelho. | `NULLABLE` |
| `pares_json` | `TEXT` | JSON detalhando os pares `(entrada, saida, duracaoMinutos)` formados no dia. | `NULLABLE` |
| `horas_extras_por_faixa_json` | `TEXT` | JSON detalhando a distribuição das horas extras por faixa percentual (`50%`, `100%`, etc.). | `NULLABLE` |
| `calculado_em` | `TIMESTAMP` | Data/hora do último cálculo de apuração deste dia. | `NOT NULL` |

---

## 24. `movimentacao_banco_horas`
**Descrição:** Razão contábil (extrato analítico) de todas as movimentações de crédito, débito e compensação do banco de horas de cada colaborador.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do lançamento no extrato. | `PK` |
| `funcionario_id` | `UUID` | Colaborador titular do banco de horas. | `FK (funcionario.id)`, `NOT NULL`, Indexado (`idx_bh_func_data`) |
| `data_referencia` | `DATE` | Data do fato gerador (dia apurado ou data da compensação). | `NOT NULL`, Indexado (`idx_bh_func_data`) |
| `tipo` | `VARCHAR(30)` | Natureza do lançamento (`TipoMovimentacaoBancoHoras`). | `NOT NULL`, `UNIQUE (apuracao_diaria_id, tipo)` (`CREDITO_APURACAO`, `DEBITO_APURACAO`, `COMPENSACAO`, `FECHAMENTO_PERIODO`) |
| `minutos` | `INTEGER` | Valor assinado em minutos (`+` para crédito, `-` para débito/compensação). | `NOT NULL` |
| `apuracao_diaria_id` | `UUID` | Vínculo com `apuracao_diaria.id` nos lançamentos automáticos. | `NULLABLE`, `UNIQUE (apuracao_diaria_id, tipo)` |
| `descricao` | `VARCHAR(500)` | Histórico descritivo da movimentação. | `NOT NULL` |
| `periodo_fechado` | `BOOLEAN` | Indica se o lançamento pertence a uma competência já encerrada (imutável). | `NOT NULL`, Default: `false` |
| `registrado_por` | `VARCHAR(150)` | Autor do registro (`SISTEMA_APURACAO` ou login do RH nas compensações). | `NULLABLE` |
| `registrado_em` | `TIMESTAMP` | Data/hora da gravação do lançamento. | `NOT NULL` |

---

## 25. `fechamento_banco_horas`
**Descrição:** Snapshot mensal congelado do saldo do banco de horas do colaborador por competência (`YYYY-MM`), garantindo imutabilidade histórica mesmo se regras futuras mudarem.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do fechamento de banco de horas. | `PK` |
| `funcionario_id` | `UUID` | Colaborador titular. | `FK (funcionario.id)`, `NOT NULL`, `UNIQUE (funcionario_id, mes_referencia)`, Indexado (`idx_bh_fechamento_func`) |
| `mes_referencia` | `VARCHAR(7)` | Competência fechada no formato `YYYY-MM`. | `NOT NULL`, `UNIQUE (funcionario_id, mes_referencia)` |
| `saldo_anterior_minutos` | `INTEGER` | Saldo acumulado trazido até o início da competência. | `NOT NULL`, Default: `0` |
| `creditos_periodo_minutos` | `INTEGER` | Soma dos créditos de horas extras gerados na competência. | `NOT NULL`, Default: `0` |
| `debitos_periodo_minutos` | `INTEGER` | Soma dos débitos (atrasos/faltas) gerados na competência. | `NOT NULL`, Default: `0` |
| `compensacoes_periodo_minutos` | `INTEGER` | Soma das compensações/folgas abatidas na competência. | `NOT NULL`, Default: `0` |
| `saldo_final_congelado_minutos` | `INTEGER` | Saldo final travado no encerramento da competência. | `NOT NULL`, Default: `0` |
| `fechado_em` | `TIMESTAMP` | Data/hora do congelamento da competência. | `NOT NULL` |
| `fechado_por` | `VARCHAR(150)` | Usuário de RH que executou o fechamento. | `NULLABLE` |

---

## 26. `fechamento_periodo`
**Descrição:** Controle de bloqueio e encerramento mensal da competência (`YYYY-MM`) da empresa. Quando `status` está `CONCLUIDO` ou `CONCLUIDO_COM_PENDENCIAS`, bloqueia edições retroativas naquele período e habilita a exportação oficial para a folha.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do fechamento da competência. | `PK` |
| `empresa_id` | `UUID` | Empresa cuja competência foi fechada. | `FK (empresa.id)`, `NOT NULL`, `UNIQUE (empresa_id, mes_referencia)` |
| `mes_referencia` | `VARCHAR(7)` | Competência no formato `YYYY-MM`. | `NOT NULL`, `UNIQUE (empresa_id, mes_referencia)` |
| `data_inicio` | `DATE` | Primeiro dia da competência. | `NOT NULL` |
| `data_fim` | `DATE` | Último dia da competência. | `NOT NULL` |
| `data_fechamento` | `TIMESTAMP` | Data/hora do último fechamento executado. | `NOT NULL` |
| `fechado_por` | `VARCHAR(255)` | Identificação nominal/usuário de RH que executou a operação. | `NOT NULL` |
| `fechado_por_id` | `UUID` | ID do funcionário de RH que executou a operação. | `NULLABLE` |
| `status` | `VARCHAR(40)` | Estado da competência (`StatusFechamentoPeriodo`). | `NOT NULL` (`ABERTO`, `EM_ANDAMENTO`, `CONCLUIDO`, `CONCLUIDO_COM_PENDENCIAS`) |
| `pendencias_abertas_no_fechamento` | `INTEGER` | Quantidade de inconsistências abertas no instante do fechamento. | `NOT NULL`, Default: `0` |
| `aviso_pendencias` | `TEXT` | Ressalva detalhada gravada quando o RH fecha o período com pendências mediante ciência. | `NULLABLE` |

---

## 27. `historico_fechamento_periodo`
**Descrição:** Trilha de auditoria imutável de todas as transições (`FECHAMENTO`, `FECHAMENTO_COM_PENDENCIAS` e `REABERTURA`) realizadas sobre uma competência em `fechamento_periodo`.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do evento de auditoria. | `PK` |
| `fechamento_periodo_id` | `UUID` | Competência à qual o evento pertence. | `FK (fechamento_periodo.id)`, `NOT NULL`, Indexado (`idx_hist_fechamento_periodo`) |
| `acao` | `VARCHAR(40)` | Operação realizada (`AcaoFechamentoPeriodo`). | `NOT NULL` (`FECHAMENTO`, `FECHAMENTO_COM_PENDENCIAS`, `REABERTURA`) |
| `autor` | `VARCHAR(150)` | Nome/login do usuário de RH que executou a ação. | `NOT NULL` |
| `autor_id` | `UUID` | ID do funcionário de RH que executou a ação. | `NULLABLE` |
| `data_hora` | `TIMESTAMP` | Data/hora exata do evento. | `NOT NULL`, Indexado (`idx_hist_fechamento_periodo`) |
| `motivo` | `TEXT` | Justificativa obrigatória na reabertura ou ressalva no fechamento com pendências. | `NULLABLE` |

---

## 28. `historico_exportacao_folha`
**Descrição:** Registro de auditoria das exportações determinísticas geradas para o sistema de Folha de Pagamento (CSV/JSON), contendo o hash criptográfico `SHA-256` do arquivo emitido.

| Coluna | Tipo PostgreSQL | Descrição | Restrições / Observações |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Identificador único da exportação. | `PK` |
| `empresa_id` | `UUID` | Empresa titular da folha exportada. | `FK (empresa.id)`, `NOT NULL` |
| `competencia` | `VARCHAR(7)` | Competência exportada no formato `YYYY-MM` (exige período fechado). | `NOT NULL` |
| `autor` | `VARCHAR(255)` | Usuário de RH que gerou a exportação. | `NOT NULL` |
| `autor_id` | `UUID` | ID do funcionário de RH que gerou a exportação. | `NULLABLE` |
| `data_hora` | `TIMESTAMP` | Data/hora da geração do arquivo. | `NOT NULL` |
| `nome_arquivo` | `VARCHAR(255)` | Nome padronizado do arquivo gerado (ex: `folha-ponto-vertice-2026-11.csv`). | `NOT NULL` |
| `quantidade_colaboradores` | `INTEGER` | Quantidade de colaboradores incluídos no lote exportado. | `NOT NULL`, Default: `0` |
| `hash_sha256` | `VARCHAR(64)` | Assinatura `SHA-256` hexadecimal do conteúdo exportado para verificação de integridade. | `NOT NULL` |
