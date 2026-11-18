# 🚀 Deploy (Render + Neon)

Guia para publicar o FinanceControl na internet usando apenas planos gratuitos:

| Peça | Serviço | Por quê |
|---|---|---|
| Aplicação (Docker) | [Render](https://render.com) | Faz o build pelo `Dockerfile` do repositório e entrega HTTPS |
| Banco PostgreSQL | [Neon](https://neon.tech) | O plano gratuito não expira (o PostgreSQL gratuito do Render é apagado após 30 dias) |
| E-mails | [Resend](https://resend.com) | API HTTPS: o Render gratuito bloqueia SMTP, então o Gmail não funciona lá |

## 1. Banco no Neon

1. Crie um projeto (de preferência na região mais próxima dos usuários).
2. Em **Connect**, copie a string de conexão, que tem este formato:
   `postgresql://USUARIO:SENHA@HOST/BANCO?sslmode=require`
3. Separe em três variáveis:

| Variável | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://HOST/BANCO?sslmode=require` |
| `DB_USER` | `USUARIO` |
| `DB_PASSWORD` | `SENHA` |

As tabelas são criadas pelo Flyway na primeira vez que a aplicação sobe.

## 2. E-mail no Resend

1. Crie a conta e gere uma **API key**. Ela vai na variável `RESEND_API_KEY`.
2. Sem domínio próprio, o remetente é `onboarding@resend.dev`, e o Resend **só entrega para o e-mail da sua conta**. Isso basta para uma demonstração.
3. Para enviar para qualquer pessoa, verifique um domínio no Resend e defina `MAIL_FROM` (ex.: `FinanceControl <nao-responda@seudominio.com>`).

Ordem de prioridade do envio: Resend (`RESEND_API_KEY`) → SMTP (`SPRING_MAIL_*`) → só log.

## 3. Aplicação no Render

1. **New → Web Service**, conecte o repositório e escolha a branch `main`.
2. Runtime **Docker** (detectado pelo `Dockerfile`) e plano **Free**.
3. Em **Environment**, adicione:

| Variável | Valor |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | do passo 1 |
| `RESEND_API_KEY` | do passo 2 |
| `COOKIE_SECURE` | `true` |
| `APP_BASE_URL` | endereço do serviço, ex.: `https://financecontrol.onrender.com` |

`PORT` é definida pelo próprio Render. Cada push na `main` gera um novo deploy automaticamente.

## 4. Login com Google (opcional)

No Google Cloud, adicione o endereço público como URI de redirecionamento:
`https://SEU-ENDERECO/login/oauth2/code/google`. Depois defina `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET` no Render. Detalhes em [LOGIN.md](LOGIN.md).

## Limitações do plano gratuito

- O serviço "dorme" após cerca de 15 minutos sem acesso; o primeiro acesso depois disso leva de 30 a 60 segundos.
- As sessões ficam na memória da aplicação: quando ela reinicia ou acorda, é preciso entrar de novo.
- 512 MB de memória: o `Dockerfile` já limita a JVM para caber nesse espaço.
