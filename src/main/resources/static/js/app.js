// Lógica da interface do FinanceControl.

const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const $ = (id) => document.getElementById(id);

let categorias = [];
let transacoes = [];
let temporizadorAviso;

/* ---------------- Utilidades ---------------- */

function formatarData(iso) {
    const [ano, mes, dia] = iso.split("-");
    return `${dia}/${mes}/${ano}`;
}

function hojeISO() {
    const d = new Date();
    const mes = String(d.getMonth() + 1).padStart(2, "0");
    const dia = String(d.getDate()).padStart(2, "0");
    return `${d.getFullYear()}-${mes}-${dia}`;
}

function mostrarAviso(texto, erro = false) {
    const aviso = $("aviso");
    aviso.textContent = texto;
    aviso.classList.toggle("aviso--erro", erro);
    aviso.hidden = false;
    clearTimeout(temporizadorAviso);
    temporizadorAviso = setTimeout(() => (aviso.hidden = true), 3500);
}

// Cria um elemento de forma segura (textContent evita injeção de HTML).
function el(tag, { classe, texto, ...atributos } = {}, filhos = []) {
    const e = document.createElement(tag);
    if (classe) e.className = classe;
    if (texto !== undefined) e.textContent = texto;
    Object.entries(atributos).forEach(([k, v]) => e.setAttribute(k, v));
    filhos.forEach((f) => e.appendChild(f));
    return e;
}

function selo(tipo) {
    return el("span", {
        classe: "selo selo--" + tipo.toLowerCase(),
        texto: tipo === "RECEITA" ? "Receita" : "Despesa"
    });
}

function confirmar(texto) {
    return new Promise((resolve) => {
        const modal = $("modal-confirmar");
        $("texto-confirmar").textContent = texto;
        modal.returnValue = "";
        modal.addEventListener("close", () => resolve(modal.returnValue === "confirmar"), { once: true });
        modal.showModal();
    });
}

function mostrarErroForm(idElemento, mensagem) {
    const p = $(idElemento);
    p.textContent = mensagem;
    p.hidden = false;
}

/* ---------------- Abas ---------------- */

document.querySelectorAll(".aba").forEach((botao) => {
    botao.addEventListener("click", () => {
        document.querySelectorAll(".aba").forEach((b) => b.classList.toggle("ativa", b === botao));
        $("aba-movimentacoes").hidden = botao.dataset.aba !== "movimentacoes";
        $("aba-categorias").hidden = botao.dataset.aba !== "categorias";
    });
});

document.querySelectorAll("[data-fechar]").forEach((botao) => {
    botao.addEventListener("click", () => botao.closest("dialog").close());
});

/* ---------------- Categorias ---------------- */

async function carregarCategorias() {
    categorias = await API.listarCategorias();
    renderizarCategorias();
    preencherFiltroCategorias();
}

function renderizarCategorias() {
    const corpo = $("lista-categorias");
    corpo.replaceChildren();
    $("vazio-categorias").hidden = categorias.length > 0;

    categorias.forEach((c) => {
        const botoes = el("div", { classe: "acoes-linha" }, [
            botaoMini("Editar", "botao--neutro", () => abrirModalCategoria(c)),
            botaoMini("Excluir", "botao--despesa", () => excluirCategoria(c))
        ]);
        corpo.appendChild(el("tr", {}, [
            el("td", { texto: c.nome }),
            el("td", {}, [selo(c.tipo)]),
            el("td", { classe: "direita" }, [botoes])
        ]));
    });
}

function botaoMini(texto, classeExtra, aoClicar) {
    const b = el("button", { classe: "botao botao--mini " + classeExtra, texto, type: "button" });
    b.addEventListener("click", aoClicar);
    return b;
}

function preencherFiltroCategorias() {
    const select = $("filtro-categoria");
    const atual = select.value;
    select.replaceChildren(el("option", { value: "", texto: "Todas" }));
    categorias.forEach((c) => {
        select.appendChild(el("option", { value: c.id, texto: `${c.nome} (${c.tipo === "RECEITA" ? "receita" : "despesa"})` }));
    });
    select.value = atual;
}

function abrirModalCategoria(categoria) {
    $("erro-categoria").hidden = true;
    $("titulo-categoria").textContent = categoria ? "Editar categoria" : "Nova categoria";
    $("categoria-id").value = categoria ? categoria.id : "";
    $("categoria-nome").value = categoria ? categoria.nome : "";
    $("categoria-tipo").value = categoria ? categoria.tipo : "DESPESA";
    $("modal-categoria").showModal();
    $("categoria-nome").focus();
}

$("nova-categoria").addEventListener("click", () => abrirModalCategoria(null));

$("form-categoria").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const id = $("categoria-id").value;
    const dados = { nome: $("categoria-nome").value, tipo: $("categoria-tipo").value };
    try {
        if (id) {
            await API.atualizarCategoria(id, dados);
        } else {
            await API.criarCategoria(dados);
        }
        $("modal-categoria").close();
        mostrarAviso(id ? "Categoria atualizada." : "Categoria criada.");
        await carregarCategorias();
        await carregarTransacoes();
    } catch (e) {
        mostrarErroForm("erro-categoria", e.message);
    }
});

async function excluirCategoria(categoria) {
    if (!(await confirmar(`Excluir a categoria "${categoria.nome}"?`))) return;
    try {
        await API.excluirCategoria(categoria.id);
        mostrarAviso("Categoria excluída.");
        await carregarCategorias();
    } catch (e) {
        mostrarAviso(e.message, true);
    }
}

/* ---------------- Movimentações ---------------- */

function filtrosAtuais() {
    return {
        inicio: $("filtro-inicio").value,
        fim: $("filtro-fim").value,
        tipo: $("filtro-tipo").value,
        categoriaId: $("filtro-categoria").value
    };
}

async function carregarTransacoes() {
    try {
        transacoes = await API.listarTransacoes(filtrosAtuais());
        renderizarTransacoes();
    } catch (e) {
        mostrarAviso(e.message, true);
    }
}

function renderizarTransacoes() {
    const corpo = $("lista-transacoes");
    corpo.replaceChildren();
    $("vazio-transacoes").hidden = transacoes.length > 0;

    let receitas = 0;
    let despesas = 0;

    transacoes.forEach((t) => {
        if (t.tipo === "RECEITA") receitas += t.valor; else despesas += t.valor;

        const sinal = t.tipo === "RECEITA" ? "+ " : "- ";
        const botoes = el("div", { classe: "acoes-linha" }, [
            botaoMini("Editar", "botao--neutro", () => abrirModalTransacao(t.tipo, t)),
            botaoMini("Excluir", "botao--despesa", () => excluirTransacao(t))
        ]);
        corpo.appendChild(el("tr", {}, [
            el("td", { texto: formatarData(t.data) }),
            el("td", { texto: t.descricao }),
            el("td", { texto: t.categoria.nome }),
            el("td", {}, [selo(t.tipo)]),
            el("td", { classe: "direita valor valor--" + t.tipo.toLowerCase(), texto: sinal + moeda.format(t.valor) }),
            el("td", { classe: "direita" }, [botoes])
        ]));
    });

    const saldo = receitas - despesas;
    $("total-receitas").textContent = moeda.format(receitas);
    $("total-despesas").textContent = moeda.format(despesas);
    $("total-saldo").textContent = moeda.format(saldo);
    $("total-saldo").classList.toggle("negativo", saldo < 0);
}

function abrirModalTransacao(tipo, transacao) {
    $("erro-transacao").hidden = true;
    const rotulo = tipo === "RECEITA" ? "receita" : "despesa";
    $("titulo-transacao").textContent = (transacao ? "Editar " : "Nova ") + rotulo;
    $("transacao-id").value = transacao ? transacao.id : "";
    $("transacao-tipo").value = tipo;
    $("transacao-descricao").value = transacao ? transacao.descricao : "";
    $("transacao-valor").value = transacao ? transacao.valor : "";
    $("transacao-data").value = transacao ? transacao.data : hojeISO();

    const select = $("transacao-categoria");
    select.replaceChildren();
    const doTipo = categorias.filter((c) => c.tipo === tipo);
    if (doTipo.length === 0) {
        select.appendChild(el("option", { value: "", texto: "Cadastre uma categoria de " + rotulo + " primeiro" }));
    }
    doTipo.forEach((c) => select.appendChild(el("option", { value: c.id, texto: c.nome })));
    if (transacao) select.value = transacao.categoria.id;

    $("modal-transacao").showModal();
    $("transacao-descricao").focus();
}

$("nova-receita").addEventListener("click", () => abrirModalTransacao("RECEITA", null));
$("nova-despesa").addEventListener("click", () => abrirModalTransacao("DESPESA", null));

$("form-transacao").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const id = $("transacao-id").value;
    const categoriaId = $("transacao-categoria").value;
    const dados = {
        descricao: $("transacao-descricao").value,
        valor: $("transacao-valor").value === "" ? null : Number($("transacao-valor").value),
        data: $("transacao-data").value || null,
        tipo: $("transacao-tipo").value,
        categoriaId: categoriaId === "" ? null : Number(categoriaId)
    };
    try {
        if (id) {
            await API.atualizarTransacao(id, dados);
        } else {
            await API.criarTransacao(dados);
        }
        $("modal-transacao").close();
        mostrarAviso(id ? "Movimentação atualizada." : "Movimentação cadastrada.");
        await carregarTransacoes();
    } catch (e) {
        mostrarErroForm("erro-transacao", e.message);
    }
});

async function excluirTransacao(t) {
    if (!(await confirmar(`Excluir "${t.descricao}" (${moeda.format(t.valor)})?`))) return;
    try {
        await API.excluirTransacao(t.id);
        mostrarAviso("Movimentação excluída.");
        await carregarTransacoes();
    } catch (e) {
        mostrarAviso(e.message, true);
    }
}

/* ---------------- Filtros ---------------- */

$("form-filtros").addEventListener("submit", (evento) => {
    evento.preventDefault();
    carregarTransacoes();
});

$("limpar-filtros").addEventListener("click", () => {
    $("form-filtros").reset();
    carregarTransacoes();
});

/* ---------------- Inicialização ---------------- */

(async function iniciar() {
    try {
        await carregarCategorias();
        await carregarTransacoes();
    } catch (e) {
        mostrarAviso(e.message, true);
    }
})();
