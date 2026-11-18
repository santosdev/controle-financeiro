# 💰 FinanceControl — Gestão Financeira Web

Sistema web de controle financeiro pessoal feito com **Java 21, Spring Boot e PostgreSQL**. Cada pessoa cria a sua conta, registra receitas e despesas, organiza por categorias e acompanha o saldo e a evolução mensal em um dashboard.

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
- **"Entrar como visitante"**: cria na hora uma conta temporária e individual (sem e-mail e sem senha), que começa igual a uma conta nova e é apagada ao sair ou em até 24 horas. Tem limite por IP e um teto de visitantes ao mesmo tempo.
- "Esqueci minha senha" com link de uso único, válido por 1 hora.
- **Perfil do usuário**: apelido, foto (enviada pela pessoa ou importada do Google na criação da conta), alteração de senha e exclusão da conta, que apaga também os dados dela.
- Bloqueio do login por 15 minutos após 5 senhas erradas.
- Senhas guardadas apenas como hash **BCrypt**; links de e-mail guardados apenas como hash.
- Sessão por cookie `HttpOnly`, proteção **CSRF** e cabeçalhos de segurança (incluindo Content-Security-Policy).
- Foto de perfil validada e reprocessada no servidor (só JPG, PNG ou GIF reais, no máximo 2 MB, sempre salva como JPEG 256x256).
- **Cada usuário enxerga somente os próprios dados**: acessar o ID de outra pessoa retorna "não encontrado".

## 🛠️ Tecnologias

| Tecnologia | Finalidade |
|---|---|
| Java 21 | Linguagem |
| Spring Boot 4 | Backend e API REST |
| Spring Security (+ OAuth2 Client) | Login, sessão, CSRF e Google |
| Spring Data JPA / Hibernate | Persistência |
| PostgreSQL | Banco de dados |
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
│   │   │   ├── service/              # regras de negócio (inclui o perfil)
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
| `POST /api/auth/visitante` | Cria uma conta temporária de visitante e já entra com ela |
| `GET /api/auth/sessao` | Dados de quem está logado |
| `POST /api/auth/esqueci-senha` / `redefinir-senha` | Recuperação de senha |
| `GET/POST/PUT/DELETE /api/transacoes` | Receitas e despesas (filtros: período, categoria, tipo) |
| `GET/POST/PUT/DELETE /api/categorias` | Categorias |
| `GET/PUT /api/perfil` | Dados do perfil (nome e apelido) |
| `GET/PUT/DELETE /api/perfil/foto` | Foto de perfil |
| `PUT /api/perfil/senha` | Alterar senha (exige a senha atual) |
| `POST /api/perfil/excluir-conta` | Exclui a conta e todos os dados dela |
| `GET /api/dashboard` | Totais, saldo, evolução mensal e totais por categoria |

## 🚀 Como rodar

**Pré-requisitos:** JDK 21, Maven (ou o `mvnw` do projeto) e PostgreSQL 14+ em execução (crie o banco com `createdb financecontrol`).

1. Clone o repositório e entre na pasta:
   ```bash
   git clone https://github.com/santosdev/controle-financeiro.git
   cd controle-financeiro
   ```
2. Informe a senha do PostgreSQL por variável de ambiente (ela nunca fica no código).

   Windows (PowerShell):
   ```powershell
   $env:DB_PASSWORD = "sua_senha_do_postgres"
   ```
   Linux/macOS:
   ```bash
   export DB_PASSWORD="sua_senha_do_postgres"
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
| `DB_PASSWORD` | Senha do PostgreSQL | Sim |
| `DB_USER` / `DB_NAME` | Usuário e nome do banco (padrão: `postgres` e `financecontrol`) | Não |
| `DB_URL` | URL JDBC completa do banco (produção). Quando definida, ignora `DB_NAME` | Não |
| `PORT` | Porta do servidor (a hospedagem define; padrão `8080`) | Não |
| `RESEND_API_KEY` | Envio de e-mails pela API do Resend (tem prioridade sobre o SMTP; ideal em hospedagens que bloqueiam SMTP) | Não |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Envio real de e-mails por SMTP | Não |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Ativa o botão "Continuar com o Google" | Não |
| `APP_BASE_URL` | Endereço público, usado nos links dos e-mails | Não |
| `COOKIE_SECURE` | `true` em produção (HTTPS) | Não |
| `VISITANTE_HABILITADO` | `false` esconde e desliga o "Entrar como visitante" (padrão: `true`) | Não |

O passo a passo para configurar o Gmail e o login com Google está em [docs/LOGIN.md](docs/LOGIN.md). Para publicar na internet (Render + Neon + Resend), veja [docs/DEPLOY.md](docs/DEPLOY.md).

## ✅ Testes

Os testes unitários cobrem as regras de negócio e **não precisam de PostgreSQL nem de servidor de e-mail**:

```bash
./mvnw test "-Dtest=UsuarioTest,TokenUsuarioTest,TokenServiceTest,AuthServiceTest,TransacaoServiceTest,DashboardServiceTest,ValidacaoDosFormulariosTest,ImagemDePerfilTest,PerfilServiceTest,FotoDoGoogleTest,EmailServiceTest,VisitanteServiceTest"
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
| Perfil | apelido, troca de senha (e bloqueio ao errar a atual), exclusão de conta na ordem certa, foto do Google |
| Foto | só imagem real, recorte quadrado 256x256, fundo branco em PNG transparente, limite de 2 MB, SVG e arquivos falsos recusados |
| Visitante | conta temporária sem senha, uma conta por visitante, limite por IP, teto de visitantes, limpeza após 24 horas, sem e-mail de redefinição, exclusão confirmada com EXCLUIR |
| E-mail | envio pela API do Resend (cabeçalho, remetente e JSON escapado), SMTP ignorado quando há chave do Resend |
| Formulários | e-mail, senha, valor, casas decimais e tamanhos de campo |

O teste `FinanceControlApplicationTests` sobe a aplicação inteira e, por isso, precisa do PostgreSQL configurado (`DB_PASSWORD`).

## 🗺️ Etapas de desenvolvimento

1. Configuração do ambiente.
2. Criação do projeto Java.
3. Banco de dados PostgreSQL.
4. Backend (API REST).
5. Interface web.
6. Integração entre frontend, backend e banco.
7. Dashboard.
8. Login, testes automatizados e documentação.

## 📚 Objetivo de aprendizagem

Este projeto faz parte do meu portfólio de desenvolvimento e tem como objetivo aprimorar conhecimentos em Java, Spring Boot, segurança de aplicações web, integração com banco de dados e testes automatizados.

## 👨‍💻 Desenvolvido por SantosDev

**Transformando ideias em soluções digitais.**
