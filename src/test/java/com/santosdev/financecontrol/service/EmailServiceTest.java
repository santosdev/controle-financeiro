package com.santosdev.financecontrol.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Envio pelo Resend, usando um servidor HTTP local no lugar da API real. */
class EmailServiceTest {

    private HttpServer servidor;
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private final AtomicReference<String> autorizacaoRecebida = new AtomicReference<>();
    private String url;

    @BeforeEach
    void subirServidorFalso() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/emails", troca -> {
            corpoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            autorizacaoRecebida.set(troca.getRequestHeaders().getFirst("Authorization"));
            byte[] resposta = "{\"id\":\"123\"}".getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(200, resposta.length);
            troca.getResponseBody().write(resposta);
            troca.close();
        });
        servidor.start();
        url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/emails";
    }

    @AfterEach
    void derrubarServidor() {
        servidor.stop(0);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> smtp(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return provider;
    }

    @Test
    void comChaveDoResendEnviaPelaApiEIgnoraOSmtp() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService email = new EmailService(smtp(sender), "FinanceControl <no-reply@financecontrol.local>",
                "https://app.exemplo.com/", "re_chave", url);

        email.enviarConfirmacaoDeConta("Ana \"Teste\" Silva", "ana@exemplo.com", "tok123");

        assertThat(autorizacaoRecebida.get()).isEqualTo("Bearer re_chave");
        assertThat(corpoRecebido.get())
                .startsWith("{\"from\":\"FinanceControl <onboarding@resend.dev>\",\"to\":[\"ana@exemplo.com\"]")
                .contains("\"subject\":\"Confirme seu e-mail no FinanceControl\"")
                .contains("https://app.exemplo.com/verificar-email.html?token=tok123")
                .contains("Olá, Ana!")
                .doesNotContain("\n");
        verifyNoInteractions(sender);
    }

    @Test
    void remetenteProprioEMantidoComResend() {
        EmailService email = new EmailService(smtp(null), "Fabio <oi@meudominio.com>",
                "https://app.exemplo.com", "re_chave", url);

        email.enviarRedefinicaoDeSenha("Ana", "ana@exemplo.com", "tok");

        assertThat(corpoRecebido.get()).startsWith("{\"from\":\"Fabio <oi@meudominio.com>\"");
    }

    @Test
    void semChaveNaoChamaOResend() {
        EmailService email = new EmailService(smtp(null), "FinanceControl <no-reply@financecontrol.local>",
                "https://app.exemplo.com", "", url);

        email.enviarConfirmacaoDeConta("Ana", "ana@exemplo.com", "tok");

        assertThat(corpoRecebido.get()).isNull();
    }

    @Test
    void escapaCaracteresEspeciaisNoJson() {
        assertThat(EmailService.json("a\"b\\c\nd\te\u0001é"))
                .isEqualTo("\"a\\\"b\\\\c\\nd\\te\\u0001é\"");
    }
}
