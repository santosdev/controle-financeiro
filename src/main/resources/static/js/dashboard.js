// Dashboard: indicadores e gráficos. Os gráficos são HTML/CSS puro (sem biblioteca),
// então herdam as cores do tema claro/escuro direto das variáveis do style.css.
// Usa $, el, moeda e mostrarAviso definidos em app.js (chamados só depois que a página carrega).

const Dashboard = (() => {
    const MESES_CURTOS = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];
    const MESES_LONGOS = ["janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"];
    const MAX_CATEGORIAS = 8; // além disso, o restante vira "Outras"
    const ALTURA_PLOT = 240;

    let dados = null;
    let tipoCategorias = "DESPESA";
    let numeroRequisicao = 0;

    /* ---------------- Datas e período ---------------- */

    const doisDigitos = (n) => String(n).padStart(2, "0");
    const iso = (d) => `${d.getFullYear()}-${doisDigitos(d.getMonth() + 1)}-${doisDigitos(d.getDate())}`;
    const primeiroDia = (ano, mes) => new Date(ano, mes, 1);
    const ultimoDia = (ano, mes) => new Date(ano, mes + 1, 0);

    function periodoSelecionado() {
        const hoje = new Date();
        const ano = hoje.getFullYear();
        const mes = hoje.getMonth();
        switch ($("dash-periodo").value) {
            case "mes-atual": return { inicio: iso(primeiroDia(ano, mes)), fim: iso(ultimoDia(ano, mes)) };
            case "mes-anterior": return { inicio: iso(primeiroDia(ano, mes - 1)), fim: iso(ultimoDia(ano, mes - 1)) };
            case "3-meses": return { inicio: iso(primeiroDia(ano, mes - 2)), fim: iso(ultimoDia(ano, mes)) };
            case "6-meses": return { inicio: iso(primeiroDia(ano, mes - 5)), fim: iso(ultimoDia(ano, mes)) };
            case "ano": return { inicio: iso(primeiroDia(ano, 0)), fim: iso(ultimoDia(ano, 11)) };
            case "personalizado": return { inicio: $("dash-inicio").value, fim: $("dash-fim").value };
            default: return { inicio: "", fim: "" };
        }
    }

    function rotuloMes(aaaaMm) {
        const [ano, mes] = aaaaMm.split("-").map(Number);
        return `${MESES_CURTOS[mes - 1]}/${String(ano).slice(2)}`;
    }

    function nomeMes(aaaaMm) {
        const [ano, mes] = aaaaMm.split("-").map(Number);
        return `${MESES_LONGOS[mes - 1]} de ${ano}`;
    }

    /* ---------------- Escala dos eixos ---------------- */

    // Escolhe um passo "redondo" (1, 2, 5 x 10^n) para o eixo vertical.
    function escala(maximo) {
        const alvo = maximo / 4;
        const base = Math.pow(10, Math.floor(Math.log10(alvo)));
        const normal = alvo / base;
        const passo = (normal <= 1 ? 1 : normal <= 2 ? 2 : normal <= 5 ? 5 : 10) * base;
        const topo = Math.ceil(maximo / passo) * passo;
        const marcas = [];
        for (let v = 0; v <= topo + passo / 2; v += passo) marcas.push(v);
        return { topo, marcas };
    }

    const formatoInteiro = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 0 });
    const formatoCompacto = new Intl.NumberFormat("pt-BR", { notation: "compact", maximumFractionDigits: 1 });
    const rotuloEixo = (v) => (v < 10000 ? formatoInteiro.format(v) : formatoCompacto.format(v));

    /* ---------------- Dica (tooltip) ---------------- */

    // Mostra os mesmos detalhes no mouse e no teclado (foco). O conteúdo é sempre inserido com textContent.
    function ligarDica(elemento, titulo, linhas) {
        const dica = $("dica-grafico");

        function montar() {
            dica.replaceChildren(el("div", { classe: "dica__titulo", texto: titulo }));
            linhas.forEach((l) => {
                const chave = el("i", { classe: "chave chave--" + l.serie });
                dica.appendChild(el("div", { classe: "dica__linha" }, [
                    chave,
                    el("strong", { texto: l.valor }),
                    el("span", { texto: l.nome })
                ]));
            });
        }

        function posicionar(x, y) {
            dica.hidden = false;
            const caixa = dica.getBoundingClientRect();
            const esquerda = Math.min(Math.max(8, x + 14), window.innerWidth - caixa.width - 8);
            const topo = y + 18 + caixa.height > window.innerHeight ? y - caixa.height - 14 : y + 18;
            dica.style.left = esquerda + "px";
            dica.style.top = Math.max(8, topo) + "px";
        }

        elemento.addEventListener("pointerenter", (e) => { montar(); posicionar(e.clientX, e.clientY); });
        elemento.addEventListener("pointermove", (e) => posicionar(e.clientX, e.clientY));
        elemento.addEventListener("pointerleave", () => { dica.hidden = true; });
        elemento.addEventListener("focus", () => {
            montar();
            const r = elemento.getBoundingClientRect();
            posicionar(r.left + r.width / 2, r.top);
        });
        elemento.addEventListener("blur", () => { dica.hidden = true; });
    }

    /* ---------------- Indicadores (KPIs) ---------------- */

    function renderizarIndicadores() {
        const { totalReceitas, totalDespesas, saldo } = dados;
        $("dash-saldo").textContent = moeda.format(saldo);
        $("dash-receitas").textContent = moeda.format(totalReceitas);
        $("dash-despesas").textContent = moeda.format(totalDespesas);

        const nota = $("dash-saldo-nota");
        nota.textContent = saldo < 0 ? "▼ Gastou mais do que recebeu" : saldo > 0 ? "▲ Recebeu mais do que gastou" : "";
        nota.classList.toggle("kpi__nota--negativa", saldo < 0);

        const poupanca = $("dash-poupanca");
        const notaPoupanca = $("dash-poupanca-nota");
        if (totalReceitas > 0) {
            const taxa = (saldo / totalReceitas) * 100;
            poupanca.textContent = taxa.toLocaleString("pt-BR", { maximumFractionDigits: 1 }) + "%";
            notaPoupanca.textContent = "da receita do período";
        } else {
            poupanca.textContent = "—";
            notaPoupanca.textContent = "sem receitas no período";
        }
    }

    /* ---------------- Gráfico de colunas: receitas x despesas por mês ---------------- */

    function renderizarMensal() {
        const area = $("grafico-mensal");
        area.replaceChildren();

        const meses = dados.evolucaoMensal;
        const maximo = Math.max(0, ...meses.flatMap((m) => [m.receitas, m.despesas]));
        if (meses.length === 0 || maximo === 0) {
            area.appendChild(el("p", { classe: "vazio", texto: "Sem movimentações no período." }));
            return;
        }

        const { topo, marcas } = escala(maximo);
        const pct = (v) => (v / topo) * 100;

        // Eixo vertical e linhas de grade (hairline, uma por marca)
        const eixo = el("div", { classe: "g-eixo" });
        const plot = el("div", { classe: "g-plot" });
        marcas.forEach((v) => {
            eixo.appendChild(el("span", { classe: "g-marca", texto: rotuloEixo(v), style: `bottom:${pct(v)}%` }));
            if (v > 0) plot.appendChild(el("div", { classe: "g-grade", style: `bottom:${pct(v)}%` }));
        });

        // Um grupo por mês, com a coluna de receita e a de despesa lado a lado
        const grupos = el("div", { classe: "g-grupos" });
        const passoRotulo = Math.ceil(meses.length / 12);
        const rotulos = el("div", { classe: "g-rotulos" });

        meses.forEach((m, i) => {
            const grupo = el("div", {
                classe: "g-grupo", tabindex: "0", role: "img",
                "aria-label": `${nomeMes(m.mes)}: receitas ${moeda.format(m.receitas)}, despesas ${moeda.format(m.despesas)}`
            }, [
                el("div", { classe: "g-coluna g-coluna--receita", style: `height:${pct(m.receitas)}%` }),
                el("div", { classe: "g-coluna g-coluna--despesa", style: `height:${pct(m.despesas)}%` })
            ]);
            ligarDica(grupo, nomeMes(m.mes), [
                { serie: "receita", valor: moeda.format(m.receitas), nome: "Receitas" },
                { serie: "despesa", valor: moeda.format(m.despesas), nome: "Despesas" },
                { serie: "saldo", valor: moeda.format(m.receitas - m.despesas), nome: "Saldo do mês" }
            ]);
            grupos.appendChild(grupo);
            rotulos.appendChild(el("span", { classe: "g-rotulo", texto: i % passoRotulo === 0 ? rotuloMes(m.mes) : "" }));
        });

        plot.appendChild(grupos);
        area.appendChild(el("div", { classe: "g-colunas", style: `--altura:${ALTURA_PLOT}px` }, [
            eixo, plot, el("span"), rotulos
        ]));
    }

    /* ---------------- Barras ranqueadas: totais por categoria ---------------- */

    // Mantém as maiores categorias e junta o restante em "Outras".
    function prepararCategorias(lista) {
        if (lista.length <= MAX_CATEGORIAS) return lista.map((c) => ({ ...c, outras: false }));
        const principais = lista.slice(0, MAX_CATEGORIAS - 1).map((c) => ({ ...c, outras: false }));
        const resto = lista.slice(MAX_CATEGORIAS - 1);
        principais.push({
            categoriaId: null,
            categoria: `Outras (${resto.length} categorias)`,
            total: resto.reduce((soma, c) => soma + c.total, 0),
            outras: true
        });
        return principais;
    }

    function renderizarCategorias() {
        const despesa = tipoCategorias === "DESPESA";
        const lista = despesa ? dados.despesasPorCategoria : dados.receitasPorCategoria;
        const totalTipo = despesa ? dados.totalDespesas : dados.totalReceitas;
        const serie = despesa ? "despesa" : "receita";

        $("titulo-categorias").textContent = despesa ? "Despesas por categoria" : "Receitas por categoria";
        $("sub-categorias").textContent = lista.length
            ? `Total: ${moeda.format(totalTipo)}`
            : "";

        const area = $("grafico-categorias");
        area.replaceChildren();
        if (lista.length === 0) {
            area.appendChild(el("p", { classe: "vazio", texto: despesa ? "Sem despesas no período." : "Sem receitas no período." }));
            return;
        }

        const linhas = prepararCategorias(lista);
        const maior = Math.max(...linhas.map((l) => l.total));

        const container = el("div", { classe: "g-ranking" });
        linhas.forEach((c) => {
            const participacao = (c.total / totalTipo) * 100;
            const textoParticipacao = participacao.toLocaleString("pt-BR", { maximumFractionDigits: 1 }) + "%";
            const barra = el("div", {
                classe: "g-barra-h " + (c.outras ? "g-barra-h--outras" : "g-barra-h--" + serie),
                style: `width:${(c.total / maior) * 100}%`
            });
            const linha = el("div", { classe: "g-linha", tabindex: "0" }, [
                el("span", { classe: "g-nome", texto: c.categoria, title: c.categoria }),
                el("div", { classe: "g-trilho" }, [barra]),
                el("span", { classe: "g-valor" }, [
                    el("strong", { texto: moeda.format(c.total) }),
                    el("span", { texto: textoParticipacao })
                ])
            ]);
            ligarDica(linha, c.categoria, [
                { serie: c.outras ? "outras" : serie, valor: moeda.format(c.total), nome: despesa ? "Despesas" : "Receitas" },
                { serie: "saldo", valor: textoParticipacao, nome: "do total" }
            ]);
            container.appendChild(linha);
        });
        area.appendChild(container);
    }

    /* ---------------- Visão em tabela (alternativa acessível aos gráficos) ---------------- */

    function tabela(titulo, colunas, linhas) {
        const cabecalho = el("tr", {}, colunas.map((c, i) => el("th", { texto: c, classe: i > 0 ? "direita" : "" })));
        const corpo = linhas.map((l) => el("tr", {}, l.map((v, i) => el("td", { texto: v, classe: i > 0 ? "direita" : "" }))));
        return el("div", { classe: "tabela-wrap" }, [
            el("table", { classe: "tabela tabela--compacta" }, [
                el("caption", { texto: titulo }),
                el("thead", {}, [cabecalho]),
                el("tbody", {}, corpo)
            ])
        ]);
    }

    function renderizarTabelas() {
        const area = $("dash-tabelas");
        area.replaceChildren();

        if (dados.evolucaoMensal.length) {
            area.appendChild(tabela("Receitas e despesas por mês", ["Mês", "Receitas", "Despesas", "Saldo"],
                dados.evolucaoMensal.map((m) => [
                    nomeMes(m.mes), moeda.format(m.receitas), moeda.format(m.despesas), moeda.format(m.receitas - m.despesas)
                ])));
        }

        const porCategoria = [
            ...dados.despesasPorCategoria.map((c) => ["Despesa", c.categoria, c.total, dados.totalDespesas]),
            ...dados.receitasPorCategoria.map((c) => ["Receita", c.categoria, c.total, dados.totalReceitas])
        ];
        if (porCategoria.length) {
            area.appendChild(tabela("Totais por categoria", ["Tipo", "Categoria", "Total", "% do tipo"],
                porCategoria.map(([tipo, nome, total, base]) => [
                    tipo, nome, moeda.format(total),
                    ((total / base) * 100).toLocaleString("pt-BR", { maximumFractionDigits: 1 }) + "%"
                ])));
        }
        if (!area.children.length) {
            area.appendChild(el("p", { classe: "vazio", texto: "Sem movimentações no período." }));
        }
    }

    /* ---------------- Carregamento ---------------- */

    async function carregar() {
        const periodo = periodoSelecionado();
        if ($("dash-periodo").value === "personalizado" && (!periodo.inicio || !periodo.fim)) {
            return; // espera as duas datas
        }

        const conteudo = $("dash-conteudo");
        conteudo.classList.add("carregando"); // mantém o desenho anterior, só mais claro
        const minha = ++numeroRequisicao;
        try {
            const resposta = await API.dashboard(periodo);
            if (minha !== numeroRequisicao) return; // chegou uma resposta mais nova
            dados = resposta;
            renderizarIndicadores();
            renderizarMensal();
            renderizarCategorias();
            renderizarTabelas();
        } catch (e) {
            mostrarAviso(e.message, true);
        } finally {
            if (minha === numeroRequisicao) conteudo.classList.remove("carregando");
        }
    }

    function trocarTipo(tipo) {
        tipoCategorias = tipo;
        $("alt-despesa").classList.toggle("ativo", tipo === "DESPESA");
        $("alt-despesa").setAttribute("aria-pressed", String(tipo === "DESPESA"));
        $("alt-receita").classList.toggle("ativo", tipo === "RECEITA");
        $("alt-receita").setAttribute("aria-pressed", String(tipo === "RECEITA"));
        if (dados) renderizarCategorias();
    }

    function iniciar() {
        $("dash-periodo").addEventListener("change", () => {
            const personalizado = $("dash-periodo").value === "personalizado";
            document.querySelectorAll(".dash-personalizado").forEach((e) => (e.hidden = !personalizado));
            carregar();
        });
        $("dash-inicio").addEventListener("change", carregar);
        $("dash-fim").addEventListener("change", carregar);
        $("alt-despesa").addEventListener("click", () => trocarTipo("DESPESA"));
        $("alt-receita").addEventListener("click", () => trocarTipo("RECEITA"));
        return carregar();
    }

    return { iniciar, carregar };
})();
