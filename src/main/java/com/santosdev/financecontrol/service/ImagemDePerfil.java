package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.exception.ApiException;
import org.springframework.http.HttpStatus;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Set;

/**
 * Valida e padroniza a foto de perfil: so aceita imagem de verdade (JPEG, PNG ou GIF), recorta o centro
 * em quadrado e devolve sempre um JPEG 256x256. Reescrever a imagem tambem descarta qualquer dado extra
 * escondido no arquivo enviado.
 */
public final class ImagemDePerfil {

    public static final int TAMANHO_MAXIMO_BYTES = 2 * 1024 * 1024;
    public static final int LADO = 256;
    /** Protege contra imagens minusculas em bytes que explodem em memoria ao abrir. */
    private static final long MAXIMO_PIXELS = 25_000_000L;
    private static final Set<String> FORMATOS = Set.of("jpeg", "png", "gif");

    private ImagemDePerfil() {
    }

    public static byte[] processar(byte[] original) {
        if (original == null || original.length == 0) {
            throw invalida("Nenhuma imagem foi enviada.");
        }
        if (original.length > TAMANHO_MAXIMO_BYTES) {
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE, "FOTO_GRANDE", "A foto deve ter no máximo 2 MB.");
        }
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (entrada == null || !leitores.hasNext()) {
                throw invalida("O arquivo não é uma imagem válida. Use JPG, PNG ou GIF.");
            }
            ImageReader leitor = leitores.next();
            try {
                if (!FORMATOS.contains(leitor.getFormatName().toLowerCase())) {
                    throw invalida("Formato de imagem não aceito. Use JPG, PNG ou GIF.");
                }
                leitor.setInput(entrada, true, true);
                long largura = leitor.getWidth(0);
                long altura = leitor.getHeight(0);
                if (largura <= 0 || altura <= 0 || largura * altura > MAXIMO_PIXELS) {
                    throw invalida("A imagem tem dimensões inválidas ou grandes demais.");
                }
                return quadrado(leitor.read(0));
            } finally {
                leitor.dispose();
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw invalida("Não foi possível ler a imagem. Tente outro arquivo.");
        }
    }

    private static byte[] quadrado(BufferedImage origem) throws java.io.IOException {
        int lado = Math.min(origem.getWidth(), origem.getHeight());
        int x = (origem.getWidth() - lado) / 2;
        int y = (origem.getHeight() - lado) / 2;

        BufferedImage destino = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setColor(Color.WHITE); // JPEG nao tem transparencia
            g.fillRect(0, 0, LADO, LADO);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(origem, 0, 0, LADO, LADO, x, y, x + lado, y + lado, null);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        if (!ImageIO.write(destino, "jpg", saida)) {
            throw new java.io.IOException("Sem codificador JPEG");
        }
        return saida.toByteArray();
    }

    private static ApiException invalida(String mensagem) {
        return new ApiException(HttpStatus.BAD_REQUEST, "FOTO_INVALIDA", mensagem);
    }
}
