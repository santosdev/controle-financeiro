package com.santosdev.financecontrol.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class FotoDoGoogleTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://lh3.googleusercontent.com/a/AEdFTp4=s96-c",
            "https://lh3.googleusercontent.com:443/a/foto",
            "https://googleusercontent.com/foto"})
    void aceitaEnderecosDoDominioDeImagensDoGoogle(String url) {
        assertThat(FotoDoGoogle.urlPermitida(url)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://lh3.googleusercontent.com/foto",
            "https://evil.com/foto",
            "https://googleusercontent.com.evil.com/foto",
            "https://evilgoogleusercontent.com/foto",
            "https://user:senha@lh3.googleusercontent.com/foto",
            "https://lh3.googleusercontent.com:8443/foto",
            "https://localhost/foto",
            "file:///etc/passwd",
            "nao e uma url",
            ""})
    void recusaQualquerOutroEndereco(String url) {
        assertThat(FotoDoGoogle.urlPermitida(url)).isFalse();
    }

    @Test
    void recusaEnderecoNulo() {
        assertThat(FotoDoGoogle.urlPermitida(null)).isFalse();
    }
}
