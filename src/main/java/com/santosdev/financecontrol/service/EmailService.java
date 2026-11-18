package com.santosdev.financecontrol.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Envia os e-mails do sistema, nesta ordem de preferencia:
 * 1. Resend (RESEND_API_KEY): API HTTPS, funciona em hospedagens que bloqueiam SMTP (ex.: Render gratuito);
 * 2. SMTP (SPRING_MAIL_HOST), por exemplo o Gmail;
 * 3. nenhum: o conteudo e o link sao apenas impressos no log, o que basta para desenvolver e testar.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remetente;
    private final String baseUrl;
    private final String resendApiKey;
    private final String resendUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    /** Remetente de testes do Resend: funciona sem dominio proprio, mas so entrega para o e-mail da conta Resend. */
    static final String REMETENTE_TESTE_RESEND = "FinanceControl <onboarding@resend.dev>";

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${app.mail.from}") String remetente,
                        @Value("${app.base-url}") String baseUrl,
                        @Value("${app.mail.resend-api-key:}") String resendApiKey,
                        @Value("${app.mail.resend-url:https://api.resend.com/emails}") String resendUrl) {
        this.mailSender = mailSender;
        this.resendUrl = resendUrl;
        this.resendApiKey = resendApiKey == null ? "" : resendApiKey.trim();
        boolean remetentePadrao = remetente.contains("@financecontrol.local");
        this.remetente = !this.resendApiKey.isEmpty() && remetentePadrao ? REMETENTE_TESTE_RESEND : remetente;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Async
    public void enviarConfirmacaoDeConta(String nome, String para, String token) {
        String link = baseUrl + "/verificar-email.html?token=" + token;
        enviar(para, "Confirme seu e-mail no FinanceControl",
                modelo(nome, "Confirme seu e-mail",
                        "Falta só um passo para ativar sua conta no FinanceControl. Clique no botão abaixo para confirmar o seu e-mail.",
                        "Confirmar e-mail", link,
                        "O link vale por 24 horas. Se você não criou esta conta, pode ignorar este e-mail."),
                link);
    }

    @Async
    public void enviarRedefinicaoDeSenha(String nome, String para, String token) {
        String link = baseUrl + "/redefinir-senha.html?token=" + token;
        enviar(para, "Redefinição de senha do FinanceControl",
                modelo(nome, "Redefinir sua senha",
                        "Recebemos um pedido para redefinir a senha da sua conta. Clique no botão abaixo para escolher uma nova senha.",
                        "Redefinir senha", link,
                        "O link vale por 1 hora e só pode ser usado uma vez. Se você não fez este pedido, ignore este e-mail: sua senha continua a mesma."),
                link);
    }

    private void enviar(String para, String assunto, String html, String link) {
        if (!resendApiKey.isEmpty()) {
            enviarPeloResend(para, assunto, html, link);
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("[E-MAIL SIMULADO - SMTP não configurado] Para: {} | Assunto: {} | Link: {}", para, assunto, link);
            return;
        }
        try {
            MimeMessage mensagem = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(para);
            helper.setSubject(assunto);
            helper.setText("Abra este link: " + link, html);
            sender.send(mensagem);
            log.info("E-mail '{}' enviado para {}", assunto, para);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail '{}' para {}: {}", assunto, para, e.getMessage());
        }
    }

    private void enviarPeloResend(String para, String assunto, String html, String link) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(resendUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpoResend(remetente, para, assunto, html, "Abra este link: " + link)))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 == 2) {
                log.info("E-mail '{}' enviado para {} (Resend)", assunto, para);
            } else {
                log.error("Resend recusou o e-mail '{}' para {}: HTTP {} {}", assunto, para, resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Falha ao enviar e-mail '{}' para {} pelo Resend: {}", assunto, para, e.getMessage());
        }
    }

    /** Monta o JSON da API do Resend. */
    static String corpoResend(String de, String para, String assunto, String html, String texto) {
        return "{" + List.of(
                        campo("from", de),
                        "\"to\":[" + json(para) + "]",
                        campo("subject", assunto),
                        campo("html", html),
                        campo("text", texto))
                .stream().collect(Collectors.joining(",")) + "}";
    }

    private static String campo(String nome, String valor) {
        return json(nome) + ":" + json(valor);
    }

    static String json(String valor) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private String modelo(String nome, String titulo, String texto, String botao, String link, String rodape) {
        String primeiroNome = HtmlUtils.htmlEscape(nome.trim().split("\\s+")[0]);
        return """
                <div style="font-family:Segoe UI,Arial,sans-serif;background:#f4f6fb;padding:32px 12px">
                  <div style="max-width:480px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px;color:#1f2937">
                    <div style="font-size:20px;font-weight:700;margin-bottom:24px">Finance<span style="color:#4f46e5">Control</span></div>
                    <h2 style="margin:0 0 12px;font-size:20px">%s</h2>
                    <p style="margin:0 0 8px">Olá, %s!</p>
                    <p style="margin:0 0 24px;line-height:1.5">%s</p>
                    <p style="margin:0 0 24px">
                      <a href="%s" style="background:#4f46e5;color:#ffffff;text-decoration:none;padding:12px 24px;border-radius:8px;font-weight:600;display:inline-block">%s</a>
                    </p>
                    <p style="margin:0 0 8px;font-size:13px;color:#6b7280;line-height:1.5">Se o botão não funcionar, copie e cole este endereço no navegador:<br>%s</p>
                    <p style="margin:16px 0 0;font-size:13px;color:#6b7280;line-height:1.5">%s</p>
                  </div>
                  <p style="text-align:center;font-size:12px;color:#9ca3af;margin-top:16px">FinanceControl · desenvolvido por SantosDev</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(titulo), primeiroNome, HtmlUtils.htmlEscape(texto),
                HtmlUtils.htmlEscape(link), HtmlUtils.htmlEscape(botao), HtmlUtils.htmlEscape(link), HtmlUtils.htmlEscape(rodape));
    }
}
