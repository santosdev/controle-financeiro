package com.santosdev.financecontrol.controller;

import com.santosdev.financecontrol.dto.AlterarSenhaRequest;
import com.santosdev.financecontrol.dto.ExcluirContaRequest;
import com.santosdev.financecontrol.dto.MensagemResponse;
import com.santosdev.financecontrol.dto.PerfilRequest;
import com.santosdev.financecontrol.dto.UsuarioResponse;
import com.santosdev.financecontrol.exception.ApiException;
import com.santosdev.financecontrol.service.ImagemDePerfil;
import com.santosdev.financecontrol.service.PerfilService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfil;

    public PerfilController(PerfilService perfil) {
        this.perfil = perfil;
    }

    @GetMapping
    public UsuarioResponse obter() {
        return perfil.obter();
    }

    @PutMapping
    public UsuarioResponse atualizar(@Valid @RequestBody PerfilRequest req) {
        return perfil.atualizar(req);
    }

    @PutMapping("/senha")
    public MensagemResponse alterarSenha(@Valid @RequestBody AlterarSenhaRequest req) {
        perfil.alterarSenha(req);
        return new MensagemResponse("Senha alterada com sucesso.");
    }

    /** A imagem de cada usuario so e entregue a ele mesmo. O navegador guarda por 1 dia; ?v= forca a troca. */
    @GetMapping("/foto")
    public ResponseEntity<byte[]> foto() {
        return perfil.foto()
                .map(bytes -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                        .body(bytes))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Corpo = a propria imagem (JPG, PNG ou GIF), com no maximo 2 MB. */
    @PutMapping("/foto")
    public UsuarioResponse trocarFoto(HttpServletRequest request) throws IOException {
        if (request.getContentLengthLong() > ImagemDePerfil.TAMANHO_MAXIMO_BYTES) {
            throw tamanhoExcedido();
        }
        // Le no maximo 2 MB + 1 byte, mesmo que o cabecalho de tamanho diga outra coisa.
        byte[] corpo = request.getInputStream().readNBytes(ImagemDePerfil.TAMANHO_MAXIMO_BYTES + 1);
        if (corpo.length > ImagemDePerfil.TAMANHO_MAXIMO_BYTES) {
            throw tamanhoExcedido();
        }
        return perfil.trocarFoto(corpo);
    }

    @DeleteMapping("/foto")
    public UsuarioResponse removerFoto() {
        return perfil.removerFoto();
    }

    /** POST (e nao DELETE) porque leva um corpo com a confirmacao. Depois de excluir, a sessao e encerrada. */
    @PostMapping("/excluir-conta")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirConta(@Valid @RequestBody ExcluirContaRequest req, HttpServletRequest request) {
        perfil.excluirConta(req);
        SecurityContextHolder.clearContext();
        HttpSession sessao = request.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
    }

    private static ApiException tamanhoExcedido() {
        return new ApiException(HttpStatus.CONTENT_TOO_LARGE, "FOTO_GRANDE", "A foto deve ter no máximo 2 MB.");
    }
}
