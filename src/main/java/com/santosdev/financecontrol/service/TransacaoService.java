package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.dto.TransacaoRequest;
import com.santosdev.financecontrol.dto.TransacaoResponse;
import com.santosdev.financecontrol.exception.RecursoNaoEncontradoException;
import com.santosdev.financecontrol.exception.RegraDeNegocioException;
import com.santosdev.financecontrol.model.Categoria;
import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.model.Transacao;
import com.santosdev.financecontrol.repository.CategoriaRepository;
import com.santosdev.financecontrol.repository.TransacaoRepository;
import com.santosdev.financecontrol.repository.UsuarioRepository;
import com.santosdev.financecontrol.security.UsuarioAtual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransacaoService {

    private final TransacaoRepository transacoes;
    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;
    private final UsuarioAtual usuarioAtual;

    public TransacaoService(TransacaoRepository transacoes, CategoriaRepository categorias,
                            UsuarioRepository usuarios, UsuarioAtual usuarioAtual) {
        this.transacoes = transacoes;
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional(readOnly = true)
    public List<TransacaoResponse> listar(LocalDate inicio, LocalDate fim, Long categoriaId, TipoTransacao tipo) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new RegraDeNegocioException("A data inicial não pode ser depois da data final");
        }
        return transacoes.filtrar(usuarioAtual.id(), inicio, fim, categoriaId, tipo).stream().map(TransacaoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public TransacaoResponse buscar(Long id) {
        return TransacaoResponse.de(obter(id));
    }

    @Transactional
    public TransacaoResponse criar(TransacaoRequest req) {
        Transacao t = new Transacao();
        t.setUsuario(usuarios.getReferenceById(usuarioAtual.id()));
        preencher(t, req);
        return TransacaoResponse.de(transacoes.save(t));
    }

    @Transactional
    public TransacaoResponse atualizar(Long id, TransacaoRequest req) {
        Transacao t = obter(id);
        preencher(t, req);
        return TransacaoResponse.de(t);
    }

    @Transactional
    public void excluir(Long id) {
        transacoes.delete(obter(id));
    }

    private void preencher(Transacao t, TransacaoRequest req) {
        // A categoria tambem precisa ser do usuario logado.
        Categoria categoria = categorias.findByIdAndUsuarioId(req.categoriaId(), usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + req.categoriaId() + " não encontrada"));
        if (categoria.getTipo() != req.tipo()) {
            throw new RegraDeNegocioException("A categoria \"" + categoria.getNome() + "\" é de "
                    + categoria.getTipo() + " e não pode ser usada em uma " + req.tipo());
        }
        t.setDescricao(req.descricao().trim());
        t.setValor(req.valor());
        t.setData(req.data());
        t.setTipo(req.tipo());
        t.setCategoria(categoria);
    }

    // Movimentacao de outro usuario e tratada como inexistente (404).
    private Transacao obter(Long id) {
        return transacoes.buscarDoUsuario(id, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Movimentação " + id + " não encontrada"));
    }
}
