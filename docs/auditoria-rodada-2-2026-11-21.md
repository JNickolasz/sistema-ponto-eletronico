# Auditoria Técnica de Segurança e Qualidade — Rodada 2 (Ponto Certo)

- **Data:** 2026-11-21
- **Alvo:** Sistema *Ponto Certo* (`pbdProject` — Spring Boot 4.1.1 / Java 21 + React/Vite + PostgreSQL/Neon)
- **Commit auditado:** `d19d59b` (branch `main`, após correções da Rodada 1)
- **Modo de execução:** Somente leitura (nenhum código, migration, config ou dado foi alterado)
- **Perspectiva:** Atacante autenticado com privilégio baixo (`COLABORADOR`, `GESTOR` ou `RH` de um tenant A mirando o tenant B ou recursos restritos do mesmo tenant)

---

## Etapa 0 — Modelo de Ameaça

### O que o sistema faz
O *Ponto Certo* é um sistema SaaS multi-tenant de controle de ponto eletrônico e apuração trabalhista (aderente à Portaria 671/MTP), cobrindo cadastro de empresas e locais de trabalho, gestão de colaboradores, jornadas/escalas, estações de ponto web e relógios físicos (importação posicional/AFD), tratamento de inconsistências, ocorrências/atestados médicos, banco de horas, fechamento de competência e exportação para folha de pagamento.

### Perfis de Acesso (Roles) — Pode vs. Não Pode

| Perfil | Autenticação | O que PODE fazer | O que NÃO PODE fazer |
| :--- | :--- | :--- | :--- |
| **Anônimo** | Nenhuma | Login (`POST /api/auth/login`), registrar batida na estação pareada com matrícula/CPF + senha (`POST /api/estacao/{id}/bater`), consultar status/pareamento de estação. | Acessar qualquer dado interno, criar funcionários, listar empresas ou alterar configurações. |
| **COLABORADOR** (`ROLE_COLABORADOR`) | JWT (Bearer) | Ver próprio espelho de ponto, próprias batidas/comprovantes, próprio extrato de banco de horas, próprias ocorrências, abrir e listar próprias solicitações de ajuste. | Ver/baixar dados, batidas, extratos ou atestados médicos de colegas; aprovar solicitações; fechar períodos; acessar dados de outro tenant. |
| **GESTOR** (`ROLE_GESTOR`) | JWT (Bearer) | Tudo do Colaborador + consultar espelhos, inconsistências, solicitações de ajuste, banco de horas e relatórios **exclusivamente dos liderados de sua equipe** (subordinados diretos/indiretos no mesmo tenant) e aprovar/reprovar ajustes da equipe. | Acessar dados de colaboradores fora de sua hierarquia; gerenciar cadastros globais (empresas, feriados, locais, equipamentos); desligar/excluir funcionários; fechar competência; acessar outro tenant. |
| **RH** (`ROLE_RH`) | JWT (Bearer) | Gestão completa **exclusivamente dentro da sua própria empresa (`empresaId`)**: funcionários, locais, equipamentos, jornadas, escalas, regras de apuração, ocorrências, importações, banco de horas, fechamento/reabertura de período e exportação de folha. | Listar, ler, alterar, desligar ou excluir recursos de **outra empresa** no SaaS; alterar/inativar feriados ou pontos facultativos globais/de outros tenants; cadastrar novas empresas. |
| **ADMIN** (`X-Admin-Key`) | Header estático `X-Admin-Key` | Provisionar empresas (`POST /api/admin/empresas`), criar RH inicial, listar todas as empresas no painel administrativo. | (Superadmin da plataforma SaaS — chave nunca deve vazar no bundle do frontend). |

### Os 5 Ativos Mais Valiosos
1. **Integridade das batidas e apurações fechadas (Portaria 671):** Registros de ponto, comprovantes e competências fechadas têm valor jurídico-trabalhista e determinam diretamente o pagamento de salários, horas extras e descontos.
2. **Isolamento Multi-Tenant (`empresa_id`):** Vazamento de dados ou sabotagem cruzada entre empresas clientes destrói o modelo SaaS e gera passivo contratual e de LGPD.
3. **Dados Pessoais e Sensíveis de Saúde (LGPD):** Atestados médicos anexados em ocorrências (`OcorrenciaColaborador`), CPFs, matrículas, PIS/PASEP e histórico de faltas/afastamentos.
4. **Credenciais e Chaves de Controle (`JWT_SECRET`, `ADMIN_KEY`, senhas):** Comprometimento da `ADMIN_KEY` ou `JWT_SECRET` permite tomada total do SaaS (forjar tokens ou provisionar tenants/RHs).
5. **Exportação de Folha e Saldo de Banco de Horas:** Alteração indevida de horas extras, compensações ou fechamentos gera impacto financeiro direto.

### Os 5 Fluxos de Negócio Mais Críticos e suas Invariantes
1. **Isolamento de Tenant:** Nenhuma operação autenticada com JWT (mesmo `ROLE_RH`) pode ler, referenciar, alterar, inativar ou excluir registros pertencentes a outro `empresa_id` (ou registros globais compartilhados como feriados nacionais).
2. **Imutabilidade de Competência Fechada:** Uma vez que o período `YYYY-MM` está com status `FECHADO` para uma empresa, **nenhuma** batida (manual, ajustada, importada ou vinculação de órfã), ocorrência ou movimentação de banco de horas pode alterar dias dentro daquele mês até reabertura formal auditada.
3. **Privacidade Intra-Tenant (Ownership e Hierarquia):** `COLABORADOR` só acessa recursos onde `funcionarioId == usuarioLogado.id`. `GESTOR` só acessa recursos de colaboradores sob sua cadeia hierárquica no mesmo tenant.
4. **Inalterabilidade/Preservação Trabalhista (Portaria 671):** Colaborador com marcações registradas **nunca** pode ser excluído fisicamente (`DELETE`), apenas desligado logicamente (`dataDesligamento`), e o desligamento deve revogar o acesso imediatamente.
5. **Estação de Ponto e Autenticação:** Login em multi-tenant deve resolver univocamente `(subdominio, usuario)`, bloquear contas desligadas e limitar força bruta em login e registro de ponto.

### Suposições e Perguntas Abertas
- **Suposição 1:** O sistema será implantado como SaaS multi-tenant em produção (confirmado pelo usuário na Rodada 1: cada cliente recebe seu subdomínio/link exclusivo).
- **Suposição 2:** Feriados `NACIONAL` e `PontoFacultativo` sem `empresaId` são recursos compartilhados entre todos os tenants — portanto, somente o `ADMIN` da plataforma (e não um `RH` de um cliente) deveria criá-los ou inativá-los.
- **Pergunta 1:** O perfil `GESTOR` deve poder visualizar atestados médicos (`GET /api/ocorrencias/{id}/anexo`) dos seus liderados diretos, ou o download de atestados deve ser restrito apenas ao próprio `COLABORADOR` e ao `RH` (por conter CID/dados de saúde sob LGPD)?
- **Pergunta 2:** O backend em produção ficará atrás de um reverse proxy/load balancer (ex.: Render, Railway, Cloudflare, Nginx)? Isso define se `request.getRemoteAddr()` precisa de `server.forward-headers-strategy` e rate limit distribuído.

---

## Etapa 1 — Verificação das Correções da Rodada 1

Avaliação do commit `d19d59b` (`fix(security): aplica hardening multi-tenant por subdominio, revogacao de desligados, rate limiting e auditoria unificada`):

| Correção Rodada 1 | Resolve causa raiz ou sintoma? | Contorno / Lacuna encontrada | Regressão / Bug novo? | Variantes encontradas no código | Cobertura de testes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **SEC-01:** `LocalDeTrabalhoService` isolado por tenant | Causa raiz no serviço de locais. | Nenhuma no próprio `LocalDeTrabalhoService`. | Não. | **SIM:** `EquipamentoService.cadastrar` aceita `localTrabalhoId` de outro tenant (**R2-07**); `FuncionarioService.desligar`/`excluir` sem tenant check (**R2-01**); `FeriadoService`/`PontoFacultativoService` sem tenant check (**R2-03**). | Não há teste de isolamento cross-tenant para esses endpoints. |
| **SEC-02:** `FuncionarioUserDetails.isEnabled()` verifica `dataDesligamento` | Causa raiz para bloqueio de desligados no `JwtAuthFilter` e login. | **Amplifica o impacto do R2-01:** como `desligar()` não valida tenant, um RH do tenant A pode desligar o RH do tenant B e trancá-lo fora do sistema instantaneamente. | Não. | Nenhuma. | Sem teste unitário/integração validando `401/403` com JWT válido após `desligamento`. |
| **SEC-03:** `JwtService` usa UUID no `sub` + login por `subdominio` | Causa raiz parcial. | Em `AuthController.java:83`, quando `subdominio` é omitido, o fluxo ainda cai no `AuthenticationManager` -> `UserDetailsServiceImpl.loadUserByUsername`, que chama `findByUsuario(username)` e lança `NonUniqueResultException` se houver login igual em duas empresas (**R2-09**). | Sem quebra em clientes que enviam `subdominio`. | `AuthController` retorna 500 se dois tenants tiverem mesmo `usuario` e cliente omitir `subdominio`. | Sem teste para colisão de `usuario` entre dois tenants. |
| **SEC-04:** `AdminFilter` com `MessageDigest.isEqual` + `ddl-auto` via env | Causa raiz na comparação timing-safe. | `application.properties:13-14` ainda mantém valores default embutidos via `${ADMIN_KEY:...}` e `${JWT_SECRET:...}` (mascarados). Além disso, o frontend ainda contém fallback hardcoded da `ADMIN_KEY` (**R2-08**). | Não. | `DatabaseSchemaAdjuster` ainda executa DDL (`ALTER TABLE ... DROP CONSTRAINT`) em todo boot (**R2-10**). | Sem teste de fail-fast sem variáveis em perfil `prod`. |
| **SEC-05:** `RegimeTrabalhoController` checa tenant/hierarquia | Causa raiz neste controller. | OK no `RegimeTrabalhoController`. | Não. | **SIM:** `BancoHorasController.consultarExtrato` (**R2-04**) e `OcorrenciaController` (**R2-02**) têm a mesma falha de checagem de hierarquia/ownership. | Sem testes de autorização por perfil (`COLABORADOR`/`GESTOR`/`RH`). |
| **SEC-06:** `RateLimiterService` no login e estação | **Sintoma** (in-memory por JVM). | `ConcurrentHashMap` sem expiração/evicção (vazamento de memória por DoS de IPs/IDs) e baseado em `request.getRemoteAddr()` sem configuração de proxy reverso; `/api/estacao/{id}/status` ficou sem rate limit (**R2-09**). | Não. | Outros endpoints públicos ou pesados (ex.: importação de arquivo) sem throttling. | Sem teste de estresse/evicção do `RateLimiterService`. |
| **SEC-07:** `FeriadoController` checa tenant | **Sintoma apenas na listagem (`GET`).** | `POST /api/v1/feriados` e `DELETE /api/v1/feriados/{id}` em `FeriadoService` **não validam** `SecurityTenantContext`, permitindo criar feriados em outra empresa ou inativar feriados nacionais/de terceiros (**R2-03**). | Não. | `PontoFacultativoController`/`Service` (**R2-03**) e `EmpresaController.listarEmpresas` (**R2-03**). | Sem teste cross-tenant em feriados/pontos facultativos. |
| **BIZ-01:** `InconsistenciaController.fecharPeriodo` delega ao `FechamentoPeriodoService` | Causa raiz no fechamento via inconsistências. | O lock de período fechado ainda é burlável via ocorrência multi-mês (**R2-05**) e via vinculação de batida órfã (**R2-06**). | Não. | `OcorrenciaColaboradorService` (**R2-05**) e `BatidaService.vincularOrfa` (**R2-06**). | Sem teste de tentativa de mutação em competência fechada nesses dois fluxos. |
| **ROB-01:** Logging SLF4J no lugar de `catch (Exception ignored)` | Causa raiz nos pontos alterados. | `OcorrenciaColaboradorService.lancarOcorrencia` ainda grava arquivo em disco antes do commit da transação, deixando arquivos órfãos em rollback (**R2-10**). | Não. | — | Sem teste de rollback com anexo. |

---

## Etapa 2 — Matriz de Autorização (26 Controllers)

Legenda de checagem de recurso: `Tenant` = isola `empresaId`; `Own` = restringe `COLABORADOR` ao próprio `id`; `Team` = restringe `GESTOR` à própria equipe; `FALHA` = brecha confirmada.

| Controller | Endpoint(s) Principais | Roles Permitidas | Checagem de Recurso / Tenant | Status |
| :--- | :--- | :--- | :--- | :--- |
| `AdminController` | `POST /api/admin/empresas`, `GET /api/admin/empresas`, `POST /api/admin/empresas/{id}/rh` | `X-Admin-Key` (via `AdminFilter`) | Superadmin SaaS | OK (mas chave vaza no front — **R2-08**) |
| `AuthController` | `POST /api/auth/login` | `permitAll` | Valida subdomínio (se enviado) + rate limit | Parcial (**R2-09**) |
| `EmpresaController` | `GET /v1/api/empresas` | `ROLE_RH` | **NENHUMA** (`findAll()` lista todas as empresas do SaaS) | **FALHA (R2-03)** |
| `FuncionarioController` | `GET/POST/PUT /api/rh/funcionarios/**` | `ROLE_RH` (e `GESTOR` em `/liderados`) | Tenant OK em `listar`, `buscar`, `criar`, `atualizar` | OK nestes |
| `FuncionarioController` | `PATCH /api/rh/funcionarios/{id}/desligar`, `DELETE /api/rh/funcionarios/{id}` | `ROLE_RH` | **NENHUMA** (`findById(id)` sem `empresaId` + `possuiMarcacoes()` retorna `false`) | **FALHA (R2-01)** |
| `OcorrenciaController` | `GET /api/ocorrencias/{id}/anexo` | Qualquer autenticado | **Apenas Tenant** (falta `Own` para Colaborador e `Team` para Gestor + `inline` XSS) | **FALHA (R2-02)** |
| `OcorrenciaController` | `GET /api/ocorrencias` | Qualquer autenticado | Tenant + `Own` (Colaborador), **falta `Team` para Gestor** | **FALHA (R2-02)** |
| `OcorrenciaController` | `POST /api/ocorrencias`, `DELETE /api/ocorrencias/{id}` | `ROLE_RH` | Tenant OK, mas **burla período fechado** em intervalo multi-mês | **FALHA (R2-05)** |
| `FeriadoController` | `GET /api/v1/feriados` | Autenticado | Tenant OK (Rodada 1) | OK |
| `FeriadoController` | `POST /api/v1/feriados`, `DELETE /api/v1/feriados/{id}` | `ROLE_RH` | **NENHUMA** (cria em outro tenant / inativa feriado nacional ou de terceiro) | **FALHA (R2-03)** |
| `PontoFacultativoController` | `GET/POST/DELETE /api/v1/pontos-facultativos` | `ROLE_RH` (escrita) / Autenticado (leitura) | **NENHUMA** (inativa registros globais/compartilhados) | **FALHA (R2-03)** |
| `BancoHorasController` | `GET /api/banco-horas/extrato` | `COLABORADOR`, `GESTOR`, `RH` | Tenant + `Own`, **falta `Team` para `GESTOR`** | **FALHA (R2-04)** |
| `BancoHorasController` | `POST /api/banco-horas/compensacao`, `POST /fechar` | `ROLE_RH` | Tenant OK + valida período aberto | OK |
| `BatidaController` | `POST /api/batidas/manual`, `GET /api/batidas/**` | `RH` / `GESTOR` / `COLABORADOR` | Tenant + `Own` + `Team` OK | OK |
| `BatidaController` | `POST /api/batidas/{id}/vincular` | `ROLE_RH` | Tenant OK, **falta lock de período fechado** | **FALHA (R2-06)** |
| `EquipamentoController` | `POST /api/equipamentos` | `ROLE_RH` | Empresa do token OK, mas **não valida tenant do `localTrabalhoId`** | **FALHA (R2-07)** |
| `EquipamentoController` | `GET/PATCH /api/equipamentos/**` | `ROLE_RH` | Tenant OK | OK |
| `LocalDeTrabalhoController` | `GET/POST/PUT/DELETE /api/locais-trabalho/**` | `ROLE_RH` | Tenant OK (Rodada 1) | OK |
| `RegimeTrabalhoController` | `GET/POST/PATCH /api/regimes-trabalho/**` | `RH` / `GESTOR` / `COLABORADOR` | Tenant + `Own` + `Team` OK (Rodada 1) | OK |
| `SolicitacaoAjusteController` | `POST /api/solicitacoes-ajuste`, `PATCH /{id}/analisar` | `COLABORADOR` / `GESTOR` / `RH` | Tenant + `Own` + `Team` OK | OK |
| `ApuracaoController` | `POST /api/apuracao/recalcular`, `GET /api/apuracao/**` | `RH` / `GESTOR` / `COLABORADOR` | Tenant + `Own` + `Team` OK | OK |
| `EspelhoController` | `GET /api/espelho/**` | `RH` / `GESTOR` / `COLABORADOR` | Tenant + `Own` + `Team` OK | OK |
| `InconsistenciaController` | `GET/POST /api/inconsistencias/**` | `RH` / `GESTOR` | Tenant + `Team` OK; `fecharPeriodo` delega a `FechamentoPeriodoService` | OK |
| `FechamentoPeriodoController` | `GET/POST /api/fechamento-periodo/**` | `ROLE_RH` | Tenant OK | OK |
| `ExportacaoFolhaController` | `POST/GET /api/exportacao-folha/**` | `ROLE_RH` | Tenant OK + proteção CSV injection | OK |
| `RelatorioController` | `GET /api/relatorios/**` | `RH` / `GESTOR` | Tenant + `Team` OK | OK |
| `PainelController` | `GET /api/painel/**` | `RH` / `GESTOR` / `COLABORADOR` | Tenant + `Own` + `Team` OK | OK |
| `JornadaController` | `GET/POST/PUT /api/jornadas/**` | `ROLE_RH` | Tenant OK | OK |
| `EscalaController` | `GET/POST/PUT /api/escalas/**` | `ROLE_RH` | Tenant OK | OK |
| `RegraApuracaoController` | `GET/POST/PUT /api/regras-apuracao/**` | `ROLE_RH` | Tenant OK | OK |
| `TipoOcorrenciaController` | `GET/POST/PUT /api/tipos-ocorrencia/**` | `ROLE_RH` / Autenticado | Tenant OK | OK |
| `HistoricoImportacaoController` | `POST/GET /api/importacoes/**` | `ROLE_RH` | Tenant OK (equipamento validado por empresa) | Parcial (sem limite de tamanho — **R2-10**) |
| `EstacaoController` | `POST /api/estacao/parear`, `POST /api/estacao/{id}/bater`, `GET /{id}/status` | `permitAll` (operação) / `ROLE_RH` (setup) | Setup valida tenant; `bater` tem rate limit; `status` sem rate limit | Parcial (**R2-09**) |

---

## Etapas 3 e 4 — Achados Detalhados da Rodada 2

### R2-01 — IDOR Destrutivo Cross-Tenant e Checagem Falsa da Portaria 671 em `desligar` e `excluir`
- **Severidade:** CRÍTICA — Um `RH` da Empresa A pode desligar (trancando o acesso imediatamente) ou excluir fisicamente funcionários (inclusive o RH) da Empresa B, além de violar a Portaria 671 permitindo deletar colaboradores com marcações.
- **Status:** `CONFIRMADO`
- **Evidência:** `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/service/FuncionarioService.java:111-126` e `143-156`
```java
// Linhas 111-126
public FuncionarioResponseDTO desligar(UUID id, DesligarFuncionarioDTO dto) {
    Funcionario funcionario = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Funcionário não encontrado"));
    // ... sem verificação de empresaId!
    funcionario.setDataDesligamento(dto.dataDesligamento());
    return FuncionarioResponseDTO.fromEntity(repository.save(funcionario));
}

// Linhas 143-156
public void excluir(UUID id) {
    Funcionario funcionario = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Funcionário não encontrado"));
    if (possuiMarcacoes(id)) {
        throw new IllegalStateException("Não é possível excluir funcionário com marcações registradas...");
    }
    repository.delete(funcionario);
}

private boolean possuiMarcacoes(UUID funcionarioId) {
    return false;
}
```
- **Cenário concreto de ataque:**
  1. Atacante autenticado como `ROLE_RH` da Empresa A envia `PATCH /api/rh/funcionarios/{uuid_rh_empresa_B}/desligar` com `{"dataDesligamento": "2026-11-21"}`.
  2. Como não há validação de `securityTenantContext.empresaAtual()`, o funcionário da Empresa B é marcado como desligado e, devido à correção SEC-02 da Rodada 1, tem o JWT revogado imediatamente (`isEnabled() == false`).
  3. De forma análoga, `DELETE /api/rh/funcionarios/{uuid_empresa_B}` remove fisicamente o registro mesmo que haja batidas registradas, pois `possuiMarcacoes(id)` está hardcoded como `return false;`.
- **Impacto real:** Sabotagem total (DoS administrativo e destruição de dados) entre clientes do SaaS e descumprimento legal da Portaria 671/MTP.
- **Correção sugerida:**
```java
UUID empresaId = securityTenantContext.empresaAtual();
Funcionario funcionario = repository.findByIdAndEmpresaId(id, empresaId)
        .orElseThrow(() -> new EntityNotFoundException("Funcionário não encontrado"));
```
E substituir `possuiMarcacoes(UUID funcionarioId)` por consulta real no `BatidaRepository` (ex.: `batidaRepository.existsByFuncionarioId(funcionarioId)`).
- **Teste de regressão:** Teste de integração onde RH da Empresa A tenta `PATCH .../desligar` e `DELETE .../{id}` sobre um funcionário da Empresa B -> deve retornar `404`. Segundo teste: RH da própria empresa tenta `DELETE` em funcionário com pelo menos 1 `Batida` persistida -> deve retornar `409`/`400` e manter o registro no banco.

---

### R2-02 — IDOR em Atestados Médicos (LGPD) + Risco de Stored XSS / Header Injection no Download de Anexos
- **Severidade:** CRÍTICA — Qualquer `COLABORADOR` pode baixar atestados médicos (dados sensíveis de saúde) de qualquer colega ou diretor da mesma empresa, `GESTOR` lista ocorrências da empresa inteira, e o anexo é servido como `inline` com `Content-Type` e `filename` não confiáveis.
- **Status:** `CONFIRMADO`
- **Evidência:**
  - `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/resource/OcorrenciaController.java:68-95`
  - `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/service/OcorrenciaColaboradorService.java:182-207`
```java
// OcorrenciaController.java:83-95
@GetMapping("/{id}/anexo")
public ResponseEntity<Resource> baixarAnexo(@PathVariable UUID id) {
    OcorrenciaColaboradorService.AnexoDownload download = ocorrenciaService.carregarAnexo(id);
    String contentType = download.contentType() != null
            ? download.contentType()
            : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + download.nomeOriginal() + "\"")
            .body(download.resource());
}
```
Em `OcorrenciaColaboradorService.buscarPorId(id)` (linhas 182-186), valida-se apenas `findByIdAndEmpresaId(id, empresaId)`, sem checar o perfil/ownership do usuário autenticado. Além disso, em `OcorrenciaController.listar` (linhas 68-81), há filtro de `COLABORADOR`, mas nenhum filtro de equipe para `GESTOR`.
- **Cenário concreto de ataque:**
  1. Um `COLABORADOR` comum autenticado chama `GET /api/ocorrencias/{uuid_ocorrencia_colega}/anexo`. O backend verifica apenas que ambos pertencem à mesma empresa e devolve o PDF/imagem do atestado médico do colega.
  2. Um `GESTOR` chama `GET /api/ocorrencias` (sem `funcionarioId` ou passando o ID de um diretor de outra área) e recebe a lista completa de ocorrências/atestados de toda a empresa.
  3. Se um arquivo foi enviado com `Content-Type` renderizável pelo navegador (ex.: `text/html` ou `image/svg+xml`) ou `filename` contendo aspas/quebra de linha, o cabeçalho `Content-Disposition: inline; filename="..."` permite renderização ativa no domínio da API ou injeção de parâmetros no header.
- **Impacto real:** Vazamento de dados sensíveis de saúde (violação grave da LGPD — Art. 5º, II e Art. 11) e vetor de execução de script/conteúdo ativo na origem da API.
- **Correção sugerida:**
  1. Em `carregarAnexo(id)`, validar que o chamador é `ROLE_RH`, ou é o próprio `ocorrencia.getFuncionario().getId()` (e, se decidido pelo negócio, `GESTOR` da hierarquia do funcionário).
  2. Em `OcorrenciaController.listar`, restringir `ROLE_GESTOR` aos seus liderados via `hierarquia`.
  3. No download, forçar `Content-Disposition: attachment` usando `ContentDisposition.attachment().filename(nomeSanitizado, StandardCharsets.UTF_8).build()`, adicionar `X-Content-Type-Options: nosniff` e restringir upload/download a uma allowlist estrita (`application/pdf`, `image/png`, `image/jpeg`) verificando magic bytes.
- **Teste de regressão:** Teste de integração com dois colaboradores da mesma empresa (`colab1` e `colab2`): `colab1` solicita `GET /api/ocorrencias/{id_colab2}/anexo` -> deve retornar `403`. Teste com `GESTOR` listando ocorrências de funcionário fora de sua equipe -> deve retornar lista vazia ou `403`. Teste de download validando header `Content-Disposition: attachment` e `X-Content-Type-Options: nosniff`.

---

### R2-03 — Vazamento de Todos os Tenants do SaaS e Sabotagem Cross-Tenant de Feriados e Pontos Facultativos
- **Severidade:** CRÍTICA — Qualquer `RH` lista todas as empresas clientes do SaaS (`id` + `razaoSocial`) e pode criar feriados em outras empresas ou inativar feriados nacionais e pontos facultativos de todos os clientes.
- **Status:** `CONFIRMADO`
- **Evidência:**
  - `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/resource/EmpresaController.java:23-28` + `EmpresaService.java:21-28`: `GET /v1/api/empresas` (`@PreAuthorize("hasRole('RH')")`) executa `empresaRepository.findAll()`.
  - `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/service/FeriadoService.java:31-69` e `107-112`: `cadastrar` aceita `dto.empresaId()` arbitrário ou `alcance = NACIONAL` sem checar `securityTenantContext.empresaAtual()`, e `inativar(UUID id)` faz `feriadoRepository.findById(id)` sem checar dono/tenant.
  - `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/service/PontoFacultativoService.java:27-53` e `85-92`: `cadastrar` e `inativar(UUID id)` operam sobre tabela global sem `empresaId`.
- **Cenário concreto de ataque:**
  1. `RH` da Empresa A chama `GET /v1/api/empresas` -> obtém os UUIDs e razões sociais de todas as empresas do SaaS.
  2. Com o `empresaId` da Empresa B (ou usando `alcance: "NACIONAL"`), o atacante chama `POST /api/v1/feriados` para injetar feriados falsos no calendário da Empresa B (alterando apuração de horas extras 100% na folha da concorrente).
  3. O atacante chama `DELETE /api/v1/feriados/{id_feriado_nacional}` ou `DELETE /api/v1/pontos-facultativos/{id}` e desativa feriados oficiais (ex.: Natal) ou pontos facultativos para todos os tenants do SaaS.
- **Impacto real:** Exposição da base de clientes SaaS e corrupção financeira da apuração trabalhista de todas as empresas na plataforma.
- **Correção sugerida:**
  - Em `EmpresaController.listarEmpresas`, retornar **apenas** a empresa do `securityTenantContext.empresaAtual()` (listagem global deve ficar restrita ao `AdminController`).
  - Em `FeriadoService.cadastrar`, ignorar `dto.empresaId()` do body, fixar sempre `securityTenantContext.empresaAtual()`, e proibir `ROLE_RH` de criar/inativar feriados `NACIONAL` (restrito ao `ADMIN`).
  - Em `FeriadoService.inativar`, buscar por `findByIdAndEmpresaId(id, empresaAtual)`.
  - Em `PontoFacultativo`, adicionar coluna `empresa_id` (multi-tenant) ou restringir escrita/inativação ao `ADMIN`.
- **Teste de regressão:** Teste onde `RH` da Empresa A chama `GET /v1/api/empresas` -> recebe apenas 1 item (Empresa A). Teste onde `RH` da Empresa A tenta `DELETE /api/v1/feriados/{id_nacional}` e `DELETE /api/v1/feriados/{id_empresa_B}` -> ambos devem retornar `403`/`404`.

---

### R2-04 — IDOR Intra-Tenant para Perfil `GESTOR` no Extrato de Banco de Horas
- **Severidade:** ALTA — Qualquer `GESTOR` pode consultar o extrato detalhado de banco de horas (saldos, horas extras, faltas e compensações) de qualquer colaborador ou diretor da empresa, mesmo fora de sua equipe.
- **Status:** `CONFIRMADO`
- **Evidência:** `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/resource/BancoHorasController.java:88-99`
```java
private void validarPermissaoAcesso(UUID funcionarioId) {
    FuncionarioUserDetails usuario = securityTenantContext.usuarioAtual();
    if (usuario.getPerfil() == PerfilAcesso.COLABORADOR && !usuario.getId().equals(funcionarioId)) {
        throw new AccessDeniedException("Colaborador só pode consultar o próprio banco de horas");
    }
    // Falta validação de hierarquia quando usuario.getPerfil() == PerfilAcesso.GESTOR!
}
```
- **Cenário concreto de ataque:**
  1. Um usuário com perfil `ROLE_GESTOR` chama `GET /api/banco-horas/extrato?funcionarioId={uuid_funcionario_outra_equipe}&mes=2026-10`.
  2. Como `usuario.getPerfil()` não é `COLABORADOR`, o `if` é pulado e o serviço apenas verifica se o funcionário pertence à mesma empresa, retornando o extrato completo.
- **Impacto real:** Quebra da segregação de acesso por equipe (invariante de privacidade intra-tenant).
- **Correção sugerida:** Quando `usuario.getPerfil() == PerfilAcesso.GESTOR` e `!usuario.getId().equals(funcionarioId)`, validar se `funcionarioId` pertence à equipe liderada por `usuario.getId()` (reutilizando o mesmo helper de hierarquia usado em `EspelhoService`/`RegimeTrabalhoController`).
- **Teste de regressão:** Teste de integração onde `GESTOR_A` consulta `GET /api/banco-horas/extrato?funcionarioId={subordinado_de_GESTOR_B}` -> deve retornar `403 Forbidden`.

---

### R2-05 — Burla de Período Fechado por Ocorrência Multi-Mês
- **Severidade:** ALTA — O lock de imutabilidade de competência fechada pode ser contornado lançando uma ocorrência cuja data inicial está no mês anterior (aberto) e a data final no mês seguinte (aberto), atravessando um mês intermediário já fechado.
- **Status:** `CONFIRMADO`
- **Evidência:** `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/service/OcorrenciaColaboradorService.java:90-91`, `143` e `156-157`
```java
// Linhas 90-91 (lancarOcorrencia) e 156-157 (cancelarOcorrencia)
validarPeriodoNaoFechado(empresaId, req.dataInicio());
validarPeriodoNaoFechado(empresaId, req.dataFim());
// ... depois executa:
recalcularDiasAfetados(ocorrencia.getFuncionario(), ocorrencia.getDataInicio(), ocorrencia.getDataFim());
```
- **Cenário concreto de ataque:**
  1. Suponha competência `2026-10` com status `FECHADO` (folha já paga/exportada), enquanto `2026-09` e `2026-11` estão abertas.
  2. Uma requisição `POST /api/ocorrencias` com `dataInicio: "2026-09-30"` e `dataFim: "2026-11-01"` passa nas duas validações pontuais (`2026-09` aberta e `2026-11` aberta).
  3. `recalcularDiasAfetados` itera dia a dia de `2026-09-30` a `2026-11-01`, sobrescrevendo todas as `ApuracaoDiaria` de outubro (`2026-10`), violando a imutabilidade do mês fechado. O mesmo ocorre em `cancelarOcorrencia`.
- **Impacto real:** Alteração retroativa silenciosa de apuração já fechada e enviada à folha de pagamento, gerando divergência contábil e trabalhista.
- **Correção sugerida:** Iterar todos os `YearMonth` do intervalo `[YearMonth.from(dataInicio), YearMonth.from(dataFim)]` e chamar `competenciaLockService.validarPeriodoAberto(empresaId, ym)` para cada mês abrangido (tanto no lançamento quanto no cancelamento).
- **Teste de regressão:** Teste com `2026-10` fechado e `2026-09`/`2026-11` abertos tentando lançar ocorrência `2026-09-28` a `2026-11-02` -> deve lançar `PeriodoFechadoException` (`409`) e não alterar nenhuma `ApuracaoDiaria` de outubro.

---

### R2-06 — Burla de Período Fechado ao Vincular Batida Órfã (`vincularOrfa`)
- **Severidade:** ALTA — `POST /api/batidas/{id}/vincular` não verifica se a competência da batida está fechada, permitindo alterar a apuração de um mês já fechado.
- **Status:** `CONFIRMADO`
- **Evidência:** `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/service/BatidaService.java:351-372`
```java
@Transactional
public Batida vincularOrfa(UUID batidaId, UUID funcionarioId) {
    UUID empresaId = securityTenantContext.empresaAtual();
    Batida batida = batidaRepository.findById(batidaId)...
    // Valida empresa da batida e do funcionário, mas NÃO chama competenciaLockService.validarPeriodoAberto!
    batida.setFuncionario(funcionario);
    batida.setStatus(StatusBatida.VALIDA);
    // ... dispara recálculo da apuração daquele dia
}
```
- **Cenário concreto de ataque:**
  1. Existe uma batida órfã importada em `2026-10-15`, e a competência `2026-10` já está `FECHADA`.
  2. Um usuário `RH` chama `POST /api/batidas/{batidaId}/vincular` com `{"funcionarioId": "..."}`.
  3. A batida é vinculada e a apuração de `2026-10-15` é alterada sem exigir reabertura auditada do período.
- **Impacto real:** Quebra da invariante de imutabilidade de competência fechada e divergência entre espelho e folha já fechada.
- **Correção sugerida:** Antes de alterar a batida em `vincularOrfa`, chamar `competenciaLockService.validarPeriodoAberto(empresaId, batida.getDataHora().toLocalDate())`.
- **Teste de regressão:** Teste de integração com período `2026-10` fechado tentando vincular batida órfã de `2026-10-15` -> deve retornar `409 Conflict` e manter a batida como órfã.

---

### R2-07 — Injeção de Referência Cross-Tenant em `EquipamentoService.cadastrar`
- **Severidade:** ALTA — Ao cadastrar um equipamento, o serviço valida apenas que o `localTrabalhoId` existe no banco, sem verificar se o local pertence à empresa do RH autenticado.
- **Status:** `CONFIRMADO`
- **Evidência:** `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/service/EquipamentoService.java:47-51`
```java
LocalDeTrabalho local = localDeTrabalhoRepository.findById(criarDto.localTrabalhoId())
        .orElseThrow(() -> new LocalDeTrabalhoNaoEncontradoException("Local de trabalho não encontrado"));
// Não verifica local.getEmpresa().getId().equals(empresa.getId())!
```
- **Cenário concreto de ataque:**
  1. `RH` da Empresa A chama `POST /api/equipamentos` enviando `localTrabalhoId` pertencente à Empresa B.
  2. O equipamento da Empresa A é persistido apontando para o `LocalDeTrabalho` da Empresa B, podendo expor nome/endereço do local da Empresa B no `EquipamentoResponseDTO` retornado na listagem.
- **Impacto real:** Vazamento de metadados de outro tenant e corrupção de integridade relacional multi-tenant.
- **Correção sugerida:** Trocar `findById(criarDto.localTrabalhoId())` por `findByIdAndEmpresaId(criarDto.localTrabalhoId(), empresa.getId())`.
- **Teste de regressão:** Teste de integração onde RH da Empresa A tenta cadastrar equipamento com `localTrabalhoId` da Empresa B -> deve retornar `404`.

---

### R2-08 — Exposição de `ADMIN_KEY` e Credenciais no Bundle do Frontend + Armazenamento de Tokens em `localStorage`
- **Severidade:** ALTA — O código-fonte entregue ao navegador do usuário contém fallbacks da chave mestra administrativa (`X-Admin-Key`) e credenciais de demonstração, além de persistir JWT e chave admin em `localStorage` (exposto a qualquer XSS).
- **Status:** `CONFIRMADO`
- **Evidência:**
  - `client/src/pages/LoginPage.jsx:227-267` (credenciais de demonstração embutidas na UI de login)
  - `client/src/pages/AdminDashboard.jsx:95`, `client/src/pages/RhDashboard.jsx:97,538`, `client/src/services/authService.js:31`, `client/src/services/rhService.js:37`, `client/src/services/localTrabalhoService.js:24` (referências/fallbacks de `X-Admin-Key` e credenciais no bundle cliente — valores omitidos/mascarados conforme regra da auditoria)
  - `client/src/services/api.js:7-8` e `client/src/services/authService.js:41-52`: `localStorage.getItem('token')` e `localStorage.getItem('adminKey')`
  - `client/src/pages/RhDashboard.jsx:771`: persiste lista de usuários criados em `localStorage`.
- **Cenário concreto de ataque:**
  1. Qualquer visitante não autenticado abre o DevTools -> Sources no bundle JS compilado pelo Vite e extrai a chave `X-Admin-Key` (caso em produção o `ADMIN_KEY` não tenha sido alterado do fallback ou tenha sido embutido via `VITE_*`).
  2. Com a chave, chama `POST /api/admin/empresas` ou `GET /api/admin/empresas`, comprometendo todo o SaaS.
  3. Em paralelo, qualquer XSS no domínio permite ler `localStorage.getItem('token')` e exfiltrar o JWT válido por 24h (sem revogação no logout).
- **Impacto real:** Comprometimento total do painel administrativo SaaS e sequestro de sessão de longa duração.
- **Correção sugerida:**
  - Remover **completamente** qualquer fallback de `ADMIN_KEY` e credenciais hardcoded do bundle `client/src/` (em dev, exigir digitação manual ou usar `.env.local` nunca usado em build de produção).
  - Migrar autenticação do Admin para conta com login/senha + MFA no backend (sem chave estática em header vinda do browser).
  - Migrar JWT de `localStorage` para cookie `HttpOnly`, `Secure`, `SameSite=Lax/Strict`, e remover armazenamento de listas de usuários em `localStorage`.
- **Teste de regressão:** Verificação automatizada em CI (grep no `dist/` após `npm run build`) garantindo ausência da chave admin e de credenciais de demo no artefato de produção.

---

### R2-09 — Lacunas e Contornos nas Correções da Rodada 1 (`RateLimiterService`, Login sem Subdomínio, Segredos Default)
- **Severidade:** MÉDIA — Limitação de taxa pode ser contornada/esgotada em produção com proxy reverso, login sem subdomínio ainda quebra com `500` em colisão de username entre tenants, e `application.properties` mantém defaults de segredos.
- **Status:** `CONFIRMADO`
- **Evidência:**
  - `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/security/RateLimiterService.java:14-43`: `ConcurrentHashMap<String, Bucket>` em memória sem limpeza/evicção de chaves antigas; chaves vindas de `request.getRemoteAddr()`.
  - `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/resource/AuthController.java:83` + `UserDetailsServiceImpl.java:24`: se `subdominio` vier nulo/vazio, executa `findByUsuario(username)` (sem `empresaId`), lançando `NonUniqueResultException` quando dois clientes usam o mesmo login (ex.: `rh` ou `joao.silva`).
  - `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/resource/EstacaoController.java:47-57`: `GET /api/estacao/{estacaoId}/status` é `permitAll` e não possui rate limit.
  - `server/pontoCerto/src/main/resources/application.properties:13-14`: placeholders `${JWT_SECRET:...}` e `${ADMIN_KEY:...}` ainda possuem valores default literais (mascarados).
- **Cenário concreto de ataque:**
  1. Em produção atrás de proxy reverso sem `server.forward-headers-strategy=framework`, todos os usuários compartilham o IP do proxy — um atacante disparando tentativas no login bloqueia **todos os clientes** do SaaS (DoS). Se o IP variar (botnet), o `ConcurrentHashMap` cresce indefinidamente até `OutOfMemoryError`.
  2. Dois clientes cadastram usuário `admin_rh`; um cliente antigo envia login sem `subdominio` e recebe `500 Internal Server Error` (além de servir como oráculo de existência de username duplicado em outro tenant).
- **Impacto real:** DoS por bloqueio coletivo ou exaustão de memória, indisponibilidade de login em colisão de nomes e risco de subida em produção com segredos fracos padrão.
- **Correção sugerida:**
  - Substituir `ConcurrentHashMap` por cache com TTL e limite máximo de entradas (ex.: Caffeine) ou Redis em múltiplas instâncias, e configurar `server.forward-headers-strategy=framework` apenas quando atrás de proxy confiável.
  - Tornar `subdominio` obrigatório no `LoginRequestDTO` (ou extraí-lo do header `Host` no backend), eliminando o fallback `findByUsuario`.
  - Criar `application-prod.properties` **sem** valores default para `JWT_SECRET`, `ADMIN_KEY` e `SPRING_DATASOURCE_*` (fail-fast no boot) e `spring.jpa.hibernate.ddl-auto=validate`.
- **Teste de regressão:** Teste unitário no `RateLimiterService` validando limite máximo de chaves/evicção; teste no `AuthController` garantindo `400 Bad Request` quando `subdominio` é omitido.

---

### R2-10 — DDL Destrutivo em Todo Boot (`DatabaseSchemaAdjuster`), Upload Sem Limite de Tamanho e Arquivos Órfãos em Rollback
- **Severidade:** MÉDIA — Execução de `ALTER TABLE` no startup gera risco de lock/deadlock no Neon em deploys com múltiplas instâncias; ausência de limite explícito de upload permite DoS de memória/banco; anexo salvo antes do commit deixa lixo em disco.
- **Status:** `CONFIRMADO`
- **Evidência:**
  - `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/config/DatabaseSchemaAdjuster.java:18-53`: `ApplicationRunner` executa `ALTER TABLE batida DROP CONSTRAINT ...` em toda inicialização da aplicação.
  - `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/service/ImportacaoService.java` + `application.properties`: sem `spring.servlet.multipart.max-file-size` explícito configurado no properties, e o conteúdo completo do arquivo importado é carregado em memória e persistido como texto em `HistoricoImportacao`.
  - `server/pontoCerto/src/main/java/com/example/projetoPBD/pontoCerto/service/OcorrenciaColaboradorService.java:125`: chama `anexoStorageService.salvar(...)` antes do commit da transação JPA — se ocorrer exceção posterior (ex.: violação de constraint ou rollback), o arquivo permanece órfão no disco.
- **Cenário concreto de ataque / falha operacional:**
  1. Durante um deploy rolling (2 pods subindo ao mesmo tempo contra o PostgreSQL/Neon), ambos disparam `ALTER TABLE batida ...` adquirindo `ACCESS EXCLUSIVE LOCK`, travando batidas em andamento ou causando deadlock de boot.
  2. Um usuário `RH` envia um arquivo posicional muito grande (ou corrompido) repetidamente, consumindo heap da JVM e armazenamento do Neon.
- **Impacto real:** Instabilidade em deploy/produção, consumo excessivo de disco/banco no Neon e indisponibilidade temporária.
- **Correção sugerida:**
  - Migrar alterações de schema para Flyway/Liquibase e remover `DatabaseSchemaAdjuster` do runtime.
  - Configurar `spring.servlet.multipart.max-file-size=5MB` e `spring.servlet.multipart.max-request-size=6MB` em `application.properties`.
  - Registrar `TransactionSynchronizationManager.registerSynchronization` (no `afterCompletion` com `STATUS_ROLLED_BACK`) para remover o arquivo gravado em disco caso a transação de ocorrência sofra rollback.
- **Teste de regressão:** Teste de rollback em `lancarOcorrencia` verificando que o arquivo salvo no diretório temporário é removido quando a transação falha; teste de upload acima de 5MB retornando `413 Payload Too Large`.

---

## Etapa 5 — Autocrítica e Hipóteses Refutadas

Antes de fechar os achados acima, cada ponto crítico foi reavaliado contra controles compensatórios (ex.: `SecurityConfig`, `@PreAuthorize`, `SecurityTenantContext`, filtros JWT):
- **Descarte de falso positivo:** Inicialmente suspeitou-se que `BatidaService.vincularOrfa` também permitisse IDOR cross-tenant. Ao ler `BatidaService.java:357-372` linha a linha, verificou-se que tanto a empresa da batida quanto a empresa do funcionário são comparadas com `securityTenantContext.empresaAtual()`. O achado foi portanto **reduzido e focado** exclusivamente no bypass real confirmado: ausência do lock de período fechado (**R2-06**).
- **Descarte de falso positivo:** Suspeitou-se de IDOR em `EquipamentoService` e `ImportacaoService` ao informar `equipamentoId` de outra empresa na importação. A leitura de `ImportacaoService.java:406-412` (`buscarEValidarEquipamento`) provou que `equipamento.getEmpresa().getId().equals(empresaLogada)` é verificado. O risco real restante é apenas na criação do equipamento com `localTrabalhoId` de outro tenant (**R2-07**).

### Registro de Hipóteses Refutadas

| Hipótese Testada | Resultado | Evidência da Refutação |
| :--- | :--- | :--- |
| "Um `RH` da Empresa A consegue editar/inativar `TipoOcorrencia` da Empresa B" | **REFUTADA** | `TipoOcorrenciaService` usa `securityTenantContext.empresaAtual()` com `findByIdAndEmpresaId`. |
| "Um `RH` da Empresa A consegue alterar `RegraApuracao`, `Jornada` ou `Escala` da Empresa B" | **REFUTADA** | `RegraApuracaoService`, `JornadaService` e `EscalaService` filtram todas as consultas e alterações por `empresaAtual()`. |
| "Um `RH` da Empresa A consegue autorizar pareamento de estação da Empresa B" | **REFUTADA** | `EstacaoSetupService.autorizarEstacao` consulta `findByEmpresaIdAndCodigo(securityTenantContext.empresaAtual(), codigo)`. |
| "Um `COLABORADOR` consegue abrir solicitação de ajuste em nome de outro colaborador ou aprovar a própria" | **REFUTADA** | `SolicitacaoAjusteController.abrirSolicitacao` extrai o colaborador do token autenticado e `analisarSolicitacao` exige `GESTOR`/`RH` com validação de hierarquia e tenant. |
| "Um `RH` consegue importar arquivo posicional usando `equipamentoId` de outro tenant" | **REFUTADA** | `ImportacaoService.java:406-412` valida explicitamente `equipamento.getEmpresa().getId().equals(empresaLogada)`. |
| "Frontend renderiza HTML não sanitizado com `dangerouslySetInnerHTML`" | **REFUTADA** | Busca global em `client/src/` não encontrou nenhuma ocorrência de `dangerouslySetInnerHTML` ou `innerHTML`. |
| "Há arquivos `.env` ou `workspace.xml` com credenciais commitados no histórico do git" | **REFUTADA** (com ressalva) | `git log --all --full-history --stat -- "**/.env*" "**/workspace.xml"` não encontrou arquivos `.env` ou `workspace.xml` no histórico. Ressalva: `application.properties` possui placeholders com defaults locais (reportado em **R2-09**). |

---

## Log de Cobertura Obrigatório

### Backend — `config/` e `security/`
| Arquivo | Nível de Leitura |
| :--- | :--- |
| `config/SecurityConfig.java` | LIDO COM ATENÇÃO |
| `config/DatabaseSchemaAdjuster.java` | LIDO COM ATENÇÃO |
| `config/ApiTimeProperties.java` | LIDO SUPERFICIALMENTE |
| `config/SecureRandomConfig.java` | LIDO SUPERFICIALMENTE |
| `security/JwtService.java` | LIDO COM ATENÇÃO |
| `security/RateLimiterService.java` | LIDO COM ATENÇÃO |
| `security/SecurityTenantContext.java` | LIDO COM ATENÇÃO |
| `security/TenantContext.java` | LIDO COM ATENÇÃO |
| `security/UserDetailsServiceImpl.java` | LIDO COM ATENÇÃO |
| `security/filter/JwtAuthFilter.java` | LIDO COM ATENÇÃO |
| `security/filter/AdminFilter.java` | LIDO COM ATENÇÃO |

### Backend — `resource/` (Todos os 26 Controllers)
| Arquivo | Nível de Leitura |
| :--- | :--- |
| `AdminController.java` | LIDO COM ATENÇÃO |
| `ApuracaoController.java` | LIDO COM ATENÇÃO |
| `AuthController.java` | LIDO COM ATENÇÃO |
| `BancoHorasController.java` | LIDO COM ATENÇÃO |
| `BatidaController.java` | LIDO COM ATENÇÃO |
| `EmpresaController.java` | LIDO COM ATENÇÃO |
| `EquipamentoController.java` | LIDO COM ATENÇÃO |
| `EscalaController.java` | LIDO SUPERFICIALMENTE |
| `EspelhoController.java` | LIDO COM ATENÇÃO |
| `EstacaoController.java` | LIDO COM ATENÇÃO |
| `ExportacaoFolhaController.java` | LIDO COM ATENÇÃO |
| `FechamentoPeriodoController.java` | LIDO COM ATENÇÃO |
| `FeriadoController.java` | LIDO COM ATENÇÃO |
| `FuncionarioController.java` | LIDO COM ATENÇÃO |
| `HistoricoImportacaoController.java` | LIDO COM ATENÇÃO |
| `InconsistenciaController.java` | LIDO COM ATENÇÃO |
| `JornadaController.java` | LIDO SUPERFICIALMENTE |
| `LocalDeTrabalhoController.java` | LIDO COM ATENÇÃO |
| `OcorrenciaController.java` | LIDO COM ATENÇÃO |
| `PainelController.java` | LIDO COM ATENÇÃO |
| `PontoFacultativoController.java` | LIDO COM ATENÇÃO |
| `RegimeTrabalhoController.java` | LIDO COM ATENÇÃO |
| `RegraApuracaoController.java` | LIDO SUPERFICIALMENTE |
| `RelatorioController.java` | LIDO COM ATENÇÃO |
| `SolicitacaoAjusteController.java` | LIDO COM ATENÇÃO |
| `TipoOcorrenciaController.java` | LIDO SUPERFICIALMENTE |

### Backend — `service/` (Todos os 30 Services)
| Arquivo | Nível de Leitura |
| :--- | :--- |
| `AnexoStorageService.java` | LIDO COM ATENÇÃO |
| `ApuracaoDiariaService.java` | LIDO SUPERFICIALMENTE |
| `AuthService.java` | LIDO SUPERFICIALMENTE |
| `BancoHorasService.java` | LIDO COM ATENÇÃO |
| `BatidaService.java` | LIDO COM ATENÇÃO |
| `CompetenciaLockService.java` | LIDO COM ATENÇÃO |
| `EmpresaService.java` | LIDO COM ATENÇÃO |
| `EquipamentoService.java` | LIDO COM ATENÇÃO |
| `EscalaService.java` | LIDO COM ATENÇÃO |
| `EspelhoService.java` | LIDO COM ATENÇÃO |
| `EstacaoService.java` | LIDO SUPERFICIALMENTE |
| `EstacaoSetupService.java` | LIDO COM ATENÇÃO |
| `ExportacaoFolhaService.java` | LIDO COM ATENÇÃO |
| `FechamentoPeriodoService.java` | LIDO COM ATENÇÃO |
| `FeriadoService.java` | LIDO COM ATENÇÃO |
| `FuncionarioService.java` | LIDO COM ATENÇÃO |
| `ImportacaoService.java` | LIDO COM ATENÇÃO |
| `InconsistenciaRegistroAutonomoService.java` | LIDO SUPERFICIALMENTE |
| `InconsistenciaService.java` | LIDO COM ATENÇÃO |
| `JornadaService.java` | LIDO COM ATENÇÃO |
| `LocalDeTrabalhoService.java` | LIDO COM ATENÇÃO |
| `OcorrenciaColaboradorService.java` | LIDO COM ATENÇÃO |
| `PainelService.java` | LIDO SUPERFICIALMENTE |
| `PontoFacultativoService.java` | LIDO COM ATENÇÃO |
| `ProcessadorArquivoPosicional.java` | LIDO SUPERFICIALMENTE |
| `RegimeTrabalhoService.java` | LIDO COM ATENÇÃO |
| `RegraApuracaoService.java` | LIDO COM ATENÇÃO |
| `RelatorioService.java` | LIDO COM ATENÇÃO |
| `RelogioService.java` | NÃO LIDO |
| `SolicitacaoAjusteService.java` | LIDO COM ATENÇÃO |
| `TipoOcorrenciaService.java` | LIDO COM ATENÇÃO |

### Backend — `repository/`
Todos os 26 repositórios JPA (`ApuracaoDiariaRepository` até `TipoOcorrenciaRepository`): **LIDO SUPERFICIALMENTE** (inspecionados conforme chamadas dos serviços para confirmar queries com `empresaId` e ausência de SQL dinâmico concatenado).

### Frontend — `client/src/`
| Arquivo(s) | Nível de Leitura |
| :--- | :--- |
| `services/api.js`, `services/authService.js`, `services/adminService.js`, `services/rhService.js`, `services/localTrabalhoService.js` | LIDO COM ATENÇÃO |
| `context/AuthContext.jsx`, `pages/LoginPage.jsx`, `pages/AdminDashboard.jsx`, `pages/RhDashboard.jsx` | LIDO COM ATENÇÃO |
| Demais serviços (`apuracaoService.js`..`solicitacaoAjusteService.js`) e componentes (`components/*.jsx`, `ColaboradorDashboard.jsx`, `GestorDashboard.jsx`, `App.jsx`, `main.jsx`, `utils/brazilianDocuments.js`) | LIDO SUPERFICIALMENTE (varredura via grep por XSS, storage, tokens e credenciais) |

---

## Resumo Executivo — Top 5 Riscos Restantes

1. **R2-01 (CRÍTICA):** `RH` de uma empresa pode desligar (bloqueando acesso) ou excluir fisicamente funcionários de **outra empresa** no SaaS, além de `possuiMarcacoes()` estar hardcoded como `false` (violando a Portaria 671).
2. **R2-02 (CRÍTICA):** Qualquer `COLABORADOR` pode baixar atestados médicos (LGPD) de colegas via `GET /api/ocorrencias/{id}/anexo`, `GESTOR` vê ocorrências da empresa inteira, e o anexo é servido como `inline` sem sanitização de `Content-Type`/`filename`.
3. **R2-03 (CRÍTICA):** `GET /v1/api/empresas` expõe todos os clientes do SaaS para qualquer `RH`, e `FeriadoService`/`PontoFacultativoService` permitem criar feriados em outros tenants ou inativar feriados nacionais/globais.
4. **R2-08 (ALTA):** `ADMIN_KEY` e credenciais de demo expostas no código do frontend, com tokens armazenados em `localStorage` sem revogação no logout.
5. **R2-05 e R2-06 (ALTA):** O bloqueio de competência fechada pode ser burlado por ocorrência multi-mês atravessando mês fechado e por vinculação de batida órfã (`POST /api/batidas/{id}/vincular`).

### Perguntas para Decisão Antes de Corrigir
1. **Atestados médicos e `GESTOR`:** O `GESTOR` deve poder baixar o anexo de atestado médico dos seus liderados diretos, ou o download deve ser exclusivo do próprio `COLABORADOR` e do `RH`?
2. **Feriados Nacionais e Pontos Facultativos:** Devem ser gerenciados apenas pelo `ADMIN` global da plataforma (enquanto o `RH` cria apenas feriados/pontos facultativos da própria empresa)?
3. **Infraestrutura de produção:** O backend vai rodar em instância única ou múltiplas instâncias atrás de proxy reverso (ex.: Render/Railway/Cloudflare)? Isso define se `RateLimiterService` precisa de Caffeine local ou Redis + `forward-headers-strategy`.
