package com.santosdev.financecontrol.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * Baixa a foto da conta Google de quem acabou de criar a conta pelo botao "Continuar com o Google".
 * A imagem e copiada para o nosso banco (a politica de seguranca do sistema nao carrega imagens de outros sites).
 * Qualquer falha e ignorada: sem foto, o sistema mostra a inicial do nome.
 */
@Component
public class FotoDoGoogle {

    private static final Logger log = LoggerFactory.getLogger(FotoDoGoogle.class);

    private final PerfilService perfil;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public FotoDoGoogle(PerfilService perfil) {
        this.perfil = perfil;
    }

    /** So baixa de enderecos https do dominio de imagens do Google, para esta chamada nunca ir a outro lugar. */
    static boolean urlPermitida(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            return "https".equals(uri.getScheme())
                    && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && (host.equals("googleusercontent.com") || host.endsWith(".googleusercontent.com"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void importar(Long usuarioId, String urlDaFoto) {
        if (!urlPermitida(urlDaFoto)) {
            return;
        }
        try {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create(urlDaFoto.trim()))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<java.io.InputStream> resposta = cliente.send(pedido, HttpResponse.BodyHandlers.ofInputStream());
            try (java.io.InputStream corpo = resposta.body()) {
                if (resposta.statusCode() != 200) {
                    return;
                }
                byte[] bytes = corpo.readNBytes(ImagemDePerfil.TAMANHO_MAXIMO_BYTES + 1);
                perfil.definirFotoSeNaoTiver(usuarioId, bytes);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Não foi possível importar a foto do Google: {}", e.getMessage());
        }
    }
}
