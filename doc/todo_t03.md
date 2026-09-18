# Lista de Afazeres (TODO) - Tarefa T03: Colaboradores, Matrícula, CPF e PIS

Este documento contém o plano de ação detalhado para deixar o backend e a integração do sistema **100% em conformidade** com os requisitos e critérios de aceite da **Tarefa T03**.

---

## 🎯 Critérios de Aceite da Tarefa T03
- [ ] **C1:** Cadastro com nome, matrícula, CPF, PIS, data de admissão, local e gestor
- [ ] **C2:** Matrícula, CPF e PIS são únicos dentro da empresa; repetido é recusado
- [ ] **C3:** CPF e PIS têm o dígito verificador conferido antes de gravar
- [ ] **C4:** Desligamento registra a data e impede novas batidas na estação
- [ ] **C5:** Colaborador com marcação não pode ser excluído, e a tela diz o motivo
- [ ] **C6:** A busca acha o colaborador por nome, matrícula, CPF ou PIS

---

## 🛠️ BACKEND (Foco Principal)

### 1. Correções Imediatas & Desbloqueio de Compilação
- [x] **Restaurar método de busca por usuário no repositório:**
  - Descomentar/recriar `Optional<Funcionario> findByUsuario(String usuario)` e `boolean existsByUsuario(String usuario)` em `FuncionarioRepository.java` para sanar os erros de compilação em `UserDetailsServiceImpl.java` e `FuncionarioController.java`.
- [ ] **Corrigir tipo do parâmetro de busca por termo:**
  - Em `FuncionarioRepository.java`, alterar `@Param("empresaId") Long empresaId` para `UUID empresaId`.
- [ ] **Padronização de nomenclatura sem caracteres especiais:**
  - Renomear a classe e o arquivo `CPFJáCadastradoException.java` para `CpfJaCadastradoException.java` (remover o acento `á` para evitar problemas em builds e ambientes CI/CD).

---

### 2. Modelo de Dados & Entidade `Funcionario`
- [ ] **Vincular Local de Trabalho (Critério C1):**
  - Adicionar o relacionamento `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "local_trabalho_id") private LocalDeTrabalho localTrabalho;` na entidade `Funcionario.java`.
  - Criar os métodos `getLocalTrabalho()` e `setLocalTrabalho(LocalDeTrabalho localTrabalho)`.
- [ ] **Vínculo de Gestor (Critério C1):**
  - Garantir que o auto-relacionamento `@ManyToOne @JoinColumn(name = "gestor_id") private Funcionario gestor;` já existente possa ser alimentado corretamente durante a criação.
- [ ] **Campos de Admissão e Desligamento (Critérios C1 e C4):**
  - Permitir que `dataAdmissao` receba o valor fornecido na requisição (não travado em `LocalDate.now()`).
  - Garantir que `dataDesligamento` possa ser preenchido/atualizado na operação de desligamento.

---

### 3. DTOs (Data Transfer Objects)
- [ ] **Expandir `FuncionarioDTO.Criar` (Critério C1, C2, C3):**
  - Adicionar campos com validações do `jakarta.validation`:
    ```java
    public record Criar(
        @NotNull(message = "O ID da empresa é obrigatório")
        UUID empresaId,

        @NotBlank(message = "O nome completo é obrigatório")
        String nomeCompleto,

        @NotBlank(message = "O usuário de login é obrigatório")
        String usuario,

        @NotBlank(message = "A senha inicial é obrigatória")
        String senha,

        @NotNull(message = "O perfil de acesso é obrigatório")
        PerfilAcesso perfilAcesso,

        @NotBlank(message = "A matrícula é obrigatória")
        String matricula,

        @NotBlank(message = "O CPF é obrigatório")
        @org.hibernate.validator.constraints.br.CPF(message = "CPF com dígito verificador inválido")
        String cpf,

        @NotBlank(message = "O PIS/PASEP é obrigatório")
        String pis_pasep,

        @NotNull(message = "A data de admissão é obrigatória")
        LocalDate dataAdmissao,

        @NotNull(message = "O local de trabalho é obrigatório")
        UUID localTrabalhoId,

        UUID gestorId
    ) {}
    ```
- [ ] **Criar `FuncionarioDTO.Desligamento` (Critério C4):**
  ```java
  public record Desligamento(
      @NotNull(message = "A data de desligamento é obrigatória")
      LocalDate dataDesligamento
  ) {}
  ```
- [ ] **Atualizar `FuncionarioDTO.Response`:**
  - Incluir `localTrabalhoId`, `localTrabalhoNome`, `gestorId`, `gestorNome`, `dataAdmissao` e `dataDesligamento`.

---

### 4. Camada de Serviço (`FuncionarioService`)
- [ ] **Sanitização de Documentos (Critério C2 e C3):**
  - Implementar método utilitário para sanitizar caracteres não-numéricos de CPF e PIS (`valor.replaceAll("\\D", "")`), garantindo que consultas de unicidade não falhem por pontuações divergentes (`.` ou `-`).
- [ ] **Validação de Dígitos Verificadores (Critério C3):**
  - Ajustar `isPisValido(String pis)` para tratar casos de `null`, tamanho diferente de 11 e sequências de dígitos idênticos (ex: `00000000000`).
  - Assegurar que tanto CPF quanto PIS sejam conferidos antes de persistir.
- [ ] **Unicidade de Matrícula, CPF e PIS por Empresa (Critério C2):**
  - Verificar existência usando os valores sanitizados:
    - `existsByMatriculaAndEmpresaId`
    - `existsByCPFAndEmpresaId`
    - `existsByPisPasepAndEmpresaId`
  - Lançar exceções específicas se algum já estiver em uso na mesma empresa.
- [ ] **Cadastro Completo com Associações (Critério C1):**
  - Validar e associar `LocalDeTrabalho` (conferindo se pertence à empresa informada).
  - Validar e associar `gestor` (se informado, conferindo se pertence à mesma empresa).
  - Setar a `dataAdmissao` informada no DTO.
- [ ] **Operação de Desligamento (Critério C4):**
  - Criar método `public FuncionarioDTO.Response desligar(UUID id, FuncionarioDTO.Desligamento dto)`.
  - Validar se `dataDesligamento` é posterior ou igual a `dataAdmissao`.
  - Atualizar a entidade com a data de desligamento.
- [ ] **Regra de Bloqueio de Batidas para Desligados (Critério C4):**
  - Criar método auxiliar de validação (ex: `public boolean podeRegistrarPonto(UUID funcionarioId, LocalDate dataBatida)`):
    - Se o colaborador possuir `dataDesligamento != null` e `dataBatida.isAfter(dataDesligamento)` (ou data anterior à admissão), retornar `false` / lançar exceção de batida inválida.
- [ ] **Regra de Exclusão com Verificação de Marcações (Critério C5):**
  - Criar método `public void excluir(UUID id)`.
  - Verificar se o funcionário possui marcações de ponto associadas (ex: via contagem em repositório de ponto ou verificação de integridade).
  - Se possuir marcações: lançar `FuncionarioPossuiMarcacoesException("Não é possível excluir o colaborador porque ele já possui marcações de ponto registradas.")`.
  - Se não possuir marcações: executar `repository.delete(funcionario)`.
- [ ] **Busca Multifuncional (Critério C6):**
  - Criar método `public List<FuncionarioDTO.Response> buscar(UUID empresaId, String termo)`.
  - Se `termo` for nulo ou em branco, listar todos da empresa.
  - Se preenchido, consultar `buscarPorTermo` (passando o termo original e a versão desformatada/apenas dígitos para CPF/PIS).

---

### 5. Camada de Controle (`FuncionarioController`)
- [ ] **Ativar Validação no Cadastro (Critério C3):**
  - Adicionar `@Valid` no endpoint de cadastro:
    ```java
    @PostMapping("/rh/adicionar") // ou @PostMapping("/rh/funcionarios")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<FuncionarioDTO.Response> criarFuncionario(@Valid @RequestBody FuncionarioDTO.Criar dto)
    ```
- [ ] **Endpoint de Busca de Colaboradores (Critério C6):**
  - Criar `GET /api/rh/funcionarios`:
    - Parâmetros: `@RequestParam UUID empresaId` e `@RequestParam(required = false) String termo`.
    - Retornar status `200 OK` com a lista encontrada.
- [ ] **Endpoint de Desligamento (Critério C4):**
  - Criar `PATCH /api/rh/funcionarios/{id}/desligar`:
    - Receber `@PathVariable UUID id` e `@Valid @RequestBody FuncionarioDTO.Desligamento dto`.
    - Retornar status `200 OK` com os dados atualizados do colaborador.
- [ ] **Endpoint de Exclusão (Critério C5):**
  - Criar `DELETE /api/rh/funcionarios/{id}`:
    - Retornar status `204 No Content` caso removido com sucesso.

---

### 6. Tratamento de Exceções Global (`@RestControllerAdvice`)
- [ ] Mapear as exceções do domínio de funcionários para status HTTP semânticos:
  - `CpfJaCadastradoException`, `MatriculaJaCadastradaException`, `PisPasepJaCadastradoException`, `UsuarioDuplicadoException` -> **409 Conflict**
  - `PisPasepInvalidoException`, `MethodArgumentNotValidException`, `IllegalArgumentException` -> **400 Bad Request**
  - `FuncionarioNaoEncontradoException` -> **404 Not Found**
  - `FuncionarioPossuiMarcacoesException` -> **422 Unprocessable Entity** ou **400 Bad Request** com corpo estruturado:
    ```json
    {
      "erro": "COLABORADOR_COM_MARCACOES",
      "mensagem": "Não é possível excluir o colaborador porque ele já possui marcações de ponto registradas. Para colaboradores com histórico, registre o desligamento."
    }
    ```

---

### 7. Testes Automatizados Unitários (`FuncionarioServiceTest`)
- [ ] Criar classe `FuncionarioServiceTest.java` com Mockito testando:
  - `deveCadastrarColaboradorComSucessoComTodosOsCampos()` (C1)
  - `deveRecusarCadastroComCpfDuplicadoNaMesmaEmpresa()` (C2)
  - `deveRecusarCadastroComMatriculaDuplicadaNaMesmaEmpresa()` (C2)
  - `deveRecusarCadastroComPisDuplicadoNaMesmaEmpresa()` (C2)
  - `deveRecusarCadastroComPisInvalido()` (C3)
  - `deveRegistrarDesligamentoComSucesso()` (C4)
  - `deveImpedirBatidaAposDataDesligamento()` (C4)
  - `deveImpedirExclusaoDeColaboradorComMarcacoes()` (C5)
  - `deveExcluirColaboradorSemMarcacoes()` (C5)
  - `deveBuscarColaboradoresPorNomeMatriculaCpfOuPis()` (C6)

---

## 💻 FRONTEND (Planejamento Futuro / Registro)

- [ ] **Formulário Completo de Cadastro (`RhDashboard.jsx`):**
  - Adicionar inputs para:
    - Matrícula
    - CPF (com máscara `000.000.000-00`)
    - PIS (com máscara `000.00000.00-0`)
    - Data de Admissão (`type="date"`)
    - Select do Local de Trabalho (carregado via `rhService.listarLocaisTrabalho()`)
    - Select de Gestor (carregado via colaboradores com perfil `GESTOR`)
- [ ] **Barra de Busca Multifuncional:**
  - Campo de busca por texto livre consultando `GET /api/rh/funcionarios?termo=...`.
- [ ] **Tabela de Colaboradores Cadastrados:**
  - Exibir colunas: Nome, Matrícula, CPF, PIS, Local, Admissão, Desligamento, Status (Ativo/Desligado).
- [ ] **Ação de Desligamento:**
  - Botão "Desligar" abrindo modal com seletor de data e confirmação.
- [ ] **Ação de Exclusão com Feedback de Motivo (Critério C5):**
  - Botão de lixeira/excluir.
  - Se a API retornar erro de colaborador com marcações, exibir toast/alerta com o motivo claro:
    *"Não é possível excluir este colaborador pois ele possui marcações de ponto registradas."*
