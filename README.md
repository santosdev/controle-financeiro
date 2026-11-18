# 💰 FinanceControl — Gestão Financeira Web

Sistema web de controle financeiro pessoal feito com **Java 21, Spring Boot e MySQL**. Cada pessoa cria a sua conta, registra receitas e despesas, organiza por categorias e acompanha o saldo e a evolução mensal em um dashboard.

## 🎯 Objetivo

Aplicação completa de portfólio, do banco de dados à interface: API REST, autenticação segura, isolamento de dados entre usuários, interface responsiva com tema claro e escuro e testes automatizados.

## ⚙️ Funcionalidades

**Finanças**
- Cadastro, edição e exclusão de receitas e despesas.
- Categorias próprias de cada usuário (11 categorias iniciais, que podem ser editadas).
- Filtros por período, categoria e tipo.
- Saldo, total de receitas e total de despesas.
- Dashboard com evolução mensal e despesas/receitas por categoria, com gráficos feitos sem biblioteca externa.

**Conta e segurança**
- Cadastro com **confirmação por e-mail**.
- Login com e-mail e senha, ou **"Continuar com o Google"** (opcional).
- "Esqueci minha senha" com link de uso único, válido por 1 hora.
- Bloqueio do login por 15 minutos após 5 senhas erradas.
- Senhas guardadas apenas como hash **BCrypt**; links de e-mail guardados apenas como hash.
- Sessão por cookie `HttpOnly`, proteção **CSRF** e cabeçalhos de segurança (incluindo Content-Security-Policy).
- **Cada usuário enxerga somente os próprios dados**: acessar o ID de outra pessoa retorna "não encontrado".

## 🛠️ Tecnologias

| Tecnologia | Finalidade |
|---|---|
| Java 21 | Linguagem |
| Spring Boot 4 | Backend e API REST |
| Spring Security (+ OAuth2 Client) | Login, sessão, CSRF e Google |
| Spring Data JPA / Hibernate | Persistência |
| MySQL | Banco de dados |
| Flyway | Versionamento do banco (migrations) |
| Spring Mail | Envio de e-mails (SMTP) |
| HTML5, CSS3 e JavaScript | Interface, sem frameworks |
| JUnit 5, Mockito e AssertJ | Testes automatizados |
| Maven | Build e dependências |

## 📁 Estrutura do projeto

```text
controle-financeiro/
├── docs/
│   └── LOGIN.md                      # configuração de e-mail e Google
├── src/
│   ├── main/
│   │   ├── java/com/santosdev/financecontrol/
│   │   │   ├── controller/           # endpoints REST
│   │   │   ├── service/              # regras de negócio
│   │   │   ├── repository/           # acesso ao banco
│   │   │   ├── model/                # entidades JPA
│   │   │   ├── dto/                  # objetos de entrada e saída
│   │   │   ├── security/             # login, sessão, CSRF, Google
│   │   │   ├── exception/            # tratamento de erros
│   │   │   └── config/
│   │   └── resources/
│   │       ├── db/migration/         # scripts do Flyway
│   │       ├── static/               # páginas, CSS e JavaScript
│   │       └── application.properties
│   └── test/                         # testes automatizados
└── pom.xml
```

## 🔌 API

Todas as rotas, exceto as de `/api/auth` e as páginas, exigem login.

| Rota | Descrição |
|---|---|
| `POST /api/auth/cadastro` | Cria a conta e envia o e-mail de confirmação |
| `POST /api/auth/verificar-email` | Confirma o e-mail pelo link recebido |
| `POST /api/auth/login` | Entra com e-mail e senha |
| `GET /api/auth/sessao` | Dados de quem está logado |
| `POST /api/auth/esqueci-senha` / `redefinir-senha` | Recuperação de senha |
| `GET/POST/PUT/DELETE /api/transacoes` | Receitas e despesas (filtros: período, categoria, tipo) |
| `GET/POST/PUT/DELETE /api/categorias` | Categorias |
| `GET /api/dashboard` | Totais, saldo, evolução mensal e totais por categoria |

## 🚀 Como rodar

**Pré-requisitos:** JDK 21, Maven (ou o `mvnw` do projeto) e MySQL 8 em execução.

1. Clone o repositório e entre na pasta:
   ```bash
   git clone https://github.com/santosdev/controle-financeiro.git
   cd controle-financeiro
   ```
2. Informe a senha do MySQL por variável de ambiente (ela nunca fica no código).

   Windows (PowerShell):
   ```powershell
   $env:DB_PASSWORD = "sua_senha_do_mysql"
   ```
   Linux/macOS:
   ```bash
   export DB_PASSWORD="sua_senha_do_mysql"
   ```
3. Inicie o sistema:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Abra **http://localhost:8080**, crie uma conta e use o sistema.

O banco `financecontrol_auth` e as tabelas são criados automaticamente pelo Flyway na primeira execução.

> Sem servidor de e-mail configurado, o link de confirmação aparece **no terminal** onde o sistema está rodando. Isso é útil para desenvolver e testar.

### Variáveis de ambiente

| Variável | Para que serve | Obrigatória |
|---|---|---|
| `DB_PASSWORD` | Senha do MySQL | Sim |
| `DB_USER` / `DB_NAME` | Usuário e nome do banco (padrão: `root` e `financecontrol_auth`) | Não |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Envio real de e-mails | Não |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Ativa o botão "Continuar com o Google" | Não |
| `APP_BASE_URL` | Endereço público, usado nos links dos e-mails | Não |
| `COOKIE_SECURE` | `true` em produção (HTTPS) | Não |

O passo a passo para configurar o Gmail e o login com Google está em [docs/LOGIN.md](docs/LOGIN.md).

## ✅ Testes

Os testes unitários cobrem as regras de negócio e **não precisam de MySQL nem de servidor de e-mail**:

```bash
./mvnw test "-Dtest=UsuarioTest,TokenUsuarioTest,TokenServiceTest,AuthServiceTest,TransacaoServiceTest,DashboardServiceTest,ValidacaoDosFormulariosTest"
```

| Área | O que é verificado |
|---|---|
| Cadastro e e-mail | conta nova, e-mail repetido, reenvio, link expirado, já usado ou inválido |
| Login | e-mail não confirmado, bloqueio após 5 falhas, desbloqueio |
| Senha | redefinição, token de uso único, resposta que não revela se o e-mail existe |
| Google | criação de conta, conta já existente, nome ausente |
| Tokens | só o hash é guardado, valores diferentes a cada geração |
| Movimentações | categoria de outro tipo ou de outro usuário, acesso a dados alheios, filtros |
| Dashboard | totais e saldo, saldo negativo, meses sem movimento, limite de 24 meses |
| Formulários | e-mail, senha, valor, casas decimais e tamanhos de campo |

O teste `FinanceControlApplicationTests` sobe a aplicação inteira e, por isso, precisa do MySQL configurado (`DB_PASSWORD`).

## 🗺️ Etapas de desenvolvimento

1. Configuração do ambiente.
2. Criação do projeto Java.
3. Banco de dados MySQL.
4. Backend (API REST).
5. Interface web.
6. Integração entre frontend, backend e banco.
7. Dashboard.
8. Login, testes automatizados e documentação.

## 📚 Objetivo de aprendizagem

Este projeto faz parte do meu portfólio de desenvolvimento e tem como objetivo aprimorar conhecimentos em Java, Spring Boot, segurança de aplicações web, integração com banco de dados e testes automatizados.

## 👨‍💻 Desenvolvido por SantosDev

**Transformando ideias em soluções digitais.**
