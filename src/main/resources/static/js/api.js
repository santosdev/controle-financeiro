// Camada de comunicação com o backend (API REST do Spring Boot).
const API = (() => {
    const BASE = "/api";

    async function requisicao(caminho, opcoes = {}) {
        let resposta;
        try {
            resposta = await fetch(BASE + caminho, {
                headers: { "Content-Type": "application/json" },
                ...opcoes
            });
        } catch (e) {
            throw new Error("Não foi possível conectar ao servidor. Ele está rodando?");
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
            throw new Error(montarMensagemErro(corpo, resposta.status));
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

    return {
        listarCategorias: (tipo) => requisicao("/categorias" + paraQuery({ tipo })),
        criarCategoria: (dados) => requisicao("/categorias", { method: "POST", body: JSON.stringify(dados) }),
        atualizarCategoria: (id, dados) => requisicao("/categorias/" + id, { method: "PUT", body: JSON.stringify(dados) }),
        excluirCategoria: (id) => requisicao("/categorias/" + id, { method: "DELETE" }),

        dashboard: (filtros) => requisicao("/dashboard" + paraQuery(filtros || {})),

        listarTransacoes: (filtros) => requisicao("/transacoes" + paraQuery(filtros || {})),
        criarTransacao: (dados) => requisicao("/transacoes", { method: "POST", body: JSON.stringify(dados) }),
        atualizarTransacao: (id, dados) => requisicao("/transacoes/" + id, { method: "PUT", body: JSON.stringify(dados) }),
        excluirTransacao: (id) => requisicao("/transacoes/" + id, { method: "DELETE" })
    };
})();
