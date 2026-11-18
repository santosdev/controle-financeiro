package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.model.TipoToken;
import com.santosdev.financecontrol.model.TokenUsuario;
import com.santosdev.financecontrol.model.Usuario;
import com.santosdev.financecontrol.repository.TokenUsuarioRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/** Gera e confere os tokens de uso unico dos links enviados por e-mail. */
@Service
public class TokenService {

    private final TokenUsuarioRepository tokens;
    private final SecureRandom aleatorio = new SecureRandom();

    public TokenService(TokenUsuarioRepository tokens) {
        this.tokens = tokens;
    }

    /** Cria um token novo (apagando os anteriores do mesmo tipo) e devolve o valor original, que vai so no e-mail. */
    public String criar(Usuario usuario, TipoToken tipo, Duration validade) {
        tokens.apagarDoUsuario(usuario.getId(), tipo);
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new TokenUsuario(usuario, tipo, hash(valor), Instant.now().plus(validade)));
        return valor;
    }

    public Optional<TokenUsuario> buscar(String valor, TipoToken tipo) {
        if (valor == null || valor.isBlank() || valor.length() > 100) {
            return Optional.empty();
        }
        return tokens.buscarPorHash(hash(valor.trim()), tipo);
    }

    /** True se um token deste tipo foi gerado para o usuario dentro da janela (evita disparar e-mails em sequencia). */
    public boolean gerouRecentemente(Long usuarioId, TipoToken tipo, Duration janela) {
        return tokens.findFirstByUsuarioIdAndTipoOrderByCriadoEmDesc(usuarioId, tipo)
                .map(t -> t.getCriadoEm().plus(janela).isAfter(Instant.now()))
                .orElse(false);
    }

    static String hash(String valor) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
