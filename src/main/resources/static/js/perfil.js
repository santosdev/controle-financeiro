// Perfil do usuário: menu do topo, foto, apelido, troca de senha e exclusão da conta.
// Usa funções de app.js ($, mostrarAviso, mostrarErroForm), que já estão carregadas quando isto roda.

const Perfil = (() => {
    const TIPOS_ACEITOS = /^image\/(jpeg|png|gif)$/;
    const TAMANHO_MAXIMO_ORIGINAL = 20 * 1024 * 1024; // o arquivo é reduzido no navegador antes de enviar
    const LADO_ENVIADO = 512;

    let usuario = null;

    /* ---------------- Avatar ---------------- */

    function inicial(u) {
        const texto = (u.nomeExibicao || u.nome || "?").trim();
        return (texto.charAt(0) || "?").toUpperCase();
    }

    // Foto, se houver; senão a inicial do nome. O ?v= muda a cada troca e força o navegador a recarregar.
    function pintarAvatar(elemento) {
        elemento.replaceChildren();
        if (usuario.fotoVersao) {
            const img = document.createElement("img");
            img.alt = "";
            img.src = "/api/perfil/foto?v=" + usuario.fotoVersao;
            img.addEventListener("error", () => elemento.replaceChildren(document.createTextNode(inicial(usuario))), { once: true });
            elemento.appendChild(img);
        } else {
            elemento.textContent = inicial(usuario);
        }
    }

    function aplicar(novo) {
        usuario = novo;
        $("usuario-nome").textContent = novo.nomeExibicao;
        $("menu-nome").textContent = novo.nomeExibicao;
        $("menu-email").textContent = novo.email;
        $("menu-email").title = novo.email;
        pintarAvatar($("avatar-topo"));
        pintarAvatar($("avatar-perfil"));
        $("foto-remover").hidden = !novo.fotoVersao;
        $("senha-google").hidden = novo.temSenha;
        $("campos-senha").hidden = !novo.temSenha;

        // Visitante: sem e-mail real e sem senha; a faixa no topo lembra que a conta é temporária.
        if (novo.visitante) {
            $("menu-email").textContent = "Conta de visitante";
            $("menu-email").title = "";
            $("form-senha").hidden = true;
            $("faixa-visitante").hidden = false;
        }
    }

    function preencherFormulario() {
        $("perfil-apelido").value = usuario.apelido || "";
        $("perfil-nome").value = usuario.nome;
        $("perfil-email").value = usuario.visitante ? "Conta de visitante (sem e-mail)" : usuario.email;
    }

    function esconderErro(id) {
        $(id).hidden = true;
    }

    /* ---------------- Menu do topo ---------------- */

    function menuAberto() {
        return !$("perfil-menu").hidden;
    }

    function abrirMenu() {
        $("perfil-menu").hidden = false;
        $("perfil-botao").setAttribute("aria-expanded", "true");
    }

    function fecharMenu() {
        $("perfil-menu").hidden = true;
        $("perfil-botao").setAttribute("aria-expanded", "false");
    }

    function ligarMenu() {
        $("perfil-botao").addEventListener("click", () => (menuAberto() ? fecharMenu() : abrirMenu()));

        document.addEventListener("click", (evento) => {
            if (menuAberto() && !$("perfil").contains(evento.target)) fecharMenu();
        });
        document.addEventListener("keydown", (evento) => {
            if (evento.key === "Escape" && menuAberto()) {
                fecharMenu();
                $("perfil-botao").focus();
            }
        });
        // Qualquer item do menu (Meu perfil, tema, Sair) fecha o menu depois de agir.
        $("perfil-menu").querySelectorAll("[role=menuitem]").forEach((item) => item.addEventListener("click", fecharMenu));

        $("menu-meu-perfil").addEventListener("click", abrirPagina);
    }

    function abrirPagina() {
        document.querySelectorAll(".aba").forEach((aba) => aba.classList.remove("ativa"));
        document.querySelectorAll(".painel").forEach((painel) => (painel.hidden = painel.id !== "aba-perfil"));
        ["erro-perfil", "erro-foto", "erro-senha"].forEach(esconderErro);
        preencherFormulario();
        window.scrollTo({ top: 0 });
    }

    /* ---------------- Foto ---------------- */

    function lerArquivo(arquivo) {
        return new Promise((resolve, reject) => {
            const leitor = new FileReader();
            leitor.onload = () => resolve(leitor.result);
            leitor.onerror = () => reject(new Error("Não foi possível ler o arquivo."));
            leitor.readAsDataURL(arquivo);
        });
    }

    function carregarImagem(endereco) {
        return new Promise((resolve, reject) => {
            const imagem = new Image();
            imagem.onload = () => resolve(imagem);
            imagem.onerror = () => reject(new Error("O arquivo não é uma imagem válida. Use JPG, PNG ou GIF."));
            imagem.src = endereco;
        });
    }

    // Recorta o centro em quadrado e reduz, para fotos grandes de celular caberem no limite de 2 MB.
    async function prepararFoto(arquivo) {
        if (!TIPOS_ACEITOS.test(arquivo.type)) {
            throw new Error("Use uma imagem JPG, PNG ou GIF.");
        }
        if (arquivo.size > TAMANHO_MAXIMO_ORIGINAL) {
            throw new Error("A foto deve ter no máximo 20 MB.");
        }
        const imagem = await carregarImagem(await lerArquivo(arquivo));
        const lado = Math.min(imagem.naturalWidth, imagem.naturalHeight);
        const destino = Math.min(lado, LADO_ENVIADO);
        const quadro = document.createElement("canvas");
        quadro.width = destino;
        quadro.height = destino;
        const contexto = quadro.getContext("2d");
        contexto.fillStyle = "#ffffff";
        contexto.fillRect(0, 0, destino, destino);
        contexto.drawImage(imagem,
            (imagem.naturalWidth - lado) / 2, (imagem.naturalHeight - lado) / 2, lado, lado,
            0, 0, destino, destino);
        return new Promise((resolve, reject) =>
            quadro.toBlob((blob) => (blob ? resolve(blob) : reject(new Error("Não foi possível preparar a imagem."))),
                "image/jpeg", 0.9));
    }

    function ligarFoto() {
        $("foto-enviar").addEventListener("click", () => $("foto-arquivo").click());

        $("foto-arquivo").addEventListener("change", async (evento) => {
            const arquivo = evento.target.files[0];
            evento.target.value = ""; // permite escolher o mesmo arquivo de novo
            if (!arquivo) return;
            esconderErro("erro-foto");
            try {
                aplicar(await API.enviarFoto(await prepararFoto(arquivo)));
                mostrarAviso("Foto atualizada.");
            } catch (e) {
                mostrarErroForm("erro-foto", e.message);
            }
        });

        $("foto-remover").addEventListener("click", async () => {
            esconderErro("erro-foto");
            try {
                aplicar(await API.removerFoto());
                mostrarAviso("Foto removida.");
            } catch (e) {
                mostrarErroForm("erro-foto", e.message);
            }
        });
    }

    /* ---------------- Dados e senha ---------------- */

    function ligarDados() {
        $("form-perfil").addEventListener("submit", async (evento) => {
            evento.preventDefault();
            esconderErro("erro-perfil");
            const nome = $("perfil-nome").value.trim();
            if (!nome) {
                mostrarErroForm("erro-perfil", "O nome é obrigatório");
                return;
            }
            try {
                aplicar(await API.atualizarPerfil({ nome, apelido: $("perfil-apelido").value }));
                preencherFormulario();
                mostrarAviso("Perfil atualizado.");
            } catch (e) {
                mostrarErroForm("erro-perfil", e.message);
            }
        });
    }

    function ligarSenha() {
        $("form-senha").addEventListener("submit", async (evento) => {
            evento.preventDefault();
            esconderErro("erro-senha");
            const senhaAtual = $("senha-atual").value;
            const novaSenha = $("senha-nova").value;
            if (!senhaAtual || !novaSenha) {
                mostrarErroForm("erro-senha", "Preencha a senha atual e a nova senha");
                return;
            }
            if (novaSenha.length < 8 || !/[A-Za-z]/.test(novaSenha) || !/\d/.test(novaSenha)) {
                mostrarErroForm("erro-senha", "A senha deve ter de 8 a 64 caracteres, com pelo menos uma letra e um número");
                return;
            }
            try {
                await API.alterarSenha({ senhaAtual, novaSenha });
                $("form-senha").reset();
                mostrarAviso("Senha alterada com sucesso.");
            } catch (e) {
                mostrarErroForm("erro-senha", e.message);
            }
        });
    }

    /* ---------------- Excluir conta ---------------- */

    function ligarExclusao() {
        const modal = $("modal-excluir-conta");

        $("abrir-excluir-conta").addEventListener("click", () => {
            // Quem tem senha confirma com a senha; quem só entra pelo Google, digitando o e-mail; o visitante, digitando EXCLUIR.
            $("excluir-conta-rotulo").textContent = usuario.visitante ? "Digite EXCLUIR para confirmar"
                : usuario.temSenha ? "Digite sua senha para confirmar" : "Digite seu e-mail para confirmar";
            $("excluir-conta-confirmacao").type = usuario.temSenha ? "password" : "text";
            $("excluir-conta-confirmacao").value = "";
            esconderErro("erro-excluir-conta");
            modal.showModal();
            $("excluir-conta-confirmacao").focus();
        });

        modal.addEventListener("close", () => ($("excluir-conta-confirmacao").value = ""));

        $("form-excluir-conta").addEventListener("submit", async (evento) => {
            evento.preventDefault();
            esconderErro("erro-excluir-conta");
            const confirmacao = $("excluir-conta-confirmacao").value;
            if (!confirmacao) {
                mostrarErroForm("erro-excluir-conta", "Informe a confirmação");
                return;
            }
            try {
                await API.excluirConta(confirmacao);
                window.location.href = "/login.html";
            } catch (e) {
                mostrarErroForm("erro-excluir-conta", e.message);
            }
        });
    }

    function iniciar(dadosDoUsuario) {
        usuario = dadosDoUsuario;
        ligarMenu();
        ligarFoto();
        ligarDados();
        ligarSenha();
        ligarExclusao();
        aplicar(dadosDoUsuario);
    }

    return { iniciar };
})();
