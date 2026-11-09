# Dicionário de Dados - Sistema Ponto Certo

## EMPRESA
**Descrição:** Armazena os dados das empresas (clientes do sistema), suportando o modelo multi-tenant e configurações visuais (white-label).

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Identificador único da empresa. | PK |
| `razao_social` | String | Razão social da empresa. | Not Null |
| `subdominio` | String | Subdomínio para acesso (ex: empresa.pontocerto.com). | Not Null, Unique |
| `cnpj` | String | CNPJ da empresa. | Not Null, Unique |
| `logo_url` | String | Link para a logo armazenada em bucket (S3, etc). | Nullable |
| `endereco` | String | Endereço sede. | Nullable |
| `config_whitelabel` | JSON | Campos para cores e customizações visuais. | Default: `{}` |

---

## FUNCIONARIO
**Descrição:** Entidade central. `GESTOR`, `COLABORADOR` e `RH` **não são tabelas separadas** — são valores do campo `perfil_acesso` desta mesma entidade. Um funcionário que é gestor continua sendo a mesma linha desta tabela, só com outros funcionários apontando `gestor_id` para ele.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Identificador único do funcionário. | PK |
| `empresa_id` | UUID / Int | Vínculo com a empresa (Multi-tenant). | FK, Not Null |
| `gestor_id` | UUID / Int | Auto-relacionamento: quem é o gestor deste funcionário. | FK, Nullable |
| `perfil_acesso` | Enum | Nível de acesso. | `COLABORADOR`, `GESTOR`, `RH` |
| `matricula` | String | Matrícula interna da empresa. | Not Null, **Unique por empresa_id** |
| `cpf` | String | CPF para validação de dígito verificador. | Not Null, **Unique por empresa_id** |
| `pis_pasep` | String | PIS/PASEP para espelho de ponto oficial. | Not Null, **Unique por empresa_id** |
| `nome_completo` | String | Nome do funcionário. | Not Null |
| `email` | String | Email de acesso e comunicação. | Not Null, **Unique por empresa_id** |
| `telefone` | String | Contato principal (pode ser JSON se for array). | Nullable |
| `usuario` | String | Login do sistema. | Not Null, **Unique por empresa_id** |
| `senha_hash` | String | Senha criptografada (login web). | Not Null |
| `jornada_id` | UUID / Int | Vínculo com a tabela de jornadas. | FK, **Nullable** (JORNADA fora do escopo de T1/T2 — vira Not Null quando a tarefa de jornada entrar) |
| `cargo` | String | Função exercida. | Not Null |
| `data_admissao` | Date | Data de início. | Not Null |
| `data_desligamento` | Date | Data de rescisão (se preenchido, inativa o usuário). | Nullable |

> **Nota:** CPF, PIS/PASEP, email e usuário eram unique globais na versão anterior — corrigido para unique *por empresa*, já que em multi-tenant duas empresas-cliente distintas não têm relação entre si e não deveriam colidir por acaso.

---

## LOCAL_TRABALHO
**Descrição:** Cadastros dos locais físicos onde os funcionários da empresa exercem suas atividades, permitindo validação geográfica (GPS/IP).

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Identificador único. | PK |
| `empresa_id` | UUID / Int | Empresa dona do local. | FK, Not Null |
| `nome` | String | Ex: Sede, Filial Centro, Obra X. | Not Null |
| `latitude` | Decimal | Coordenada Y. | Nullable (Precisão 8,6) |
| `longitude` | Decimal | Coordenada X. | Nullable (Precisão 9,6) |
| `raio_metros` | Int | Raio permitido para bater ponto via GPS. | Default: `0` |
| `ip_esperado` | String | IP da rede local para validação de segurança. | Nullable |
| `endereco` | String | Dados de localização textual. | Not Null |
| `municipio` | String | Município do local. | Not Null |
| `uf` | String | Estado do local. | Not Null |

---

## EQUIPAMENTO
**Descrição:** Cadastro geral de relógios físicos (AFD) e estações web instaladas nos locais de trabalho. `empresa_id` é denormalizado aqui (além de vir via `local_trabalho_id`) para permitir a constraint de unicidade do número de fabricação por empresa direto no banco. `status` mora na classe base — tanto relógio quanto estação precisam poder ser desativados, conforme T02.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Identificador do equipamento. | PK |
| `codigo` | String | Código único do equipamento `EQP-0000`. | Not Null |
| `empresa_id` | UUID / Int | Denormalizado, para a unique constraint de `num_fabricacao`. | FK, Not Null |
| `local_trabalho_id` | UUID / Int | Onde ele está fisicamente instalado. | FK, Not Null |
| `tipo` | Enum | Define a origem da batida. | `RELOGIO`, `ESTACAO` | Not Null |
| `identificacao` | String | Ex: "Corredor B", "Recepção Central #04". | Not Null |
| `status` | Enum | Estado do terminal. | `ATIVO`, `INATIVO`, `MANUTENCAO` | Not Null |
| `created_by` | UUID/Int | Por quem foi criado. | FK, Not Null |
| `updated_by` | UUID/Int | Por quem foi atualizado. | FK, Nullable |
| `created_at` | Timestamp | Quando foi criado. | Not Null |
| `updated_at` | Timestamp | Quando foi atualizado | Nullable |

## ESTAÇÃO
**Descrição:** Subtipo de `EQUIPAMENTO` para quando `tipo = ESTACAO` — carrega os dados de autenticação do terminal (WebAuthn/attestation), separados da autenticação do funcionário.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `equipamento_id` | UUID / Int | Referência ao equipamento pai. | PK e FK |
| `credential_id` | String | ID de credencial WebAuthn do terminal. | Nullable |
| `public_key` | String | Chave pública criptográfica do terminal. | Nullable |
| `status` | Enum | Estado da Estação. | `ATIVO`, `PENDENTE`, `REVOGADO` | Not Null |
| `revoked_at` | Timestamp | Quando foi revogado. | Nullable |
| `revoked_by` | UUID/Int | Por quem foi revogado. | FK, Nullable |

## RELOGIO
**Descrição:** Subtipo de `EQUIPAMENTO` para quando `tipo = RELOGIO` — carrega o número do equipamento e quantas linhas foram importadas.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `equipamento_id` | UUID / Int | Referência ao equipamento pai. | PK e FK |
| `num_fabricacao` | String | Serial do fabricante.| Not Null, **Unique por empresa_id** |
| `linhas_importadas` | Long | Quantas linhas já foram importadas desse equipamento, dado não é perdido mesmo se status for `INATIVO` | Nullable |

---

## JORNADA
**Descrição:** Define os horários previstos, dias de trabalho e as regras de tolerância para os colaboradores vinculados.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Identificador. | PK |
| `empresa_id` | UUID / Int | Empresa dona da jornada. | FK, Not Null |
| `descricao` | String | Nome da jornada (Ex: "6x1", "Administrativo 8h-18h"). | Not Null |
| `hora_entrada` | Time | Horário previsto de chegada. | Not Null |
| `hora_saida` | Time | Horário previsto de saída. | Not Null |
| `intervalo_inicio` | Time | Início da pausa. | Nullable |
| `intervalo_fim` | Time | Fim da pausa. | Nullable |
| `tolerancia_minutos` | Int | Minutos de tolerância para atrasos/extras. | Default: `10` |
| `dias_semana` | JSON/String | Dias aplicáveis (Ex: `[1,2,3,4,5]` = Seg a Sex). | Not Null |

---

## FERIADO
**Descrição:** Datas em que não se espera trabalho. Se `empresa_id` for nulo, é feriado nacional válido pra todo o sistema; caso contrário, aplica-se à empresa (e, se municipal, ao `municipio` informado).

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Identificador. | PK |
| `empresa_id` | UUID / Int | Nulo = feriado nacional do sistema. | FK, Nullable |
| `descricao` | String | Nome do feriado. | Not Null |
| `data` | Date | Dia em que ocorre. | Not Null |
| `alcance` | Enum | Nível da restrição. | `NACIONAL`, `ESTADUAL`, `MUNICIPAL` |
| `uf` | String | Estado (se estadual ou municipal). | Nullable |
| `municipio` | String | Município (se municipal). | Nullable |

## PONTO_FACULTATIVO
**Descrição:** Dia em que a jornada continua valendo normalmente, mas ausência não gera falta (diferente de feriado — quem trabalha, trabalha normal).

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Identificador. | PK |
| `descricao` | String | Nome da data. | Not Null |
| `data` | Date | Dia em que ocorre. | Not Null |
| `alcance` | Enum | Nível da restrição. | `NACIONAL`, `ESTADUAL`, `MUNICIPAL` |
| `uf` | String | Estado (se aplicável). | Nullable |
| `municipio` | String | Município (se municipal). | Nullable |

---

## BATIDA
**Descrição:** Armazena os registros brutos e individuais das batidas de ponto (entrada ou saída), incluindo auditoria, tratamento de órfãs/duplicadas e geração sequencial de NSR. **Regra Portaria 671:** Os campos originais da marcação são estritamente imutáveis (`updatable = false`), permitindo apenas atualização dos metadados de vínculo/descarte auditado pelo RH.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID | Identificador único da batida. | PK, `updatable = false` |
| `nsr` | BigInt / Long | Número Sequencial de Registro (Portaria 671). | Not Null, gerado via sequence `seq_nsr_batida`, Unique composto `(equipamento_id, nsr)` |
| `funcionario_id` | UUID | Vínculo com o colaborador que realizou a batida. | FK, **Nullable** (nulo enquanto `ORFA_PENDENTE`) |
| `identificador_original` | String | Número bruto lido no arquivo AFD (PIS, CPF ou Matrícula). | Nullable, `updatable = false` |
| `data_hora` | Timestamp | Momento exato do registro. | Not Null, `updatable = false` |
| `tipo_registro` | Enum | Tipo do registro. | `ENTRADA`, `SAIDA`, Not Null, `updatable = false` |
| `status_batida` | Enum | Estado da marcação para apuração. | `NORMAL`, `ORFA_PENDENTE`, `VINCULADA`, `DESCARTADA` |
| `tipo_origem` | Enum | De onde veio a informação. | `ESTACAO`, `ARQUIVO_IMPORTADO`, `AJUSTE_MANUAL`, `APLICATIVO_WEB` | Not Null, `updatable = false` |
| `vinculado_por` | String | Autor do RH que vinculou ou descartou a batida. | Nullable |
| `data_vinculo` | Timestamp | Data/hora do tratamento manual na batida. | Nullable |
| `motivo_vinculo` | String | Justificativa auditável do vínculo ou descarte. | Nullable |
| `equipamento_id` | UUID | Aparelho físico ou estação web utilizada (se aplicável). | FK, Nullable, `updatable = false` |
| `arquivo_importado_id` | UUID | De qual importação essa batida veio, se `tipo_origem = ARQUIVO_IMPORTADO`. | FK lógica (`historico_importacao.id`), Nullable, `updatable = false` |
| `pedido_ajuste_id` | UUID | Pedido que originou esta batida, se `tipo_origem = AJUSTE_MANUAL`. | FK lógica, Nullable, `updatable = false` |
| `foto_url` | String | URL do bucket, caso a estação exija foto biométrica. | Nullable, `updatable = false` |
| `latitude_recebida` | Decimal | Coordenada Y registrada no momento via browser/app. | Nullable, `updatable = false` |
| `longitude_recebida` | Decimal | Coordenada X registrada no momento via browser/app. | Nullable, `updatable = false` |
| `ip_recebido` | String | IP real do cliente no momento da requisição. | Nullable, `updatable = false` |

---

## PAR_PONTO
**Descrição:** Relaciona as batidas de entrada e saída correspondentes para facilitar o cálculo do tempo trabalhado.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | O "casamento" entre uma entrada e uma saída. | PK |
| `funcionario_id` | UUID / Int | Para indexação rápida da query diária. | FK, Not Null |
| `data_referencia` | Date | A qual dia de trabalho esse par pertence. | Not Null |
| `entrada_ponto_id` | UUID / Int | FK para a tabela PONTO. | FK, Not Null |
| `saida_ponto_id` | UUID / Int | FK para a tabela PONTO. | FK, Nullable (Aberto) |
| `status` | Enum | Estado daquele par. | `ABERTO`, `FECHADO`, `NAO_REGISTRADO` |

---

## AUTH_CHALLENGE
**Descrição:** Desafio criptográfico de curta duração usado para autenticar a estação (o terminal físico) junto ao backend — independente da autenticação do funcionário. O servidor consome atomicamente o challenge após uma validação bem-sucedida.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Identificador. | PK |
| `estacao_id` | UUID / Int | Estação que solicitou o desafio. | FK, Not Null |
| `challenge` | String | Valor do desafio criptográfico. | Not Null |
| `ttl` | Int | Tempo (segundos) que a verificação dura. | Not Null |

---

## PARING_CODE
**Descrição:** Código de pareamento gerado para sincronizar uma estação física com a criada no servidor. O servidor consome atomicamente o código após um pareamento bem-sucedido.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Identificador. | PK |
| `paring_code` | Int | Código de 6 dígitos usado para parear a estação física. | Not Null |
| `equipament_id` | UUID/Int | Id do equipamento que está sendo pareado | FK, Not Null |
| `ttl` | Int | Tempo (segundos) que a verificação dura. | Not Null |

---

## HISTORICO_IMPORTACAO (ARQUIVO_IMPORTADO)
**Descrição:** Histórico de auditoria e controle do processamento de arquivos posicionais AFD do relógio de ponto (T09/T11).

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID | Registro de upload de AFD do relógio. | PK |
| `data_importacao` | Timestamp | Data/hora em que a importação foi processada. | Not Null, `updatable = false` |
| `autor_id` | UUID | Funcionário (RH) que subiu o arquivo. | FK (`funcionario.id`), Nullable |
| `equipamento_id` | UUID | Equipamento de origem selecionado para o lote. | FK (`equipamento.id`), Not Null |
| `nome_arquivo` | String | Nome original do `.txt`. | Not Null |
| `conteudo_arquivo` | Text | Conteúdo bruto integral do arquivo importado para re-download/auditoria. | Nullable |
| `total_lidas` | Int | Total de linhas de marcação (tipo `3`) lidas no arquivo. | Default: `0` |
| `total_validas` | Int | Total de batidas inéditas de colaboradores identificados. | Not Null, Default: `0` |
| `total_repetidas` | Int | Total de batidas ignoradas por duplicidade de `(equipamento, nsr)` ou mesmo horário. | Not Null, Default: `0` |
| `total_recusadas` | Int | Total de batidas sem colaborador identificado (gravadas como `ORFA_PENDENTE`). | Not Null, Default: `0` |
| `status` | Enum | Status do processamento transacional. | `PENDENTE`, `CONCLUIDA`, `FALHA` |

---

## INCONSISTENCIA
**Descrição:** Fila de pendências geradas **automaticamente** pelo sistema toda vez que algo não fecha (T12). Nada é resolvido em silêncio: cada tratamento exige ação registrada (`VINCULAR`, `INCLUIR_MARCACAO_AJUSTE`, `DESCARTAR_DUPLICADA` ou `ABONAR_DIA`), gravando autor, data/hora e motivo, e mantendo a pendência resolvida consultável no histórico.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID | Identificador único da pendência gerada pelo sistema. | PK |
| `empresa_id` | UUID | Empresa à qual a pendência pertence (Multi-tenant). | FK (`empresa.id`), Not Null |
| `tipo` | Enum | Tipo da inconsistência detectada (`TipoInconsistencia`). | `ORFA`, `DIA_IMPAR`, `DUPLICADA_MINUTO`, `FORA_DA_JORNADA`, `SEQUENCIA_INVALIDA`, `EQUIPAMENTO_DESCONHECIDO` |
| `situacao` | Enum | Situação atual na fila (`StatusInconsistencia`). | `ABERTA`, `RESOLVIDA` (Default: `ABERTA`) |
| `funcionario_id` | UUID | Colaborador afetado pela pendência. | FK (`funcionario.id`), **Nullable** (nulo quando `ORFA` ainda não vinculada ou `EQUIPAMENTO_DESCONHECIDO`) |
| `equipamento_id` | UUID | Equipamento relacionado à inconsistência (se conhecido). | FK (`equipamento.id`), Nullable |
| `batida_origem_id` | UUID | Marcação que originou a pendência (ex: batida órfã, entrada sem saída, saída sem entrada). | Referência lógica (`batida.id`), Nullable |
| `batida_secundaria_id` | UUID | Segunda marcação envolvida (ex: batida duplicada no minuto ou marcação de ajuste criada na resolução). | Referência lógica (`batida.id`), Nullable |
| `arquivo_importado_id` | UUID | Histórico da importação AFD que originou a pendência (se aplicável). | Referência lógica (`historico_importacao.id`), Nullable |
| `nome_arquivo` | String | Nome do arquivo `.txt` de relógio que originou a pendência (inclusive para `EQUIPAMENTO_DESCONHECIDO`). | Nullable |
| `identificador_recebido` | String | Identificador bruto não reconhecido (PIS/CPF/Matrícula na `ORFA` ou serial/código no `EQUIPAMENTO_DESCONHECIDO`). | Nullable |
| `data_referencia` | Date | Data de competência/ocorrência para filtro por período e validação de fechamento. | Not Null |
| `data_hora_origem` | Timestamp | Data e hora exatas do evento que originou a pendência. | Not Null |
| `descricao_origem` | Text | Explicação detalhada do que aconteceu e originou a pendência. | Not Null |
| `chave_deduplicacao` | String | Chave lógica para evitar criação duplicada da mesma pendência aberta. | Nullable |
| `criado_em` | Timestamp | Momento em que o sistema detectou e criou a pendência. | Not Null, `updatable = false` |
| `acao_tomada` | Enum | Ação executada pelo RH para resolver (`AcaoTratamento`). | `VINCULAR`, `INCLUIR_MARCACAO_AJUSTE`, `DESCARTAR_DUPLICADA`, `ABONAR_DIA` (Nullable enquanto `ABERTA`) |
| `tratado_por` | String | Identificação nominal/login do usuário de RH que resolveu a pendência. | Nullable |
| `tratado_por_id` | UUID | ID do funcionário do RH que realizou o tratamento. | FK lógica (`funcionario.id`), Nullable |
| `tratado_em` | Timestamp | Data e hora exatas em que o tratamento foi registrado. | Nullable |
| `motivo_tratamento` | Text | Justificativa obrigatória informada pelo RH ao resolver a pendência. | Nullable |
| `detalhe_resolucao` | Text | Resumo do efeito prático da resolução (ex: colaborador vinculado, NSR do ajuste criado, batida descartada ou dia abonado). | Nullable |

---

## PEDIDO_AJUSTE
**Descrição:** Solicitações submetidas **manualmente** pelo colaborador e dependem de aprovação (Gestor/RH).

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Criado manualmente pelo colaborador. | PK |
| `funcionario_id` | UUID / Int | Quem solicitou. | FK, Not Null |
| `data_referencia` | Date | Qual dia quer ajustar. | Not Null |
| `tipo` | Enum | Motivo do pedido. | `ESQUECIMENTO`, `ATESTADO`, `ABONO` |
| `horario_solicitado` | Time | Se for esquecimento, qual hora ele indica. | Nullable |
| `justificativa` | Text | Explicação ou link de anexo (atestado). | Nullable |
| `status` | Enum | Fluxo de aprovação. | `PENDENTE`, `APROVADO`, `REJEITADO` |
| `analisado_por` | UUID / Int | Gestor ou RH que decidiu. | FK, Nullable |
| `analisado_em` | Timestamp | Momento da decisão. | Nullable |

---

## APURACAO_DIARIA
**Descrição:** Consolidação diária: horas trabalhadas, débitos (atrasos/faltas) e créditos (horas extras).

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Resumo do dia calculado. | PK |
| `funcionario_id` | UUID / Int | O dono do dia. | FK, Not Null |
| `data_referencia` | Date | O dia apurado. | Not Null |
| `horas_trabalhadas` | Int | Tempo em minutos. | Default: `0` |
| `horas_atraso` | Int | Débito do dia em minutos. | Default: `0` |
| `horas_extra` | Int | Crédito do dia em minutos. | Default: `0` |
| `status_dia` | Enum | Resultado da consolidação. | `NORMAL`, `INCONSISTENTE`, `FALTA`, `FERIADO`, `FACULTATIVO`, `FOLGA` |
| `par_ponto_id` | UUID / Int | Par de batidas usado no cálculo. | FK, Nullable |

---

## BANCO_HORAS
**Descrição:** Acumulador de saldo em minutos de cada colaborador.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Acumulador do colaborador. | PK |
| `funcionario_id` | UUID / Int | Dono do saldo. | FK, Not Null, Unique |
| `saldo_minutos` | Int | Pode ser negativo. Atualizado via trigger/job. | Default: `0` |
| `ultima_atualizacao` | Timestamp | Última vez que o saldo foi recalculado. | Not Null |

> **Nota:** esta tabela guarda só o saldo corrente, sem histórico por período. Se for necessário provar "qual era o saldo em março" após um fechamento, considerar uma tabela de histórico por `mes_referencia` vinculada a `FECHAMENTO_PERIODO`.

---

## FECHAMENTO_PERIODO
**Descrição:** Registra o encerramento do ciclo de competência. Não permite fechar o período com pendência aberta na fila sem ciência do RH — ou bloqueia o fechamento, ou fecha mediante confirmação gravando o aviso detalhado das pendências que ficaram abertas (T12).

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID | Identificador único do fechamento. | PK |
| `empresa_id` | UUID | Empresa sendo fechada. | FK (`empresa.id`), Not Null |
| `mes_referencia` | String | Competência no formato `YYYY-MM`. | Not Null |
| `data_inicio` | Date | Data inicial do período apurado. | Not Null |
| `data_fim` | Date | Data final do período apurado. | Not Null |
| `data_fechamento` | Timestamp | Data e hora exatas da execução do fechamento. | Not Null |
| `fechado_por` | String | Identificação do usuário de RH que executou o fechamento. | Not Null |
| `fechado_por_id` | UUID | ID do funcionário do RH que executou o fechamento. | FK lógica (`funcionario.id`), Nullable |
| `status` | Enum | Resultado do fechamento (`StatusFechamentoPeriodo`). | `EM_ANDAMENTO`, `CONCLUIDO`, `CONCLUIDO_COM_PENDENCIAS` |
| `pendencias_abertas_no_fechamento` | Int | Quantidade de inconsistências que estavam abertas no período no ato do fechamento. | Not Null, Default: `0` |
| `aviso_pendencias` | Text | Aviso registrado detalhando o que ficou pendente por tipo quando o RH opta por fechar com ressalva. | Nullable |
