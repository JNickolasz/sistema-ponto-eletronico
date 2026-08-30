# Ponto Certo

Sistema de ponto eletrônico desenvolvido para registrar jornada de trabalho, controlar entradas e saídas e facilitar a gestão de funcionários e marcações de ponto.

Projeto desenvolvido por **José Nickolas**, **Luan Victor** e **Lucas Alves**.

## Repositório

- GitHub: [https://github.com/JNickolasz/sistema-ponto-eletronico](https://github.com/JNickolasz/sistema-ponto-eletronico)

## Tecnologias

- **Frontend:** React + Vite + Material UI + Tailwind CSS
- **Backend:** Java 21 + Spring Boot 4.x + Spring Security + JPA
- **Banco de dados:** PostgreSQL (Hospedado na nuvem via Neon.tech)

## Estrutura do projeto

```text
pbdProject/
├── client/                  # Aplicação frontend em React
├── server/
│   └── pontoCerto/         # API REST em Spring Boot
├── doc/                    # Documentação do projeto
├── README.md               # Documentação geral
└── .gitignore              # Arquivos ignorados pelo Git
```

## Funcionalidades previstas

O sistema foi pensado para atender perfis com isolamento de acesso (RH, Gestor e Colaborador) e para incluir regras de auditoria, sem permitir a exclusão de marcações originais.

- **Dupla entrada de ponto:** registro por estação web e importação de arquivos posicionalmente estruturados de relógios físicos.
- **Tratamento de inconsistências:** fila para análise de batidas órfãs, dias ímpares, marcações fora da jornada e equipamentos desconhecidos.
- **Apuração de frequência:** cálculo de horas trabalhadas, atrasos, extras por faixa, adicional noturno e banco de horas.
- **Gestão de justificativas:** solicitação de ajuste pelo colaborador e validação pelo gestor.
- **Fechamento de período:** processo de encerramento mensal e geração de dados consolidados para integração com sistemas de pagamento.

## Pré-requisitos

Antes de rodar o projeto, certifique-se de ter instalado:

- Node.js 20+
- npm ou yarn
- Java 21
- Maven ou Maven Wrapper

## Configuração do backend

A API utiliza variáveis de ambiente para configurar a conexão com o banco de dados hospedado na nuvem.

No terminal, dentro da pasta do backend:

```bash
cd server/pontoCerto

export DB_URL=jdbc:postgresql://ep-crimson-smoke-ayozdouc-pooler.c-5.us-east-2.aws.neon.tech/neondb
export DB_USER=neondb_owner
export DB_PASSWORD=sua_senha_aqui

./mvnw spring-boot:run
```

> O arquivo `application.properties` do projeto lê essas variáveis com a sintaxe `${DB_URL}`, `${DB_USER}` e `${DB_PASSWORD}`. Lembre-se de configurar essas variáveis de ambiente na sua IDE (ex: IntelliJ) para rodar o projeto localmente.

## Configuração do frontend

No terminal, dentro da pasta do cliente:

```bash
cd client
npm install
npm run dev
```

Depois, abra o endereço exibido no terminal pelo Vite (normalmente `http://localhost:5173`).

## Scripts úteis

### Backend

```bash
cd server/pontoCerto
./mvnw clean install
./mvnw test
./mvnw spring-boot:run
```

### Frontend

```bash
cd client
npm install
npm run dev
npm run build
```

## Observações

- Este projeto foi estruturado em arquitetura cliente-servidor.
- O backend será responsável pela lógica de negócio e pelo acesso ao banco.
- O frontend consumirá os endpoints da API para fornecer a interface web.
- Evite versionar arquivos sensíveis como `.env`, arquivos `.pem`, chaves privadas e credenciais do banco.

## Licença

Este projeto é destinado ao uso acadêmico e de desenvolvimento, conforme a necessidade do curso ou da equipe responsável.