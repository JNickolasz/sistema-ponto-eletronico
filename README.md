# Ponto Certo — SaaS Multi-Tenant de Ponto Eletrônico & Gestão de Jornada

Sistema SaaS **Multi-Tenant** e **White-Label** de Ponto Eletrônico, Apuração de Frequência e Fechamento de Folha desenvolvido conforme a **Portaria 671/MTP** e a **LGPD**.

Projeto desenvolvido por **José Nickolas**, **Luan Victor** e **Lucas Alves** para a disciplina de **Projeto de Banco de Dados (2026.2)**.

---

## Repositório & Documentação Técnica

- **GitHub:** [https://github.com/JNickolasz/sistema-ponto-eletronico](https://github.com/JNickolasz/sistema-ponto-eletronico)
- **Dicionário de Dados Completo (27 Tabelas + Sequence):** [`doc/data_dictionary.md`](doc/data_dictionary.md)
- **Layout Determinístico de Exportação para Folha:** [`doc/layout-exportacao-folha.md`](doc/layout-exportacao-folha.md)
- **Relatório de Auditoria de Segurança e Correções:** [`docs/auditoria-rodada-2-2026-11-21.md`](docs/auditoria-rodada-2-2026-11-21.md)

---

## Tecnologias Utilizadas

- **Frontend:** React 19 + Vite 8 + Tailwind CSS v4 + Lucide Icons + Axios (Temas Claro/Escuro nativos com `light-dark()` e variáveis CSS dinâmicas por tenant)
- **Backend:** Java 21 + Spring Boot 4.1.1 + Spring Security 7 (JWT + RBAC + Isolamento Multi-Tenant) + Spring Data JPA / Hibernate 7
- **Banco de Dados:** PostgreSQL (Hospedado em nuvem via **Neon.tech**, com suporte a `JSONB`, sequences monotônicas e constraints compostas por empresa)

---

## Arquitetura Multi-Tenant por Subdomínio & White-Label

O **Ponto Certo** opera no modelo **SaaS Multi-Tenant por subdomínio** (estilo 1Doc), separando a vitrine comercial/administração global do ambiente exclusivo de cada empresa contratante:

### 1. Domínio Raiz (`http://localhost:5173`)
- **Landing Page Comercial (`/`)**: Apresentação institucional do SaaS com identidade padrão Azul & Branco, alternância de tema Claro/Escuro e navegação *Scrollspy* fluida.
- **Portal de Acesso Raiz (`/login`)**:
  - **Aba "Sou Empresa"**: Localiza o subdomínio da empresa cliente e redireciona para `http://{subdominio}.localhost:5173/login`.
  - **Aba "Admin do Sistema"**: Autenticação global do Super Admin SaaS via chave administrativa (`X-Admin-Key`), sem exigir subdomínio.
- **Painel Super Admin SaaS (`/admin`)**:
  - Estatísticas consolidadas de toda a plataforma (total de empresas, colaboradores ativos, locais, equipamentos e batidas).
  - Inspeção individual de cada empresa cliente e provisionamento de novas empresas + primeiro usuário de RH.
  - **Editor White-Label ao Vivo**: Personalização da cor primária e logo (`config_whitelabel` em `JSONB` no PostgreSQL) de qualquer empresa cliente em tempo real.
  - Gestão global de Feriados Nacionais.

### 2. Subdomínio Corporativo da Empresa (`http://{subdominio}.localhost:5173`)
Ao acessar o subdomínio de uma empresa cadastrada (ex: `http://vertice.localhost:5173`), o frontend consome `GET /api/v1/public/tenant/{subdominio}` e aplica automaticamente a logomarca, razão social, paleta de cores e estações ativas daquela empresa:
- **Portal Deslizante Horizontal (`/` ↔ `/login`)**:
  - **Relógio de Ponto Principal (`/`)**: Terminal de recepção/corredor para registro de ponto sem sair da tela, com relógio sincronizado com o servidor (`GET /api/v1/batidas/hora-atual`), captura de foto via câmera WebRTC, pareamento automático da estação ativa e emissão imediata do comprovante oficial com NSR (Portaria 671).
  - **Login Corporativo (`/login`)**: Transição lateral suave para autenticação nos painéis internos com proteção cruzada de subdomínio (`X-Tenant-Subdomain`).
- **Painel do RH (`/rh`)**: Gestão completa da empresa — Locais de Trabalho, Equipamentos (Relógios AFD e Estações Web), Colaboradores, Jornadas, Escalas, Feriados, Importação Posicional AFD (2 etapas com preview), Batidas Órfãs, Fila de Inconsistências, Espelho & Apuração, Ocorrências & Abonos, Banco de Horas, Fechamento de Período, Relatórios Gerenciais e Exportação determinística para Folha (CSV/JSON com hash SHA-256).
- **Painel do Gestor (`/gestor`)**: Acompanhamento exclusivo dos colaboradores liderados (`gestor_id`), análise de solicitações de ajuste (deferimento/indeferimento com geração automática de batida manual e re-apuração), espelho diário e saldo de banco de horas da equipe.
- **Painel do Colaborador (`/colaborador`)**: Consulta do próprio espelho de ponto, comprovantes Portaria 671 com download/impressão, extrato e histórico de fechamentos do banco de horas, ocorrências/atestados e abertura de solicitação de ajuste.

---

## Estrutura do Projeto

```text
pbdProject/
├── client/                  # Aplicação Frontend (React 19 + Vite 8 + Tailwind v4)
│   ├── public/logos/        # Logos vetoriais das empresas White-Label
│   └── src/
│       ├── components/      # Módulos de negócio e primitivos de UI (BrandLogo, Modal, ThemeToggle)
│       ├── context/         # AuthContext e ThemeContext (Light/Dark + White-Label por subdomínio)
│       ├── features/rh/     # Seções modulares do painel de Departamento Pessoal (RH)
│       ├── layouts/         # AppShell unificado com sidebar, breadcrumbs e tema dinâmico
│       ├── pages/           # LandingPage, LoginPage, TenantPortalSlider, Admin/Rh/Gestor/Colaborador
│       ├── services/        # Clientes HTTP Axios integrados à API REST
│       └── utils/           # Resolução de subdomínio multi-tenant e validadores de documentos (CPF/PIS/CNPJ)
├── server/
│   └── pontoCerto/          # API REST Backend (Java 21 + Spring Boot 4.1.1)
│       └── src/main/java/.../pontoCerto/
│           ├── config/      # SecurityConfig, JwtAuthenticationFilter, PitchDataSeeder
│           ├── domain/      # 27 Entidades JPA + Enums
│           ├── dto/         # Contratos de entrada/saída e validações
│           ├── repository/  # Repositórios Spring Data JPA escopados por tenant
│           ├── resource/    # Controllers REST (/api/v1/**)
│           └── service/     # Motores de apuração, importação AFD, banco de horas e fechamento
├── doc/                     # Dicionário de dados e layout de exportação para folha
├── docs/                    # Relatórios de auditoria de segurança
└── README.md                # Documentação geral
```

---

## Pré-requisitos

- **Java 21**
- **Maven** (ou utilizar o `./mvnw` incluso no projeto)
- **Node.js 20+** e **npm**

---

## Configuração e Execução do Backend

A API utiliza variáveis de ambiente para conexão com o banco PostgreSQL (Neon.tech), assinatura de tokens JWT e chave do Super Admin SaaS.

### 1. Variáveis de Ambiente

| Variável | Obrigatória | Descrição |
| :--- | :---: | :--- |
| `DB_URL` | Sim | JDBC URL do banco PostgreSQL (ex: `jdbc:postgresql://.../neondb?sslmode=require`) |
| `DB_USER` | Sim | Usuário do banco PostgreSQL |
| `DB_PASSWORD` | Sim | Senha do banco PostgreSQL |
| `JWT_SECRET` | Sim (Prod) | Segredo para assinatura HMAC-SHA256 dos tokens JWT |
| `APP_ADMIN_KEY` | Sim (Admin) | Chave secreta validada no header `X-Admin-Key` para acesso ao painel `/admin` |
| `APP_SEED_PITCH` | Opcional | Quando `true`, popula de forma idempotente a empresa modelo **Vértice Engenharia & Sistemas Ltda** (`subdominio: vertice`) no banco real |
| `APP_SEED_PITCH_PASSWORD` | Opcional | Senha atribuída aos usuários da empresa modelo (`rh.vertice`, `gestor.eng`, `camila.dev`, etc.) quando `APP_SEED_PITCH=true` |

### 2. Executando pelo Terminal

```bash
cd server/pontoCerto

export DB_URL="sua_jdbc_url_aqui"
export DB_USER="seu_usuario_aqui"
export DB_PASSWORD="sua_senha_aqui"
export APP_ADMIN_KEY="sua_chave_super_admin"

# Opcional na primeira execução para popular a empresa demonstrativa (Vértice Engenharia):
APP_SEED_PITCH=true APP_SEED_PITCH_PASSWORD="sua_senha_demo" ./mvnw spring-boot:run
```

> **Dica para IntelliJ IDEA:** Em **Run/Debug Configurations → Modify options → Environment variables**, adicione `DB_URL`, `DB_USER`, `DB_PASSWORD`, `APP_ADMIN_KEY` (e opcionalmente `APP_SEED_PITCH=true;APP_SEED_PITCH_PASSWORD=...` na primeira execução).

---

## Configuração e Execução do Frontend

```bash
cd client
npm install
npm run dev
```

### Endereços Locais para Teste e Apresentação

- **Landing Page Comercial (Domínio Raiz):** `http://localhost:5173`
- **Login Raiz (Localizador de Subdomínio / Super Admin SaaS):** `http://localhost:5173/login`
- **Empresa Modelo White-Label (Relógio de Ponto + Portal Deslizante):** `http://vertice.localhost:5173`
  - **Perfil RH:** usuário `rh.vertice`
  - **Perfil Gestor:** usuário `gestor.eng`
  - **Perfil Colaborador:** usuário `camila.dev` (ou `lucas.back`, `rafael.qa`, `mariana.campo`)

---

## Testes e Build

### Backend (40 testes automatizados de unidade e regressão de segurança)
```bash
cd server/pontoCerto
./mvnw test
```

### Frontend (Verificação de lint e bundle de produção)
```bash
cd client
npm run build
```

---

## Licença

Projeto acadêmico desenvolvido para a disciplina de Projeto de Banco de Dados.
