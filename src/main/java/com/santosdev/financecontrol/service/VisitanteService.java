package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.exception.ApiException;
import com.santosdev.financecontrol.model.ProvedorLogin;
import com.santosdev.financecontrol.model.Usuario;
import com.santosdev.financecontrol.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Entrar como visitante": cria uma conta temporaria, igual a uma conta nova (com as categorias padrao),
 * para quem quer conhecer o sistema sem se cadastrar. Cada visitante tem a sua conta, entao um nao ve os dados do outro.
 * A conta e apagada quando a pessoa sai ou, no maximo, depois de {@link #DURACAO}.
 */
@Service
public class VisitanteService {

    private static final Logger log = LoggerFactory.getLogger(VisitanteService.class);

    public static final Duration DURACAO = Duration.ofHours(24);
    static final int LIMITE_POR_IP = 10;
    static final Duration JANELA_DO_LIMITE = Duration.ofHours(1);
    /** Teto de visitantes ao mesmo tempo, para ninguem lotar o banco criando contas sem parar. */
    static final long MAXIMO_DE_VISITANTES = 300;
    /** Dominio reservado (RFC 2606): nunca recebe e-mail, entao nada e enviado por engano. */
    static final String DOMINIO = "@visitante.invalid";

    private final UsuarioRepository usuarios;
    private final CategoriasPadraoService categoriasPadrao;
    private final PerfilService perfil;
    private final boolean habilitado;
    private final Clock relogio;
    private final Map<String, Deque<Instant>> criacoesPorIp = new HashMap<>();

    @Autowired
    public VisitanteService(UsuarioRepository usuarios, CategoriasPadraoService categoriasPadrao, PerfilService perfil,
                            @Value("${app.visitante.habilitado:true}") boolean habilitado) {
        this(usuarios, categoriasPadrao, perfil, habilitado, Clock.systemUTC());
    }

    VisitanteService(UsuarioRepository usuarios, CategoriasPadraoService categoriasPadrao, PerfilService perfil,
                     boolean habilitado, Clock relogio) {
        this.usuarios = usuarios;
        this.categoriasPadrao = categoriasPadrao;
        this.perfil = perfil;
        this.habilitado = habilitado;
        this.relogio = relogio;
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    @Transactional
    public Usuario criar(String ip) {
        if (!habilitado) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VISITANTE_DESLIGADO", "O acesso como visitante está desativado.");
        }
        if (usuarios.countByProvedor(ProvedorLogin.VISITANTE) >= MAXIMO_DE_VISITANTES) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "VISITANTES_ESGOTADOS",
                    "Muitos visitantes agora. Tente de novo mais tarde ou crie uma conta.");
        }
        registrarCriacao(ip == null ? "?" : ip);

        String email = "visitante-" + UUID.randomUUID() + DOMINIO;
        Usuario visitante = usuarios.save(new Usuario("Visitante", email, null, ProvedorLogin.VISITANTE, true));
        categoriasPadrao.criarPara(visitante);
        return visitante;
    }

    /** Ao sair, a conta de visitante deixa de ter dono (nao ha senha para voltar), entao ja e apagada. */
    public void apagar(Long usuarioId) {
        perfil.apagarTudoDoUsuario(usuarioId);
    }

    /** De hora em hora apaga os visitantes com mais de 24 horas (quem fechou o navegador sem clicar em "Sair"). */
    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
    public void apagarExpirados() {
        List<Long> expirados = usuarios.idsCriadosAntesDe(ProvedorLogin.VISITANTE, relogio.instant().minus(DURACAO));
        for (Long id : expirados) {
            try {
                perfil.apagarTudoDoUsuario(id);
            } catch (RuntimeException e) {
                log.warn("Não foi possível apagar o visitante {}: {}", id, e.getMessage());
            }
        }
        if (!expirados.isEmpty()) {
            log.info("{} conta(s) de visitante expirada(s) apagada(s)", expirados.size());
        }
    }

    /** No maximo {@link #LIMITE_POR_IP} contas por endereco a cada {@link #JANELA_DO_LIMITE}. */
    private synchronized void registrarCriacao(String ip) {
        Instant agora = relogio.instant();
        Instant inicioDaJanela = agora.minus(JANELA_DO_LIMITE);
        criacoesPorIp.values().forEach(fila -> fila.removeIf(momento -> momento.isBefore(inicioDaJanela)));
        criacoesPorIp.values().removeIf(Deque::isEmpty);

        Deque<Instant> fila = criacoesPorIp.computeIfAbsent(ip, chave -> new ArrayDeque<>());
        if (fila.size() >= LIMITE_POR_IP) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "MUITOS_VISITANTES",
                    "Você já entrou como visitante várias vezes. Tente de novo mais tarde ou crie uma conta.");
        }
        fila.addLast(agora);
    }
}
