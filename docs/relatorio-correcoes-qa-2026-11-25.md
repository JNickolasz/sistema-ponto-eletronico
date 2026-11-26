# Relatório Técnico de Correções — QA (25/11/2026)

Este documento detalha cada inconsistência reportada pela equipe de QA no sistema **Ponto Certo**, explicando **o que causava o problema** (causa raiz no código) e **como foi alterado** no backend (Spring Boot / PostgreSQL) e no frontend (React / Vite) para corrigir definitivamente cada comportamento.

---

## 1. Erros `"Erro na requisição"` e `"Falha ao consultar relatório."` nas telas do Gestor (Fotos 1 e 2)

### Sintoma observado
- Ao clicar nos filtros de status (`PENDENTE`, `DEFERIDO`, `INDEFERIDO`, `Todas`) na tela **Solicitações de Ajuste** do Gestor (ex.: `gestor.ops`), múltiplos toasts de **`"Erro na requisição"`** eram disparados.
- Ao acessar **Relatórios da Equipe** ou demais abas do Gestor, apareciam os toasts **`"Falha ao consultar relatório."`** e **`"Erro na requisição"`**, sem deslogar o usuário nem exibir a mensagem real do servidor.

### O que causava o erro (Causa Raiz)
1. **Token JWT expirado/inválido retornando `403` com corpo vazio (`""`):**
   - No filtro `JwtAuthFilter.java`, quando o cabeçalho `Authorization: Bearer <token>` era enviado com um token expirado (após 24h) ou inválido, `jwtService.validateToken(token)` retornava `null`.
   - O filtro não interrompia a requisição com `401 Unauthorized`; ele simplesmente seguia a cadeia (`filterChain.doFilter`) sem autenticar o `SecurityContext`.
   - O `AuthorizationFilter` padrão do Spring Security bloqueava o acesso retornando **HTTP `403 Forbidden` com corpo vazio (0 bytes)** em vez de `401`.
2. **Frontend preso na sessão expirada:**
   - Em `client/src/services/api.js`, a verificação de sessão revogada/expirada checava apenas `response.status === 401`. Como o Spring Security devolvia `403` sem corpo, o frontend não limpava o `localStorage`, não redirecionava para `/login` e caía no fallback genérico `'Erro na requisição'` a cada clique.
3. **`relatorioService.js` isolado do cliente HTTP padrão:**
   - `client/src/services/relatorioService.js` usava um `fetch` próprio que fazia `await res.text()` sem interpretar JSON nem disparar o evento `auth:session-revoked` em caso de erro de autenticação, caindo sempre no fallback `'Falha ao consultar relatório.'`.

### Como foi alterado para corrigir
- **Backend (`JwtAuthFilter.java`):**
  - Quando um cabeçalho `Authorization: Bearer ...` é enviado e `jwtService.validateToken(token)` retorna `null`, o filtro agora interrompe imediatamente a requisição com **HTTP `401 Unauthorized`** e payload JSON `{"codigo":"TOKEN_INVALIDO","message":"Sua sessão expirou ou é inválida. Faça login novamente."}`.
- **Backend (`SecurityConfig.java` e `GlobalExceptionHandler.java`):**
  - Configurados `authenticationEntryPoint` (retornando `401` JSON `NAO_AUTENTICADO`) e `accessDeniedHandler` (retornando `403` JSON `ACESSO_NEGADO`) na `SecurityFilterChain`, além de `@ExceptionHandler(AccessDeniedException.class)` no `GlobalExceptionHandler` e fallback para mensagens nulas, garantindo que nenhuma falha retorne corpo vazio.
- **Backend (`FuncionarioController.java`):**
  - O endpoint `GET /api/v1/gestor/painel` passou a retornar também `nomeCompleto` e `funcionarioId` do gestor autenticado.
- **Frontend (`api.js` e `relatorioService.js`):**
  - `api.js` passou a reconhecer os códigos `TOKEN_INVALIDO` e `NAO_AUTENTICADO` para encerrar a sessão e redirecionar ao login.
  - `relatorioService.js` foi atualizado para extrair mensagens de erro JSON do backend e disparar `auth:session-revoked` quando receber `401`.

---

## 2. Filtragem de Feriados e Pontos Facultativos no RH e visualização do total cadastrado (Fotos 3 e 8)

### Sintoma observado
- Na aba **Feriados & Calendário** do RH, não era possível visualizar todos os feriados cadastrados no total sem filtrar, nem filtrar de forma independente apenas por Estado (`UF`), apenas por Cidade (`Município`) ou apenas por `Ano`.

### O que causava o erro (Causa Raiz)
1. **Parâmetro `ano` obrigatório no backend e forçado no frontend:**
   - `FeriadoController.java` e `PontoFacultativoController.java` exigiam `@RequestParam("ano") int ano`.
   - Em `feriadoService.js`, o código fazia `params.append('ano', filtros.ano || new Date().getFullYear())`, e no `RhDashboard.jsx` o estado `anoCalendario` era convertido com `Number(e.target.value)` — impedindo deixar o campo `Ano` vazio para listar todos os registros cadastrados.
2. **Query acoplada à regra de apuração diária:**
   - Os endpoints `/calendario` reutilizavam `buscarCalendarioAnual` (projetada para o motor de cálculo de ponto, onde um colaborador de `Recife/PE` precisa receber feriados `NACIONAL OR ESTADUAL(PE) OR MUNICIPAL(Recife)`). Por isso, ao filtrar na tela de RH por `UF` ou `Município`, feriados nacionais e estaduais continuavam misturados no resultado.

### Como foi alterado para corrigir
- **Backend (`FeriadoRepository.java`, `FeriadoService.java`, `FeriadoController.java`, `PontoFacultativoRepository.java`, `PontoFacultativoService.java`, `PontoFacultativoController.java`):**
  - O parâmetro `ano` nos endpoints `GET /api/v1/feriado/calendario` e `GET /api/v1/ponto-facultativo/calendario` passou a ser opcional (`@RequestParam(value = "ano", required = false) Integer ano`).
  - Foram adicionados os métodos `listarAtivosPorEmpresaOuGlobais` nos repositórios e `listarCalendarioFiltrado(Integer ano, String uf, String municipio, ...)` nos services, aplicando cada filtro (`ano`, `uf`, `municipio` sem diferenciar maiúsculas/acentos) de forma **100% independente**:
    - Sem filtros preenchidos: retorna **todos os feriados e pontos facultativos ativos cadastrados no total**.
    - Preenchendo só `Ano`, só `UF`, só `Município` ou qualquer combinação entre eles: filtra estritamente pelos campos informados.
  - O método `buscarCalendarioAnual` original foi mantido intacto para o motor de apuração e espelho de ponto.
- **Frontend (`feriadoService.js` e `RhDashboard.jsx`):**
  - `anoCalendario` agora inicia vazio (`''`, placeholder `"Todos"`), exibindo de imediato a contagem e a lista total de feriados e pontos facultativos cadastrados.
  - Adicionado o botão **`Todos`** ao lado de **`Filtrar`** para limpar rapidamente qualquer filtro aplicado.

---

## 3. Câmera ficando em tela branca após bater o ponto e voltar para a tela

### Sintoma observado
- Na tela do Relógio / Estação de Ponto (`TenantPortalSlider.jsx`), após abrir a câmera uma vez, bater o ponto e aguardar o retorno do comprovante para a tela de marcação, o quadro da câmera quebrava e ficava totalmente branco em vez de fechar novamente.

### O que causava o erro (Causa Raiz)
- Quando a marcação era concluída em `handleBaterPonto`, o estado `comprovante` passava a ser preenchido e a interface trocava o card do relógio (que continha `<video ref={videoRef} />`) pelo card do comprovante, **desmontando o elemento `<video>` do DOM**.
- No entanto, a função `pararCamera()` não era chamada após o registro da batida, deixando `cameraAtiva = true`.
- Quando o comprovante fechava e a tela voltava ao relógio, um novo elemento `<video>` era montado no DOM com `cameraAtiva === true`, porém com `srcObject = null` (e também ocorria perda do `srcObject` ao alternar entre `fotoPreview` e `<video>`), resultando em um retângulo branco vazio.

### Como foi alterado para corrigir
- **Frontend (`client/src/pages/TenantPortalSlider.jsx`):**
  - `pararCamera()` agora é chamada imediatamente após o sucesso da requisição em `handleBaterPonto` (encerrando as tracks da câmera, limpando `videoRef.current.srcObject` e definindo `cameraAtiva = false`). Assim, ao voltar do comprovante para a tela de batida, a câmera já está fechada e pronta para um novo clique em **"Abrir Câmera"**.
  - O elemento `<video ref={videoRef} />` passou a permanecer montado mesmo durante a exibição de `fotoPreview` (alternando visibilidade via classe CSS `cameraAtiva && !fotoPreview ? 'block' : 'hidden'`) com um `useEffect` que garante a sincronização de `videoRef.current.srcObject = streamRef.current`.

---

## 4. Relatórios Gerenciais: Colaborador (`Luan Victor Teste`) ausente (`—`) e coluna `Detalhes` exibindo o próprio Gestor (Fotos 4 e 5)

### Sintoma observado
- Na tela **Relatórios Gerenciais** (`Horas Extras`, `Absenteísmo & Atrasos`, `Banco de Horas`), a coluna **COLABORADOR** e a coluna **INDICADOR PRINCIPAL** exibiam `—` para todas as linhas (fazendo parecer que colaboradores recém-criados como `Luan Victor Teste` não estavam na listagem, embora sua matrícula `MAT-202` estivesse lá).
- A coluna **DETALHES** exibia o nome do gestor do colaborador (ex.: `Bruno Albuquerque`, `Helena Vasconcelos` ou a própria gestora logada `Luana Vitoria Teste`).

### O que causava o erro (Causa Raiz)
- Divergência entre os nomes dos campos retornados pelo backend (`RelatorioDTO.java`) e os campos lidos pela tabela em `client/src/features/rh/RelatoriosSection.jsx`:
  1. **Coluna Colaborador:** O backend envia `nomeCompleto` nos itens de `HorasExtras`, `Absenteismo` e `BancoHoras`, mas o componente lia apenas `row.nomeColaborador || row.colaboradorNome || row.nome` (resultando sempre em `'—'`).
  2. **Coluna Indicador Principal:** O backend envia os valores em minutos (`totalMinutosExtras`, `saldoFinalMinutos`, `totalMinutosFalta`, `totalMinutosAtraso`, `percentualAbsenteismo`), mas o componente procurava strings pré-formatadas inexistentes (`totalHorasExtrasFormatado`, `saldoFormatado`, `totalFaltasFormatado`).
  3. **Coluna Detalhes:** O backend envia o nome do local em `row.localTrabalhoNome`, mas o componente lia `row.localTrabalho || row.gestorNome || row.status || 'Consolidado'`. Como `row.localTrabalho` era `undefined`, a expressão caía sempre em `row.gestorNome`, exibindo o nome do gestor no badge de detalhes.
  4. **Aba Pendências:** O backend retorna `{ abertas: [...], tratadas: [...] }`, enquanto o componente lia apenas `dados.itens`.

### Como foi alterado para corrigir
- **Frontend (`client/src/features/rh/RelatoriosSection.jsx`):**
  - **Colaborador:** passou a exibir `row.nomeCompleto || row.nomeColaborador || row.colaboradorNome || row.nome || '—'` (acompanhado do `row.cargo` abaixo do nome, quando preenchido).
  - **Indicador Principal:** adicionada formatação em `HH:mm` para os minutos retornados pelo backend em cada aba (`totalMinutosExtras` em Horas Extras; soma de faltas/atrasos/saídas antecipadas + `%` em Absenteísmo; `saldoFinalMinutos` com sinal `+/-` em Banco de Horas; `tipo` em Pendências).
  - **Detalhes:** passou a exibir os indicadores secundários reais do período junto com `row.localTrabalhoNome` (ex.: quantidade de dias com extra, resumo de faltas/atrasos/abonos, créditos/débitos do banco de horas e data/situação da pendência), sem exibir o nome do gestor como detalhe.
  - **Pendências:** unificadas as listas `dados.abertas` e `dados.tratadas`.

---

## 5. Impossibilidade de redefinir a jornada de colaborador que já possuía jornada (Foto 6)

### Sintoma observado
- Ao tentar vincular uma nova jornada ou escala a um colaborador que já tinha um regime ativo em **Vincular Regime ao Colaborador**, o sistema bloqueava a operação com o erro: `"O colaborador já possui um regime vigente com período sobreposto."`.

### O que causava o erro (Causa Raiz)
- Os vínculos de regime (`RegimeTrabalho`) normalmente são criados com vigência em aberto (`data_fim_vigencia = NULL`).
- Em `RegimeTrabalhoService.java`, o método `vincular` consultava `regimeRepository.findSobrepostos(...)` e lançava `RegimeTrabalhoConflitoException` caso existisse qualquer regime aberto ou sobreposto, sem encerrar automaticamente a vigência anterior.

### Como foi alterado para corrigir
- **Backend (`RegimeTrabalhoService.java`):**
  - Ao vincular um novo regime para um colaborador que já possui regime(s) sobreposto(s):
    - Se o regime anterior iniciou **antes** da nova `dataInicioVigencia`, sua `dataFimVigencia` é encerrada automaticamente no dia imediatamente anterior (`dto.dataInicioVigencia().minusDays(1)`), preservando o histórico passado.
    - Se o regime anterior iniciou **na mesma data ou após** a nova `dataInicioVigencia`, ele é substituído pelo novo vínculo.

---

## 6. Colunas `cargo` e `email` ficando `NULL` no banco sem opção de preenchimento/edição na interface (Foto 7)

### Sintoma observado
- Na tabela `funcionario` do PostgreSQL, as colunas `cargo` e `email` apareciam como `NULL` para os colaboradores criados pelo sistema e para os colaboradores do seeder de apresentação, não existindo campos na interface do RH para preenchê-los ou editá-los.

### O que causava o erro (Causa Raiz)
- Embora a entidade JPA `Funcionario.java` possuísse os atributos `cargo` e `email`, eles não estavam expostos nos records `FuncionarioDTO.Criar` e `FuncionarioDTO.Response`, não eram persistidos em `FuncionarioService.criar(...)`, não eram preenchidos no `PitchDataSeeder.java` e não possuíam inputs nem modal de edição em `ColaboradoresSection.jsx`.

### Como foi alterado para corrigir
- **Backend (`FuncionarioDTO.java`, `FuncionarioService.java`, `FuncionarioController.java`, `PitchDataSeeder.java`):**
  - Adicionados `cargo` e `email` em `FuncionarioDTO.Criar` (mantendo construtor sobrecarregado de 10 argumentos para retrocompatibilidade com testes), `FuncionarioDTO.Atualizar` e `FuncionarioDTO.Response`.
  - `FuncionarioService.criar` agora persiste `cargo` e `email`.
  - Criado o método `FuncionarioService.atualizar(UUID id, FuncionarioDTO.Atualizar dto)` e o endpoint `PUT /api/v1/rh/funcionarios/{id}` em `FuncionarioController.java` para permitir editar nome, cargo, e-mail, perfil, local de trabalho e gestor direto.
  - `PitchDataSeeder.java` foi atualizado para preencher `cargo` e `email` automaticamente nos colaboradores da empresa modelo (inclusive atualizando registros existentes que estivessem com `cargo` ou `email` nulos no startup).
- **Frontend (`client/src/services/rhService.js` e `client/src/features/rh/ColaboradoresSection.jsx`):**
  - Adicionados os campos **Cargo** e **E-mail** no formulário **Novo Colaborador**.
  - Exibidos o badge de **Cargo** e o **E-mail** na listagem do **Quadro Funcional**.
  - Adicionado botão de edição (ícone de lápis) em cada colaborador da lista, abrindo o modal **Editar Colaborador** para preencher ou modificar `Cargo`, `E-mail`, `Nome Completo`, `Perfil`, `Local` e `Gestor Direto`.

---

## 7. Validação Executada

- **Testes Unitários do Backend:**
  - Comando: `./server/pontoCerto/mvnw test -f server/pontoCerto/pom.xml -Dtest="!PontoCertoApplicationTests"`
  - Resultado: **46 testes executados, 0 falhas, 0 erros, 0 ignorados (`BUILD SUCCESS`)**.
- **Build de Produção do Frontend:**
  - Comando: `npm --prefix client run build`
  - Resultado: **Bundle compilado com sucesso em `client/dist/` (`0` erros)**.
