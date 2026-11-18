// Telas de conta: entrar, criar conta, esqueci a senha, redefinir senha e confirmar e-mail.
// Cada página HTML marca o seu tipo em <body data-pagina="...">.
(() => {
    const $ = (id) => document.getElementById(id);
    const pagina = document.body.dataset.pagina;
    const parametros = new URLSearchParams(window.location.search);

    /* ---------------- Utilidades ---------------- */

    function mostrarMensagem(texto, tipo = "erro") {
        const caixa = $("mensagem");
        caixa.textContent = texto;
        caixa.className = "mensagem mensagem--" + tipo;
        caixa.hidden = false;
    }

    function limparMensagem() {
        const caixa = $("mensagem");
        caixa.hidden = true;
        caixa.textContent = "";
    }

    function marcarCampo(idCampo, mensagem) {
        const campo = $(idCampo);
        campo.classList.toggle("invalido", Boolean(mensagem));
        const erro = $(idCampo + "-erro");
        if (erro) erro.textContent = mensagem || "";
    }

    // Deixa o botão "ocupado" enquanto a requisição roda (evita cliques repetidos).
    async function comBotaoOcupado(botao, textoOcupado, acao) {
        const conteudoOriginal = Array.from(botao.childNodes, (no) => no.cloneNode(true)); // preserva ícones
        botao.disabled = true;
        botao.textContent = textoOcupado;
        try {
            return await acao();
        } finally {
            botao.disabled = false;
            botao.replaceChildren(...conteudoOriginal);
        }
    }

    const emailValido = (e) => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(e) && e.length <= 160;

    const REGRAS_SENHA = {
        tamanho: (s) => s.length >= 8 && s.length <= 64,
        letra: (s) => /[A-Za-z]/.test(s),
        numero: (s) => /\d/.test(s)
    };
    const senhaValida = (s) => Object.values(REGRAS_SENHA).every((teste) => teste(s));

    function ligarChecklistDeSenha(idInput) {
        const lista = $("regras-senha");
        if (!lista) return;
        const atualizar = () => {
            const valor = $(idInput).value;
            lista.querySelectorAll("li").forEach((li) => li.classList.toggle("ok", REGRAS_SENHA[li.dataset.regra](valor)));
        };
        $(idInput).addEventListener("input", atualizar);
        atualizar();
    }

    function ligarVerSenha() {
        document.querySelectorAll(".ver-senha").forEach((botao) => {
            botao.addEventListener("click", () => {
                const input = botao.parentElement.querySelector("input");
                const mostrar = input.type === "password";
                input.type = mostrar ? "text" : "password";
                botao.classList.toggle("visivel", mostrar);
                botao.setAttribute("aria-label", mostrar ? "Ocultar senha" : "Mostrar senha");
            });
        });
    }

    // Mostra "Continuar com o Google" só se o servidor tiver as credenciais configuradas
    // e "Entrar como visitante" só se o acesso de visitante estiver ligado (só existe na tela de login).
    async function prepararGoogle() {
        try {
            const cfg = await API.configuracao();
            if (cfg.google) {
                $("botao-google").hidden = false;
                $("divisor").hidden = false;
            }
            if (cfg.visitante && $("bloco-visitante")) {
                $("bloco-visitante").hidden = false;
            }
        } catch (e) {
            // sem Google: tudo bem, o login por e-mail continua funcionando
        }
    }

    // Quem já está logado vai direto para o sistema.
    async function irParaSistemaSeLogado() {
        try {
            await API.eu(false);
            window.location.replace("/");
        } catch (e) {
            // não está logado: segue na tela
        }
    }

    /* ---------------- Entrar ---------------- */

    function paginaLogin() {
        const erros = {
            "google": "Não foi possível entrar com o Google. Tente de novo ou use e-mail e senha.",
            "google-email": "O Google não confirmou o seu e-mail. Use outra conta ou entre com e-mail e senha."
        };
        if (parametros.get("erro") && erros[parametros.get("erro")]) mostrarMensagem(erros[parametros.get("erro")]);
        if (parametros.get("redefinida")) mostrarMensagem("Senha alterada! Entre com a nova senha.", "sucesso");
        if (parametros.get("confirmado")) mostrarMensagem("E-mail confirmado! Agora você já pode entrar.", "sucesso");

        irParaSistemaSeLogado();
        prepararGoogle();
        ligarVerSenha();

        $("botao-visitante").addEventListener("click", async () => {
            limparMensagem();
            try {
                await comBotaoOcupado($("botao-visitante"), "Preparando a sua conta…", () => API.entrarComoVisitante());
                window.location.href = "/";
            } catch (e) {
                mostrarMensagem(e.message);
            }
        });

        $("form-login").addEventListener("submit", async (evento) => {
            evento.preventDefault();
            limparMensagem();
            $("reenviar-area").hidden = true;

            const email = $("email").value.trim();
            const senha = $("senha").value;
            marcarCampo("email", emailValido(email) ? "" : "Informe um e-mail válido");
            marcarCampo("senha", senha ? "" : "Informe a senha");
            if (!emailValido(email) || !senha) return;

            try {
                await comBotaoOcupado($("botao-entrar"), "Entrando…", () => API.entrar({ email, senha }));
                window.location.href = "/";
            } catch (e) {
                mostrarMensagem(e.message);
                if (e.codigo === "EMAIL_NAO_VERIFICADO") $("reenviar-area").hidden = false;
            }
        });

        $("botao-reenviar").addEventListener("click", async () => {
            const email = $("email").value.trim();
            if (!emailValido(email)) return;
            try {
                const resposta = await API.reenviarConfirmacao(email);
                mostrarMensagem(resposta.mensagem, "sucesso");
                $("reenviar-area").hidden = true;
            } catch (e) {
                mostrarMensagem(e.message);
            }
        });
    }

    /* ---------------- Criar conta ---------------- */

    function paginaCadastro() {
        irParaSistemaSeLogado();
        prepararGoogle();
        ligarVerSenha();
        ligarChecklistDeSenha("senha");
        let emailCadastrado = "";

        $("form-cadastro").addEventListener("submit", async (evento) => {
            evento.preventDefault();
            limparMensagem();

            const nome = $("nome").value.trim();
            const email = $("email").value.trim();
            const senha = $("senha").value;
            const confirmar = $("confirmar").value;

            marcarCampo("nome", nome ? "" : "Informe seu nome");
            marcarCampo("email", emailValido(email) ? "" : "Informe um e-mail válido");
            marcarCampo("senha", senhaValida(senha) ? "" : "A senha não atende às regras abaixo");
            marcarCampo("confirmar", senha === confirmar ? "" : "As senhas não são iguais");
            if (!nome || !emailValido(email) || !senhaValida(senha) || senha !== confirmar) return;

            try {
                const resposta = await comBotaoOcupado($("botao-cadastrar"), "Criando conta…", () => API.cadastrar({ nome, email, senha }));
                emailCadastrado = email;
                $("form-cadastro").hidden = true;
                $("bloco-google").hidden = true;
                $("titulo-pagina").hidden = true;
                $("sucesso-texto").textContent = resposta.mensagem;
                $("sucesso-cadastro").hidden = false;
                document.querySelector(".auth-rodape").hidden = true;
            } catch (e) {
                mostrarMensagem(e.message);
            }
        });

        $("botao-reenviar").addEventListener("click", async () => {
            try {
                const resposta = await API.reenviarConfirmacao(emailCadastrado);
                mostrarMensagem(resposta.mensagem, "info");
            } catch (e) {
                mostrarMensagem(e.message);
            }
        });
    }

    /* ---------------- Esqueci minha senha ---------------- */

    function paginaEsqueciSenha() {
        $("form-esqueci").addEventListener("submit", async (evento) => {
            evento.preventDefault();
            limparMensagem();
            const email = $("email").value.trim();
            marcarCampo("email", emailValido(email) ? "" : "Informe um e-mail válido");
            if (!emailValido(email)) return;

            try {
                const resposta = await comBotaoOcupado($("botao-enviar"), "Enviando…", () => API.esqueciSenha(email));
                mostrarMensagem(resposta.mensagem, "sucesso");
                $("form-esqueci").hidden = true;
            } catch (e) {
                mostrarMensagem(e.message);
            }
        });
    }

    /* ---------------- Redefinir senha ---------------- */

    function paginaRedefinirSenha() {
        const token = parametros.get("token");
        ligarVerSenha();
        ligarChecklistDeSenha("senha");

        if (!token) {
            $("form-redefinir").hidden = true;
            mostrarMensagem("Link inválido. Peça um novo link de redefinição.");
            $("link-novo").hidden = false;
            return;
        }

        $("form-redefinir").addEventListener("submit", async (evento) => {
            evento.preventDefault();
            limparMensagem();
            const senha = $("senha").value;
            const confirmar = $("confirmar").value;
            marcarCampo("senha", senhaValida(senha) ? "" : "A senha não atende às regras abaixo");
            marcarCampo("confirmar", senha === confirmar ? "" : "As senhas não são iguais");
            if (!senhaValida(senha) || senha !== confirmar) return;

            try {
                await comBotaoOcupado($("botao-redefinir"), "Salvando…", () => API.redefinirSenha(token, senha));
                window.location.href = "/login.html?redefinida=1";
            } catch (e) {
                mostrarMensagem(e.message);
                if (e.codigo === "LINK_INVALIDO") $("link-novo").hidden = false;
            }
        });
    }

    /* ---------------- Confirmar e-mail ---------------- */

    async function paginaVerificarEmail() {
        const token = parametros.get("token");
        const mostrar = (estado, titulo, texto) => {
            const icone = $("resultado-icone");
            icone.className = "resultado__icone resultado__icone--" + estado;
            icone.textContent = estado === "ok" ? "✓" : estado === "erro" ? "!" : "…";
            $("resultado-titulo").textContent = titulo;
            $("resultado-texto").textContent = texto;
        };

        if (!token) {
            mostrar("erro", "Link inválido", "Este link de confirmação está incompleto. Abra o link do e-mail novamente.");
            $("acoes-erro").hidden = false;
            return;
        }
        try {
            await API.verificarEmail(token);
            mostrar("ok", "E-mail confirmado!", "Sua conta está ativa. Agora você já pode entrar.");
            $("acoes-ok").hidden = false;
        } catch (e) {
            mostrar("erro", "Não foi possível confirmar", e.message);
            $("acoes-erro").hidden = false;
        }
    }

    /* ---------------- Início ---------------- */

    ({
        "login": paginaLogin,
        "cadastro": paginaCadastro,
        "esqueci-senha": paginaEsqueciSenha,
        "redefinir-senha": paginaRedefinirSenha,
        "verificar-email": paginaVerificarEmail
    })[pagina]?.();

    // Garante o cookie do token CSRF antes do primeiro envio de formulário.
    if (pagina !== "login" && pagina !== "cadastro") API.configuracao().catch(() => {});
})();
