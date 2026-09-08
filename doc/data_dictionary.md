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
| `pin_hash` | String | PIN numérico (4-6 dígitos) para autenticação rápida na estação. | Not Null |
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
| `empresa_id` | UUID / Int | Denormalizado, para a unique constraint de `num_fabricacao`. | FK, Not Null |
| `local_trabalho_id` | UUID / Int | Onde ele está fisicamente instalado. | FK, Not Null |
<<<<<<< Updated upstream
| `tipo` | Enum | Define a origem da batida. | `RELOGIO_AFD`, `ESTACAO_WEB` |
| `identificacao` | String | Ex: "Corredor B", "Recepção Central #04". | Not Null |
| `status` | Enum | Estado do terminal. | `ATIVO`, `INATIVO`, `MANUTENCAO`, `REVOGADO` | Not Null |
=======
| `tipo` | Enum | Define a origem da batida. | `RELOGIO`, `ESTACAO` |
| `identificacao` | String | Nome/token do equipamento (Ex: "Corredor B", "REP-004"). | Not Null |
| `num_fabricacao` | String | Serial do fabricante. Obrigatório só se `tipo = RELOGIO` (validado no service, não no banco). | Nullable, **Unique por empresa_id** |
| `status` | Enum | Permite desativar sem apagar marcações já vindas dele. | `ATIVO`, `INATIVO` |

## ESTAÇÃO
**Descrição:** Subtipo de `EQUIPAMENTO` para quando `tipo = ESTACAO` — carrega os dados de autenticação do terminal (WebAuthn/attestation), separados da autenticação do funcionário.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `equipamento_id` | UUID / Int | Referência ao equipamento pai. | PK e FK |
>>>>>>> Stashed changes
| `credential_id` | String | ID de credencial WebAuthn do terminal. | Nullable |
| `public_key` | String | Chave pública criptográfica do terminal. | Nullable |
| `created_at` | Timestamp | Quando foi criado. | Not Null |
| `updated_at` | Timestamp | Quando foi atualizado | Nullable |
| `revoked_at` | Timestamp | Quando foi revogado. | Nullable |
| `created_by` | UUID/Int | Por quem foi criado. | FK, Not Null |

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

## PONTO
**Descrição:** Armazena os registros brutos e individuais das batidas (entrada ou saída), incluindo informações de auditoria.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | ID individual da batida. | PK |
| `funcionario_id` | UUID / Int | Quem bateu. | FK, Not Null |
| `data_hora` | Timestamp | Momento exato do registro. | Not Null |
| `origem` | Enum | De onde veio a informação. | `ESTACAO`, `ARQUIVO_IMPORTADO`, `AJUSTE_MANUAL` |
| `equipamento_id` | UUID / Int | Aparelho físico ou estação web utilizada. | FK, Nullable |
| `arquivo_importado_id` | UUID / Int | De qual importação essa batida veio, se `origem = ARQUIVO_IMPORTADO`. | FK, Nullable |
| `pedido_ajuste_id` | UUID / Int | Pedido que originou esta batida, se `origem = AJUSTE_MANUAL`. | FK, Nullable |
| `foto_url` | String | URL do bucket, caso a estação exija foto. | Nullable |
| `latitude_recebida` | Decimal | Registrado no momento via browser. | Nullable |
| `longitude_recebida` | Decimal | Registrado no momento via browser. | Nullable |
| `ip_recebido` | String | IP real no momento da requisição. | Nullable |

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

## ARQUIVO_IMPORTADO
**Descrição:** Histórico de auditoria e controle do processamento de arquivos AFD do relógio de ponto.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Registro de upload de AFD do relógio. | PK |
| `empresa_id` | UUID / Int | Empresa pertinente. | FK, Not Null |
| `nome_arquivo` | String | Nome original do `.txt`. | Not Null |
| `hash_arquivo` | String | MD5/SHA256 para evitar importação duplicada. | Not Null, Unique |
| `importado_por` | UUID / Int | Funcionário (RH) que subiu o arquivo. | FK, Not Null |
| `qtd_linhas_lidas` | Int | Estatística de processamento. | Default: `0` |
| `qtd_linhas_rejeitadas` | Int | Estatística de erros do arquivo. | Default: `0` |

---

## INCONSISTENCIA
**Descrição:** Alertas gerados **automaticamente** pelo sistema — nunca criados por um funcionário — indicando anomalias nos pontos.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Gerada automaticamente pelo sistema. | PK |
| `funcionario_id` | UUID / Int | Afetado pela inconsistência. | FK, Not Null |
| `par_ponto_id` | UUID / Int | A qual par a engine achou erro. | FK, Nullable |
| `tipo` | Enum | O motivo do alerta. | `DUPLICIDADE`, `BATIDA_ORFA`, `IMPAR`, `IP_DIVERGENTE`, `GEO_DIVERGENTE` |
| `status` | Enum | Estado da pendência. | `PENDENTE`, `RESOLVIDA` |
| `pedido_ajuste_id` | UUID / Int | Preenchido só se a resolução passou por um pedido de ajuste aprovado. | FK, Nullable |
| `tratado_por` | UUID / Int | Quem no RH corrigiu. | FK, Nullable |
| `tratado_em` | Timestamp | Quando foi corrigido. | Nullable |

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
**Descrição:** Registra o encerramento do ciclo de competência, impedindo modificações em dados antigos.

| Coluna | Tipo | Descrição | Observações |
| :--- | :--- | :--- | :--- |
| `id` | UUID / Int | Tranca o mês. | PK |
| `empresa_id` | UUID / Int | Empresa sendo fechada. | FK, Not Null |
| `mes_referencia` | String | Formato YYYY-MM. | Not Null |
| `data_fechamento` | Timestamp | Data exata da ação. | Not Null |
| `fechado_por` | UUID / Int | Qual RH executou. | FK, Not Null |
| `status` | Enum | Evita alteração de pontos antigos. | `EM_ANDAMENTO`, `CONCLUIDO` |
