# TODO: T15 · Ocorrências e Abonos

## 1. Banco de Dados & Modelagem (PostgreSQL / JPA)
- [x] **Criar Enum `TipoAbonoOcorrencia`:**
    - `INTEGRAL` (abona 100% das horas previstas do dia)
    - `PARCIAL` (abona uma quantidade específica de minutos ou percentual do dia, convivendo com batidas do outro turno)
    - `APENAS_REGISTRO` (não abona horas; apenas documenta o evento no espelho, ex.: feriado trabalhado)
- [x] **Criar Entidade `TipoOcorrencia`:**
    - `id` (Long / UUID)
    - `nome` (String, único, ex.: "Atestado Médico", "Férias", "Licença", "Falta Justificada", "Feriado Trabalhado")
    - `descricao` (String, opcional)
    - `tipoAbono` (`TipoAbonoOcorrencia`, obrigatório)
    - `minutosAbonoPadrao` (Integer, nullable, usado como default quando `tipoAbono == PARCIAL`)
    - `exigeAnexo` (Boolean, default `false`)
    - `ativo` (Boolean, default `true`)
- [x] **Criar Entidade `OcorrenciaColaborador`:**
    - `id` (Long / UUID)
    - `funcionario` (`@ManyToOne Funcionario`, obrigatório)
    - `tipoOcorrencia` (`@ManyToOne TipoOcorrencia`, obrigatório)
    - `dataInicio` (LocalDate, obrigatório)
    - `dataFim` (LocalDate, obrigatório)
    - `minutosAbonadosPorDia` (Integer, nullable — preenchido quando `tipoAbono == PARCIAL`, ex.: `240` min para meio período)
    - `observacao` (String, opcional)
    - **Metadados do Anexo:**
        - `nomeArquivoAnexo` (String, nullable)
        - `caminhoOuUrlAnexo` (String, nullable)
        - `contentTypeAnexo` (String, nullable)
    - **Auditoria:**
        - `lancadoPor` (String / Usuário RH)
        - `lancadoEm` (LocalDateTime)
- [x] **Ajuste na Entidade `ApuracaoDiaria` (Integração T14):**
    - [x] Adicionar `minutosAbonados` (Integer, default `0`) e `ocorrenciaId` (opcional, FK para referência rápida no espelho).

---

## 2. Backend — Regras de Negócio e Integração com Apuração (Spring Boot)
- [x] **CRUD de Tipos de Ocorrência (`TipoOcorrenciaService` / `TipoOcorrenciaController`):**
    - [x] `POST /api/tipos-ocorrencia`: Cadastrar novo tipo definindo regra de abono (`INTEGRAL`, `PARCIAL`, `APENAS_REGISTRO`).
    - [x] `GET /api/tipos-ocorrencia`: Listar tipos cadastrados (ativos/todos).
    - [x] `PUT /api/tipos-ocorrencia/{id}`: Editar configuração de abono ou status ativo.
- [x] **Validações no Lançamento de Ocorrência (`OcorrenciaService`):**
    - [x] Validar `dataFim >= dataInicio`.
    - [x] **Bloqueio de Período Fechado:** Verificar no `FechamentoPeriodoRepository` se qualquer data no intervalo `[dataInicio, dataFim]` pertence a uma competência já fechada (`CONCLUIDO` ou `CONCLUIDO_COM_PENDENCIAS`). Se sim, recusar com HTTP `409 Conflict` / `422 Unprocessable Entity`.
    - [x] **Bloqueio de Sobreposição:** Consultar se já existe ocorrência ativa para o mesmo `funcionarioId` onde `existente.dataInicio <= nova.dataFim` AND `existente.dataFim >= nova.dataInicio`. Se houver colisão de datas, recusar com mensagem clara indicando o período conflitante.
- [x] **Upload e Download de Anexo (`StorageService` / `OcorrenciaController`):**
    - [x] Aceitar upload via `multipart/form-data` (PDF, JPG, PNG) no lançamento da ocorrência.
    - [x] Endpoint `GET /api/ocorrencias/{id}/anexo` para visualização/download do documento comprobatório pelo RH.
- [x] **Integração com o Motor de Apuração Diária (`ApuracaoDiariaService` - T14):**
    - [x] Ao apurar um dia, buscar ocorrências vigentes que cubram a `dataReferencia`.
    - [x] **Se `tipoAbono == INTEGRAL`:**
        - Dia não é marcado como `FALTA`.
        - `minutosAbonados = minutosPrevistos`; status atualizado para `ABONADO` (ou `APURADO` se houver batidas adicionais).
    - [x] **Se `tipoAbono == PARCIAL`:**
        - Somar `minutosTrabalhados` (dos pares batidos no outro turno) + `minutosAbonados`.
        - Abater `minutosAbonados` do cálculo de `minutosAtraso` / `minutosSaidaAntecipada` / `minutosFalta` (ex.: jornada de 8h com 4h de atestado + 4h trabalhadas = saldo zerado, sem falta e sem atraso).
    - [x] **Se `tipoAbono == APENAS_REGISTRO`:**
        - Não altera o saldo de horas; mantém o registro informativo no dia.
    - [x] Disparar recálculo automático de todos os dias do intervalo `[dataInicio, dataFim]` imediatamente após salvar ou remover uma ocorrência.
- [x] **Endpoints de Lançamento (`OcorrenciaController`):**
    - [x] `POST /api/ocorrencias`: Lançar ocorrência com suporte a anexo.
    - [x] `GET /api/ocorrencias`: Listar ocorrências com filtros por colaborador, tipo e período.
    - [x] `DELETE /api/ocorrencias/{id}`: Cancelar/remover ocorrência (bloqueado se período fechado) e recalcular os dias afetados.

---

## 3. Frontend (React)
- [x] **Configuração de Tipos de Ocorrência (Painel RH):**
    - [x] Tela/modal para criar e gerenciar tipos de ocorrência (Nome, Regra de Abono: Integral / Parcial / Apenas Registro, Minutos de abono se parcial).
- [x] **Lançamento de Ocorrência por Colaborador:**
    - [x] Formulário com seleção de Colaborador, Tipo de Ocorrência, Data Inicial, Data Final, campo condicional de horas/minutos abonados (quando parcial), Observação e Upload de Anexo (PDF/Imagem).
    - [x] Exibição clara dos alertas retornados pela API em caso de período sobreposto ou mês já fechado.
- [x] **Integração Visual no Espelho de Ponto:**
    - [x] Exibir badge da ocorrência na linha do dia (ex.: `Atestado Médico - Abono Integral` ou `Atestado Parcial (04:00)`).
    - [x] Mostrar na soma do dia tanto as horas trabalhadas pelos pares quanto as horas abonadas pela ocorrência.
    - [x] Ícone/link para abrir ou baixar o documento anexado.

---

## 4. Testes e Validações (QA)
- [x] **Teste de Abono Integral:** Lançar férias ou atestado de 3 dias para colaborador sem batidas -> nenhum dos 3 dias deve ser apurado como `FALTA`.
- [x] **Teste de Abono Parcial (Meio Período):** Jornada de 8h, atestado parcial de 4h + 1 par de batidas de 4h -> dia apurado sem falta e sem atraso (`4h trabalhadas + 4h abonadas = 8h`).
- [x] **Teste de Sobreposição:** Tentar lançar ocorrência de `10/11` a `15/11` quando já existe outra de `14/11` a `18/11` para o mesmo colaborador -> operação recusada.
- [x] **Teste de Período Fechado:** Tentar lançar ocorrência em competência já fechada -> operação recusada.
- [x] **Teste de Anexo:** Enviar arquivo PDF/imagem no lançamento e validar recuperação via endpoint de download.