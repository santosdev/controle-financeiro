// Camada de comunicação com o backend (API REST do Spring Boot).
const API = (() => {
    const BASE = "/api";

    // Erro da API: guarda o código curto (ex.: EMAIL_NAO_VERIFICADO) e o status HTTP.
    class ErroApi extends Error {
        constructor(mensagem, status, codigo) {
            super(mensagem);
            this.status = status;
            this.codigo = codigo;
        }
    }

    function lerCookie(nome) {
        const achado = document.cookie.split("; ").find((c) => c.startsWith(nome + "="));
        return achado ? decodeURIComponent(achado.substring(nome.length + 1)) : null;
    }

    async function requisicao(caminho, opcoes = {}) {
        const { redirecionarSeDeslogado, ...resto } = opcoes;
        // Por padrão, um 401 leva ao login, exceto nas telas de conta (onde 401 = senha errada).
        const redirecionar = redirecionarSeDeslogado ?? !caminho.startsWith("/auth/");
        const metodo = (resto.method || "GET").toUpperCase();
        const cabecalhos = { "Content-Type": "application/json", ...(resto.headers || {}) };

        // Proteção CSRF: devolve o token do cookie no cabeçalho em toda requisição que altera dados.
        if (metodo !== "GET" && metodo !== "HEAD") {
            const token = lerCookie("XSRF-TOKEN");
            if (token) cabecalhos["X-XSRF-TOKEN"] = token;
        }

        let resposta;
        try {
            resposta = await fetch(BASE + caminho, { ...resto, headers: cabecalhos, credentials: "same-origin" });
        } catch (e) {
            throw new ErroApi("Não foi possível conectar ao servidor. Ele está rodando?", 0, "SEM_CONEXAO");
        }

        if (resposta.status === 204) {
            return null;
        }

        let corpo = null;
        try {
            corpo = await resposta.json();
        } catch (e) {
            // resposta sem JSON
        }

        if (!resposta.ok) {
            // Sessão expirada: volta para o login (as telas de login/cadastro tratam o 401 por conta própria).
            if (resposta.status === 401 && redirecionar) {
                window.location.href = "/login.html";
            }
            throw new ErroApi(montarMensagemErro(corpo, resposta.status), resposta.status, corpo && corpo.codigo);
        }
        return corpo;
    }

    function montarMensagemErro(corpo, status) {
        if (corpo && corpo.campos) {
            return Object.values(corpo.campos).join(" · ");
        }
        if (corpo && corpo.erro) {
            return corpo.erro;
        }
        return "Erro inesperado (código " + status + ").";
    }

    function paraQuery(filtros) {
        const params = new URLSearchParams();
        Object.entries(filtros).forEach(([chave, valor]) => {
            if (valor !== "" && valor !== null && valor !== undefined) {
                params.append(chave, valor);
            }
        });
        const texto = params.toString();
        return texto ? "?" + texto : "";
    }

    const enviar = (metodo, caminho, dados, opcoes = {}) =>
        requisicao(caminho, { method: metodo, body: JSON.stringify(dados), ...opcoes });

    return {
        // ----- Conta e login -----
        configuracao: () => requisicao("/auth/config", { redirecionarSeDeslogado: false }),
        // Devolve o usuário logado. Sem login: leva para a tela de entrar (ou, com redirecionar=false, só lança o erro).
        eu: async (redirecionar = true) => {
            const sessao = await requisicao("/auth/sessao", { redirecionarSeDeslogado: false });
            if (!sessao.autenticado) {
                if (redirecionar) window.location.href = "/login.html";
                throw new ErroApi("Você precisa entrar para continuar", 401, "NAO_AUTENTICADO");
            }
            return sessao.usuario;
        },
        cadastrar: (dados) => enviar("POST", "/auth/cadastro", dados),
        verificarEmail: (token) => enviar("POST", "/auth/verificar-email", { token }),
        reenviarConfirmacao: (email) => enviar("POST", "/auth/reenviar-confirmacao", { email }),
        entrar: (dados) => enviar("POST", "/auth/login", dados),
        sair: () => requisicao("/auth/logout", { method: "POST", redirecionarSeDeslogado: false }),
        esqueciSenha: (email) => enviar("POST", "/auth/esqueci-senha", { email }),
        redefinirSenha: (token, novaSenha) => enviar("POST", "/auth/redefinir-senha", { token, novaSenha }),

        // ----- Perfil -----
        atualizarPerfil: (dados) => enviar("PUT", "/perfil", dados),
        alterarSenha: (dados) => enviar("PUT", "/perfil/senha", dados),
        // A imagem vai como corpo da requisição (sem JSON). O servidor valida e padroniza.
        enviarFoto: (arquivo) => requisicao("/perfil/foto", {
            method: "PUT",
            body: arquivo,
            headers: { "Content-Type": arquivo.type || "application/octet-stream" }
        }),
        removerFoto: () => requisicao("/perfil/foto", { method: "DELETE" }),
        excluirConta: (confirmacao) => enviar("POST", "/perfil/excluir-conta", { confirmacao }),

        // ----- Dados financeiros -----
        listarCategorias: (tipo) => requisicao("/categorias" + paraQuery({ tipo })),
        criarCategoria: (dados) => enviar("POST", "/categorias", dados),
        atualizarCategoria: (id, dados) => enviar("PUT", "/categorias/" + id, dados),
        excluirCategoria: (id) => requisicao("/categorias/" + id, { method: "DELETE" }),

        dashboard: (filtros) => requisicao("/dashboard" + paraQuery(filtros || {})),

        listarTransacoes: (filtros) => requisicao("/transacoes" + paraQuery(filtros || {})),
        criarTransacao: (dados) => enviar("POST", "/transacoes", dados),
        atualizarTransacao: (id, dados) => enviar("PUT", "/transacoes/" + id, dados),
        excluirTransacao: (id) => requisicao("/transacoes/" + id, { method: "DELETE" })
    };
})();
