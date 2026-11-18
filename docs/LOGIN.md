# Login, e-mail e Google: como configurar

O FinanceControl tem contas de usuário: cada pessoa vê **só os próprios dados**. O login funciona com
e-mail e senha (com confirmação do e-mail e "esqueci minha senha") e, opcionalmente, com o Google.

Tudo é configurado por **variáveis de ambiente**. Nenhuma senha ou chave fica no código.

| Variável | Para que serve | Obrigatória? |
|---|---|---|
| `DB_PASSWORD` | Senha do usuário `root` do MySQL | Sim |
| `DB_NAME` | Nome do banco (padrão: `financecontrol_auth`) | Não |
| `APP_BASE_URL` | Endereço do sistema, usado nos links dos e-mails (padrão: `http://localhost:8080`) | Não |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Servidor SMTP para enviar e-mails de verdade | Não (sem elas, os e-mails só aparecem no log) |
| `MAIL_FROM` | Remetente exibido nos e-mails | Não |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Ativam o botão "Continuar com o Google" | Não (sem elas, o botão não aparece) |
| `COOKIE_SECURE` | `true` quando o sistema estiver em HTTPS | Não |

## Sem configurar nada (modo desenvolvimento)

Funciona com apenas `DB_PASSWORD`. Quando o sistema "envia" um e-mail, ele imprime o link no terminal:

```
[E-MAIL SIMULADO - SMTP não configurado] Para: fulano@email.com | Assunto: ... | Link: http://localhost:8080/verificar-email.html?token=...
```

Copie o link do terminal e cole no navegador para confirmar a conta ou redefinir a senha.

## Enviar e-mails de verdade pelo Gmail

1. Na conta Google que vai **enviar** os e-mails, ative a **verificação em duas etapas**
   (https://myaccount.google.com/security).
2. Crie uma **senha de app**: https://myaccount.google.com/apppasswords
   (dê um nome, por exemplo "FinanceControl", e copie a senha de 16 letras).
3. Defina as variáveis (PowerShell):

   ```powershell
   [Environment]::SetEnvironmentVariable("SPRING_MAIL_HOST", "smtp.gmail.com", "User")
   [Environment]::SetEnvironmentVariable("SPRING_MAIL_PORT", "587", "User")
   [Environment]::SetEnvironmentVariable("SPRING_MAIL_USERNAME", "seu.email@gmail.com", "User")
   [Environment]::SetEnvironmentVariable("SPRING_MAIL_PASSWORD", "SENHA_DE_APP_DE_16_LETRAS", "User")
   ```

4. Feche e abra o VS Code e rode o sistema de novo. Use a **senha de app**, nunca a senha normal da conta.

> O Gmail limita o envio (cerca de 500 e-mails por dia). Para algo maior que um portfólio, use um serviço de
> e-mail transacional (Brevo, Resend, SendGrid etc.): basta trocar as variáveis `SPRING_MAIL_*`.

## Ativar "Continuar com o Google"

1. Acesse https://console.cloud.google.com e crie um projeto (ou escolha um).
2. Em **APIs e serviços > Tela de consentimento OAuth**, configure o app (tipo **Externo**), informe nome,
   e-mail de suporte e adicione os escopos `openid`, `email` e `profile`. Em modo de teste, adicione o seu
   e-mail em **Usuários de teste**.
3. Em **APIs e serviços > Credenciais > Criar credenciais > ID do cliente OAuth**, escolha **Aplicativo da Web** e
   cadastre em **URIs de redirecionamento autorizados**:

   ```
   http://localhost:8080/login/oauth2/code/google
   ```

   (Em produção, use o endereço real do sistema no lugar de `http://localhost:8080`.)
4. Copie o **ID do cliente** e a **chave secreta** e defina:

   ```powershell
   [Environment]::SetEnvironmentVariable("GOOGLE_CLIENT_ID", "SEU_ID.apps.googleusercontent.com", "User")
   [Environment]::SetEnvironmentVariable("GOOGLE_CLIENT_SECRET", "SUA_CHAVE_SECRETA", "User")
   ```

5. Reinicie o VS Code e o sistema. O botão **Continuar com o Google** aparece nas telas de entrar e de criar conta.

Quem entra pelo Google tem o e-mail já confirmado pelo próprio Google. Se a pessoa já tinha uma conta com o mesmo
e-mail, as duas passam a ser a mesma conta.

## O que foi feito para proteger as contas

- **Senhas** guardadas só como hash **BCrypt** (custo 12); regra: 8 a 64 caracteres, com letra e número.
- **Confirmação de e-mail** obrigatória antes do primeiro login (link válido por 24 horas).
- **Esqueci minha senha**: link de uso único, válido por 1 hora. A resposta é sempre a mesma exista o e-mail ou
  não, e há intervalo de 1 minuto entre e-mails. Os tokens ficam no banco apenas como hash (SHA-256).
- **Bloqueio por tentativas**: 5 senhas erradas bloqueiam o login por 15 minutos; redefinir a senha libera.
- **Mensagem de login genérica** ("e-mail ou senha inválidos"), sem revelar se o e-mail existe.
- **Sessão** em cookie `HttpOnly` + `SameSite=Lax` (`Secure` em HTTPS), com troca do id da sessão no login
  (contra fixação de sessão) e expiração em 2 horas.
- **CSRF**: toda requisição que altera dados exige o token do cookie `XSRF-TOKEN` no cabeçalho `X-XSRF-TOKEN`.
- **Isolamento**: todas as consultas filtram pelo dono. Acessar o ID de outra pessoa retorna 404.
- **Cabeçalhos de segurança**: Content-Security-Policy restritiva (só scripts do próprio sistema), `X-Frame-Options`,
  `X-Content-Type-Options`, `Referrer-Policy` e `Permissions-Policy`.
- **Banco versionado** com Flyway (`src/main/resources/db/migration`).

## Endpoints

| Método | Caminho | O que faz |
|---|---|---|
| GET | `/api/auth/config` | Informa se o login com Google está ativo |
| GET | `/api/auth/sessao` | Quem está logado (ou `autenticado: false`) |
| POST | `/api/auth/cadastro` | Cria a conta e envia o e-mail de confirmação |
| POST | `/api/auth/verificar-email` | Confirma o e-mail com o token do link |
| POST | `/api/auth/reenviar-confirmacao` | Reenvia o e-mail de confirmação |
| POST | `/api/auth/login` | Entra com e-mail e senha |
| POST | `/api/auth/logout` | Sai |
| POST | `/api/auth/esqueci-senha` | Envia o link de redefinição |
| POST | `/api/auth/redefinir-senha` | Troca a senha com o token do link |

Todo o resto de `/api/**` exige login.

## Para produção (checklist)

- Usar **HTTPS** e definir `COOKIE_SECURE=true` e `APP_BASE_URL` com o endereço real.
- Trocar o Gmail por um serviço de e-mail transacional.
- Cadastrar o endereço real no redirecionamento do Google e publicar a tela de consentimento.
- Colocar um limite de requisições (rate limit) no proxy ou no provedor, além do bloqueio por conta.
