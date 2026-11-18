package com.santosdev.financecontrol.dto;

import com.santosdev.financecontrol.model.TipoTransacao;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ValidacaoDosFormulariosTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void criarValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        fabrica.close();
    }

    /* ---------------- cadastro ---------------- */

    @Test
    void cadastroValidoPassa() {
        assertThat(validador.validate(new RegistroRequest("Ana Souza", "ana@email.com", "Senha1234"))).isEmpty();
    }

    @Test
    void cadastroIgnoraEspacosNasPontasDoNomeEDoEmail() {
        RegistroRequest req = new RegistroRequest("  Ana  ", "  ana@email.com  ", "Senha1234");

        assertThat(req.nome()).isEqualTo("Ana");
        assertThat(req.email()).isEqualTo("ana@email.com");
        assertThat(validador.validate(req)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"semarroba.com", "ana@", "@email.com", "ana@email", "ana @email.com", ""})
    void cadastroRecusaEmailInvalido(String email) {
        assertThat(validador.validate(new RegistroRequest("Ana", email, "Senha1234"))).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"curta1", "somenteletras", "12345678", ""})
    void cadastroRecusaSenhaFraca(String senha) {
        assertThat(validador.validate(new RegistroRequest("Ana", "ana@email.com", senha))).isNotEmpty();
    }

    @Test
    void cadastroRecusaSenhaComMaisDe64Caracteres() {
        assertThat(validador.validate(new RegistroRequest("Ana", "ana@email.com", "a1" + "b".repeat(63)))).isNotEmpty();
    }

    @Test
    void cadastroRecusaNomeVazio() {
        assertThat(validador.validate(new RegistroRequest("   ", "ana@email.com", "Senha1234"))).isNotEmpty();
    }

    /* ---------------- movimentacao ---------------- */

    private static TransacaoRequest transacao(String descricao, String valor) {
        return new TransacaoRequest(descricao, valor == null ? null : new BigDecimal(valor),
                LocalDate.of(2026, 11, 10), TipoTransacao.DESPESA, 1L);
    }

    @Test
    void movimentacaoValidaPassa() {
        assertThat(validador.validate(transacao("Mercado", "89.90"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-5.00"})
    void movimentacaoRecusaValorZeroOuNegativo(String valor) {
        assertThat(validador.validate(transacao("Mercado", valor))).isNotEmpty();
    }

    @Test
    void movimentacaoRecusaMaisDeDuasCasasDecimais() {
        assertThat(validador.validate(transacao("Mercado", "10.123"))).isNotEmpty();
    }

    @Test
    void movimentacaoRecusaValorAusenteEDescricaoVazia() {
        assertThat(validador.validate(transacao("Mercado", null))).isNotEmpty();
        assertThat(validador.validate(transacao("  ", "10.00"))).isNotEmpty();
    }

    @Test
    void movimentacaoRecusaDescricaoComMaisDe120Caracteres() {
        assertThat(validador.validate(transacao("x".repeat(121), "10.00"))).isNotEmpty();
        assertThat(validador.validate(transacao("x".repeat(120), "10.00"))).isEmpty();
    }
}
