package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.dto.AlterarSenhaRequest;
import com.santosdev.financecontrol.dto.ExcluirContaRequest;
import com.santosdev.financecontrol.dto.PerfilRequest;
import com.santosdev.financecontrol.dto.UsuarioResponse;
import com.santosdev.financecontrol.exception.ApiException;
import com.santosdev.financecontrol.model.FotoUsuario;
import com.santosdev.financecontrol.model.Usuario;
import com.santosdev.financecontrol.repository.CategoriaRepository;
import com.santosdev.financecontrol.repository.FotoUsuarioRepository;
import com.santosdev.financecontrol.repository.TransacaoRepository;
import com.santosdev.financecontrol.repository.UsuarioRepository;
import com.santosdev.financecontrol.security.UsuarioAtual;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Perfil de quem esta logado: apelido, foto, senha e exclusao da conta. */
@Service
public class PerfilService {

    private final UsuarioRepository usuarios;
    private final FotoUsuarioRepository fotos;
    private final TransacaoRepository transacoes;
    private final CategoriaRepository categorias;
    private final PasswordEncoder encoder;
    private final UsuarioAtual usuarioAtual;

    public PerfilService(UsuarioRepository usuarios, FotoUsuarioRepository fotos, TransacaoRepository transacoes,
                         CategoriaRepository categorias, PasswordEncoder encoder, UsuarioAtual usuarioAtual) {
        this.usuarios = usuarios;
        this.fotos = fotos;
        this.transacoes = transacoes;
        this.categorias = categorias;
        this.encoder = encoder;
        this.usuarioAtual = usuarioAtual;
    }

    /** Dados do usuario para a tela. Vazio se a conta nao existe mais (sessao antiga de conta excluida). */
    @Transactional(readOnly = true)
    public Optional<UsuarioResponse> buscar(Long usuarioId) {
        return usuarios.findById(usuarioId).map(this::resposta);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obter() {
        return resposta(atual());
    }

    public UsuarioResponse resposta(Usuario usuario) {
        Long versao = fotos.buscarAtualizadaEm(usuario.getId()).map(Instant::toEpochMilli).orElse(null);
        return UsuarioResponse.de(usuario, versao);
    }

    /* ---------------- Dados ---------------- */

    @Transactional
    public UsuarioResponse atualizar(PerfilRequest req) {
        Usuario usuario = atual();
        usuario.setNome(req.nome());
        usuario.setApelido(req.apelido() == null || req.apelido().isBlank() ? null : req.apelido());
        return resposta(usuario);
    }

    /* ---------------- Foto ---------------- */

    @Transactional
    public UsuarioResponse trocarFoto(byte[] imagemEnviada) {
        Usuario usuario = atual();
        fotos.save(new FotoUsuario(usuario.getId(), ImagemDePerfil.processar(imagemEnviada), Instant.now()));
        return resposta(usuario);
    }

    @Transactional
    public UsuarioResponse removerFoto() {
        Usuario usuario = atual();
        fotos.deleteById(usuario.getId());
        return resposta(usuario);
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> foto() {
        return fotos.findById(usuarioAtual.id()).map(FotoUsuario::getDados);
    }

    /** Usado no primeiro login com o Google: nunca sobrescreve uma foto que a pessoa ja escolheu. */
    @Transactional
    public void definirFotoSeNaoTiver(Long usuarioId, byte[] imagem) {
        if (fotos.existsById(usuarioId)) {
            return;
        }
        fotos.save(new FotoUsuario(usuarioId, ImagemDePerfil.processar(imagem), Instant.now()));
    }

    /* ---------------- Senha ---------------- */

    /** Errar a senha atual conta como tentativa de login, para a tela nao servir de atalho para adivinhar senhas. */
    @Transactional(noRollbackFor = ApiException.class)
    public void alterarSenha(AlterarSenhaRequest req) {
        Usuario usuario = atual();
        if (usuario.getSenhaHash() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SEM_SENHA",
                    "Sua conta entra pelo Google e não tem senha. Use \"Esqueci minha senha\" na tela de login para criar uma.");
        }
        conferirSenhaOuFalhar(usuario, req.senhaAtual(), "SENHA_ATUAL_INCORRETA", "A senha atual está incorreta.");
        usuario.setSenhaHash(encoder.encode(req.novaSenha()));
    }

    /* ---------------- Excluir conta ---------------- */

    /**
     * Apaga a conta e tudo que e dela. Exige a senha (ou, para quem so usa o Google, o e-mail digitado).
     * As movimentacoes saem antes das categorias porque referenciam elas; tokens e foto saem por cascata no banco.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void excluirConta(ExcluirContaRequest req) {
        Usuario usuario = atual();
        if (usuario.getSenhaHash() != null) {
            conferirSenhaOuFalhar(usuario, req.confirmacao(), "CONFIRMACAO_INCORRETA", "A senha informada está incorreta.");
        } else if (!AuthService.normalizar(req.confirmacao()).equals(usuario.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CONFIRMACAO_INCORRETA",
                    "Digite o e-mail da conta exatamente como aparece em \"E-mail\".");
        }
        Long id = usuario.getId();
        transacoes.apagarDoUsuario(id);
        categorias.apagarDoUsuario(id);
        usuarios.deleteById(id);
    }

    /* ---------------- Internos ---------------- */

    private void conferirSenhaOuFalhar(Usuario usuario, String senhaDigitada, String codigo, String mensagem) {
        if (usuario.estaBloqueado(Instant.now())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "CONTA_BLOQUEADA",
                    "Muitas tentativas incorretas. Tente novamente mais tarde ou redefina sua senha.");
        }
        if (!encoder.matches(senhaDigitada, usuario.getSenhaHash())) {
            usuario.registrarFalhaDeLogin(AuthService.LIMITE_DE_FALHAS, AuthService.BLOQUEIO);
            throw new ApiException(HttpStatus.BAD_REQUEST, codigo, mensagem);
        }
        usuario.zerarFalhasDeLogin();
    }

    private Usuario atual() {
        return usuarios.findById(usuarioAtual.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Você precisa entrar para continuar"));
    }
}
