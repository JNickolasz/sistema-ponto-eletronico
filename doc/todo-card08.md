# TODO - Issue T08: Comprovante de Marcação e Geração de NSR

## 1. Banco de Dados (PostgreSQL)
Para garantir que o número nunca se repita ou retroceda (mesmo em requisições simultâneas) sem travar a tabela toda, usaremos uma `SEQUENCE` nativa do banco.

*   **Script SQL (Migration):**
    ```sql
    -- Criação da sequência que gerenciará o NSR isoladamente
    CREATE SEQUENCE seq_nsr_batida START 1 INCREMENT 1;

    -- Adição da coluna na tabela existente
    ALTER TABLE batida 
    ADD COLUMN nsr BIGINT NOT NULL DEFAULT nextval('seq_nsr_batida');
    
    -- Constraint para garantir que nunca haverá duplicidade no banco
    ALTER TABLE batida ADD CONSTRAINT uk_batida_nsr UNIQUE (nsr);
    ```

## 2. Backend (Spring Boot)
O backend não deve calcular o NSR. Ele apenas delega ao banco e retorna o pacote completo para o frontend. Além disso, a modelagem da entidade principal deve ser blindada para garantir a imutabilidade exigida pela legislação.

*   **Mapeamento da Entidade (`Batida.java`):**
    Pela Portaria 671, um registro original de batida **nunca** pode ser modificado. Se o RH precisar corrigir um horário, o sistema deve criar um registro de "Tratativa" à parte, mantendo a batida original intacta. Por isso, a regra de ouro aqui é o `updatable = false`.
    *   `id` (`UUID`): Chave primária padrão. Anotações: `@Id`, `@GeneratedValue`, `@Column(updatable = false, nullable = false)`.
    *   `nsr` (`Long`): O Número Sequencial de Registro. Anotações: `@Column(name = "nsr", updatable = false, insertable = false)`. Deve usar `@Generated(GenerationTime.INSERT)` (ou equivalente da sua versão do Hibernate) para o JPA saber que o banco vai preencher isso sozinho via *sequence*.
    *   `funcionario` (`Funcionario`): Vínculo com quem bateu o ponto. Anotações: `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "funcionario_id", nullable = false, updatable = false)`. Uma batida nunca pode trocar de dono.
    *   `dataHora` (`LocalDateTime` ou `OffsetDateTime`): O momento exato do registro. **Lógica vital:** Esse valor deve ser preenchido instanciando a hora atual dentro do Service/Backend (ex: `LocalDateTime.now()`), ignorando qualquer data/hora que venha no JSON do frontend para evitar fraudes. Anotações: `@Column(name = "data_hora", nullable = false, updatable = false)`.
    *   `tipo` (`Enum` - Opcional, dependendo da sua arquitetura): Muitas vezes os sistemas salvam se foi Entrada, Saída, etc. Se existir, também deve ser `nullable = false` e `updatable = false`.

*   **DTO de Resposta (`ComprovanteBatidaDTO`):**
    Criar um objeto que retorne tudo o que a Portaria 671 exige ao registrar o ponto:
    *   `nomeEmpresa` e `cnpjEmpresa`
    *   `nomeFuncionario` e `cpfFuncionario`
    *   `dataHoraBatida` (Timestamp oficial do servidor)
    *   `nsr` (O número gerado)

*   **Endpoints Necessários (Controller):**
    *   `POST /api/batidas`: Registra o ponto (sem receber data/hora no body) e retorna o `ComprovanteBatidaDTO` com status HTTP 201.
    *   `GET /api/funcionarios/{id}/batidas`: Retorna a lista de batidas (com seus respectivos NSRs) para o histórico.

## 3. Frontend (React)
O colaborador precisa ver o comprovante imediatamente após a batida e ter a opção de consultá-lo depois.

*   **Componente `<ComprovanteModal />` ou `<ComprovantePrint />`:**
    Criar um componente visual focado em leitura/impressão, exibindo as informações do DTO recebido.
*   **Fluxo de Marcação:**
    Ao clicar em "Bater Ponto", o `.then()` da requisição deve abrir o componente de comprovante com os dados retornados pelo `POST`.
*   **Fluxo de Histórico (Re-impressão):**
    Na tela de "Meus Pontos", adicionar um botão "Ver Comprovante" ao lado de cada registro da tabela. Ele abrirá o mesmo componente criado acima, passando os dados da batida selecionada.

## 4. Testes e Validações (QA)
A prova de fogo deste card é garantir a integridade contra concorrência.

*   **Teste de Carga (Integração no Spring Boot):**
    Criar um teste com `@SpringBootTest` usando `ExecutorService` (multithreading).
    *   Disparar 100 requisições simultâneas para o método de salvar batida.
    *   Fazer um `assertEquals(100, repository.count())` para garantir que todas salvaram.
    *   Fazer um `SELECT COUNT(DISTINCT nsr) FROM batida` e garantir que o resultado também seja 100 (nenhum NSR sobreposto).
*   **Teste Visual:**
    Garantir que a hora exibida no comprovante é a hora que veio do banco de dados (backend), e não a hora do sistema operacional local do colaborador (evitando fraudes de alteração de relógio no Windows/Celular).

---

## ✅ Checklist de Critérios de Aceite
- [ ] Coluna `nsr` criada no banco com `SEQUENCE` para evitar concorrência.
- [ ] Entidade `Batida.java` mapeada com bloqueios de atualização (`updatable = false`) em todos os campos.
- [ ] Endpoint de registro devolve os dados completos do comprovante (NSR, Empresa, Colaborador, Data/Hora).
- [ ] Frontend exibe o comprovante em tela logo após o registro da batida com sucesso.
- [ ] Frontend possui tela/aba de histórico permitindo visualizar/imprimir comprovantes antigos.
- [ ] Data/Hora do comprovante são geradas obrigatoriamente pelo backend.
- [ ] Teste automatizado de concorrência confirma que registros paralelos não duplicam o NSR.