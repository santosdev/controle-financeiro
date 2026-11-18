// Tema claro/escuro. Carregado no <head> para aplicar o tema antes da página aparecer (evita "piscar").
(function () {
    const CHAVE = "financecontrol-tema";
    const raiz = document.documentElement;
    const sistemaEscuro = window.matchMedia("(prefers-color-scheme: dark)");

    function lerSalvo() {
        try {
            const valor = localStorage.getItem(CHAVE);
            return valor === "claro" || valor === "escuro" ? valor : null;
        } catch (e) {
            return null;
        }
    }

    function salvar(tema) {
        try {
            localStorage.setItem(CHAVE, tema);
        } catch (e) {
            // navegador bloqueou o armazenamento: o tema vale só nesta visita
        }
    }

    function aplicar(tema) {
        raiz.setAttribute("data-tema", tema);
        const botao = document.getElementById("botao-tema");
        if (botao) {
            const paraEscuro = tema === "claro";
            botao.setAttribute("aria-label", paraEscuro ? "Ativar modo escuro" : "Ativar modo claro");
            botao.setAttribute("title", paraEscuro ? "Modo escuro" : "Modo claro");
            // No menu do perfil o botão tem um texto visível; nas telas de login ele só tem ícone.
            const rotulo = document.getElementById("tema-rotulo");
            if (rotulo) rotulo.textContent = paraEscuro ? "Modo escuro" : "Modo claro";
        }
    }

    // Sem escolha salva, segue o tema do sistema operacional.
    aplicar(lerSalvo() || (sistemaEscuro.matches ? "escuro" : "claro"));

    sistemaEscuro.addEventListener("change", (e) => {
        if (!lerSalvo()) aplicar(e.matches ? "escuro" : "claro");
    });

    document.addEventListener("DOMContentLoaded", () => {
        aplicar(raiz.getAttribute("data-tema"));
        document.getElementById("botao-tema").addEventListener("click", () => {
            const novo = raiz.getAttribute("data-tema") === "escuro" ? "claro" : "escuro";
            salvar(novo);
            aplicar(novo);
        });
    });
})();
