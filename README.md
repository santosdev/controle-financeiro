# FinanceControl

Controle financeiro pessoal na web, feito com Java 21, Spring Boot e PostgreSQL. Cada usuário tem a própria conta, lança receitas e despesas por categoria e acompanha o saldo e a evolução mês a mês no dashboard.

**Site:** https://financecontrol-04ly.onrender.com
Para conhecer sem criar conta, use **Entrar como visitante** na tela de login.

O servidor gratuito desliga depois de alguns minutos sem uso, então o primeiro acesso pode levar cerca de um minuto.

Há também um vídeo de demonstração em [docs/FinanceControl.mp4](docs/FinanceControl.mp4).

## Funcionalidades

### Finanças

- Receitas e despesas com descrição, valor, data e categoria.
- Categorias por usuário. Toda conta nova começa com 11 categorias, que podem ser editadas ou apagadas.
- Filtros por período, categoria e tipo.
- Dashboard com saldo, totais, evolução mensal e divisão por categoria. Os gráficos são desenhados sem biblioteca externa.
- Tema claro e escuro, e layout adaptado para celular.

### Conta

- Cadastro com confirmação por e-mail.
- Login com e-mail e senha. O "Continuar com o Google" é opcional e só aparece se estiver configurado.
- Recuperação de senha por link de uso único, válido por 1 hora.
- Perfil com apelido, foto, troca de senha e exclusão da conta. A exclusão apaga também todos os dados do usuário.
- Acesso como visitante: cria uma conta temporária, separada para cada pessoa, sem e-mail e sem senha. Ela é apagada quando a pessoa sai ou, no máximo, 24 horas depois. O acesso tem limite de 10 contas por IP por hora e de 300 visitantes ao mesmo tempo.

### Segurança

- Senhas guardadas com BCrypt. Os tokens enviados por e-mail ficam no banco só como hash SHA-256.
- Login bloqueado por 15 minutos após 5 senhas erradas.
- Sessão em cookie `HttpOnly`, proteção CSRF e cabeçalhos de segurança, incluindo Content-Security-Policy.
- Cada usuário só acessa os próprios dados. Pedir um registro de outra pessoa pelo ID retorna 404.
- A foto de perfil é validada e refeita no servidor: aceita só JPG, PNG ou GIF de verdade, de até 2 MB, e salva sempre como JPEG 256x256.

## Tecnologias

| | |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security (com OAuth2 Client), Spring Data JPA / Hibernate |
| Banco | PostgreSQL, com migrations do Flyway |
| E-mail | API do Resend ou SMTP (Spring Mail) |
| Frontend | HTML, CSS e JavaScript, sem framework |
| Testes | JUnit 5, Mockito e AssertJ |
| Build e deploy | Maven, Docker, Render (aplicação), Neon (banco) |

## Estrutura

```text
controle-financeiro/
├── docs/
│   ├── DEPLOY.md               # publicação no Render + Neon + Resend
│   ├── LOGIN.md                # configuração de e-mail e do login com Google
│   └── FinanceControl.mp4      # vídeo de demonstração
├── src/main/java/com/santosdev/financecontrol/
│   ├── controller/             # endpoints REST
│   ├── service/                # regras de negócio
│   ├── repository/             # consultas ao banco
│   ├── model/                  # entidades JPA
│   ├── dto/                    # entrada e saída da API
│   ├── security/               # login, sessão, CSRF e Google
│   ├── exception/              # respostas de erro
│   └── config/
├── src/main/resources/
│   ├── db/migration/           # scripts do Flyway
│   ├── static/                 # páginas, CSS e JavaScript
│   └── application.properties
├── src/test/                   # testes automatizados
├── Dockerfile
└── pom.xml
```

## API

As rotas de `/api/auth` são públicas. As demais rotas de `/api` exigem login.

| Rota | Descrição |
|---|---|
| `POST /api/auth/cadastro` | Cria a conta e envia o e-mail de confirmação |
| `POST /api/auth/verificar-email` | Confirma o e-mail |
| `POST /api/auth/login` | Login com e-mail e senha |
| `POST /api/auth/visitante` | Cria uma conta de visitante e já entra com ela |
| `POST /api/auth/logout` | Sai (a conta de visitante é apagada) |
| `GET /api/auth/sessao` | Usuário logado |
| `POST /api/auth/esqueci-senha` e `/redefinir-senha` | Recuperação de senha |
| `GET/POST/PUT/DELETE /api/transacoes` | Receitas e despesas, com filtros `inicio`, `fim`, `categoriaId` e `tipo` |
| `GET/POST/PUT/DELETE /api/categorias` | Categorias |
| `GET /api/dashboard` | Totais, saldo, evolução mensal e totais por categoria |
| `GET/PUT /api/perfil` | Nome e apelido |
| `GET/PUT/DELETE /api/perfil/foto` | Foto de perfil |
| `PUT /api/perfil/senha` | Troca de senha (pede a senha atual) |
| `POST /api/perfil/excluir-conta` | Exclui a conta e os dados dela |

## Como rodar

Você vai precisar de JDK 21 e de um PostgreSQL 14 ou mais novo rodando. O Maven não precisa ser instalado: o projeto traz o `mvnw`.

1. Crie o banco:
   ```bash
   createdb financecontrol
   ```
   As tabelas são criadas pelo Flyway quando o sistema sobe pela primeira vez.

2. Clone o projeto:
   ```bash
   git clone https://github.com/santosdev/controle-financeiro.git
   cd controle-financeiro
   ```

3. Defina a senha do PostgreSQL:
   ```bash
   export DB_PASSWORD="sua_senha"          # Linux/macOS
   ```
   ```powershell
   $env:DB_PASSWORD = "sua_senha"          # Windows (PowerShell)
   ```

4. Suba o sistema e abra http://localhost:8080:
   ```bash
   ./mvnw spring-boot:run
   ```

Se nenhum serviço de e-mail estiver configurado, o link de confirmação de cadastro sai no terminal, e não por e-mail.

### Variáveis de ambiente

Só a `DB_PASSWORD` é obrigatória.

| Variável | Uso |
|---|---|
| `DB_PASSWORD` | Senha do PostgreSQL |
| `DB_USER`, `DB_NAME` | Usuário e banco (padrão: `postgres` e `financecontrol`) |
| `DB_URL` | URL JDBC completa, usada em produção. Substitui `DB_NAME` |
| `PORT` | Porta HTTP (padrão `8080`) |
| `APP_BASE_URL` | Endereço público, usado nos links dos e-mails |
| `COOKIE_SECURE` | `true` quando o site roda em HTTPS |
| `RESEND_API_KEY` | Envia e-mails pela API do Resend. Tem prioridade sobre o SMTP |
| `MAIL_FROM` | Remetente dos e-mails |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Envio por SMTP (ex.: Gmail) |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Ativam o login com Google |
| `VISITANTE_HABILITADO` | `false` desliga o acesso como visitante (padrão `true`) |

A configuração do Gmail e do Google está em [docs/LOGIN.md](docs/LOGIN.md). A publicação está em [docs/DEPLOY.md](docs/DEPLOY.md).

## Testes

São 140 testes unitários, que rodam sem banco e sem servidor de e-mail:

```bash
./mvnw test "-Dtest=!FinanceControlApplicationTests"
```

O `FinanceControlApplicationTests` sobe a aplicação inteira e precisa de um PostgreSQL configurado.

| Área | O que é verificado |
|---|---|
| Cadastro e e-mail | conta nova, e-mail repetido, reenvio, link expirado, já usado ou inválido |
| Login | e-mail não confirmado, bloqueio após 5 falhas e desbloqueio |
| Senha | redefinição, token de uso único, resposta igual exista o e-mail ou não |
| Google | criação de conta, conta já existente, nome ausente |
| Visitante | conta sem senha, uma conta por pessoa, limites, limpeza após 24 horas, exclusão confirmada com EXCLUIR |
| Movimentações | categoria de outro tipo ou de outro usuário, acesso a dados alheios, filtros |
| Dashboard | totais, saldo negativo, meses sem movimento, limite de 24 meses |
| Perfil e foto | apelido, troca e bloqueio de senha, ordem da exclusão de conta, validação e recorte da imagem |
| E-mail | chamada à API do Resend e prioridade sobre o SMTP |
| Formulários | e-mail, senha, valor, casas decimais e tamanho dos campos |

## Autor

Desenvolvido por [SantosDev](https://github.com/santosdev).
