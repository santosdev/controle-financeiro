package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagemDePerfilTest {

    private static byte[] imagem(int largura, int altura, String formato, int tipo) throws IOException {
        BufferedImage img = new BufferedImage(largura, altura, tipo);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, largura, altura);
        g.dispose();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(img, formato, saida);
        return saida.toByteArray();
    }

    private static BufferedImage ler(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private static void assertFotoInvalida(byte[] bytes, String codigo) {
        assertThatThrownBy(() -> ImagemDePerfil.processar(bytes))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }

    @Test
    void imagemLargaViraQuadradoJpegDe256() throws IOException {
        byte[] resultado = ImagemDePerfil.processar(imagem(800, 300, "png", BufferedImage.TYPE_INT_RGB));

        BufferedImage lida = ler(resultado);
        assertThat(lida.getWidth()).isEqualTo(ImagemDePerfil.LADO);
        assertThat(lida.getHeight()).isEqualTo(ImagemDePerfil.LADO);
        // JPEG sempre comeca com FF D8
        assertThat(resultado[0] & 0xFF).isEqualTo(0xFF);
        assertThat(resultado[1] & 0xFF).isEqualTo(0xD8);
    }

    @Test
    void imagemPequenaTambemFicaEm256() throws IOException {
        BufferedImage lida = ler(ImagemDePerfil.processar(imagem(40, 40, "png", BufferedImage.TYPE_INT_RGB)));

        assertThat(lida.getWidth()).isEqualTo(256);
    }

    @Test
    void pngComTransparenciaGanhaFundoBranco() throws IOException {
        BufferedImage transparente = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB); // todo transparente
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(transparente, "png", saida);

        BufferedImage lida = ler(ImagemDePerfil.processar(saida.toByteArray()));

        int pixel = lida.getRGB(128, 128);
        assertThat((pixel >> 16) & 0xFF).isGreaterThan(240);
        assertThat((pixel >> 8) & 0xFF).isGreaterThan(240);
        assertThat(pixel & 0xFF).isGreaterThan(240);
    }

    @Test
    void aceitaJpegEGif() throws IOException {
        assertThat(ImagemDePerfil.processar(imagem(120, 90, "jpg", BufferedImage.TYPE_INT_RGB))).isNotEmpty();
        assertThat(ImagemDePerfil.processar(imagem(120, 90, "gif", BufferedImage.TYPE_INT_RGB))).isNotEmpty();
    }

    @Test
    void recusaArquivoQueNaoEImagem() {
        assertFotoInvalida("isto não é uma imagem".getBytes(StandardCharsets.UTF_8), "FOTO_INVALIDA");
        assertFotoInvalida("<svg xmlns='http://www.w3.org/2000/svg'><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8), "FOTO_INVALIDA");
    }

    @Test
    void recusaFormatoDeImagemNaoPermitido() throws IOException {
        assertFotoInvalida(imagem(50, 50, "bmp", BufferedImage.TYPE_INT_RGB), "FOTO_INVALIDA");
    }

    @Test
    void recusaVazioENulo() {
        assertFotoInvalida(new byte[0], "FOTO_INVALIDA");
        assertFotoInvalida(null, "FOTO_INVALIDA");
    }

    @Test
    void recusaArquivoMaiorQue2Mb() {
        assertThatThrownBy(() -> ImagemDePerfil.processar(new byte[ImagemDePerfil.TAMANHO_MAXIMO_BYTES + 1]))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo("FOTO_GRANDE");
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
                });
    }

    @Test
    void recusaImagemTruncada() throws IOException {
        byte[] completa = imagem(300, 300, "png", BufferedImage.TYPE_INT_RGB);
        byte[] cortada = java.util.Arrays.copyOf(completa, completa.length / 3);

        assertFotoInvalida(cortada, "FOTO_INVALIDA");
    }
}
