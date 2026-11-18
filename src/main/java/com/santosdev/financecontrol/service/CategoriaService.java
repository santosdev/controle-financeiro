package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.dto.CategoriaDTO;
import com.santosdev.financecontrol.exception.RecursoNaoEncontradoException;
import com.santosdev.financecontrol.exception.RegraDeNegocioException;
import com.santosdev.financecontrol.model.Categoria;
import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.repository.CategoriaRepository;
import com.santosdev.financecontrol.repository.TransacaoRepository;
import com.santosdev.financecontrol.repository.UsuarioRepository;
import com.santosdev.financecontrol.security.UsuarioAtual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final TransacaoRepository transacoes;
    private final UsuarioRepository usuarios;
    private final UsuarioAtual usuarioAtual;

    public CategoriaService(CategoriaRepository categorias, TransacaoRepository transacoes,
                            UsuarioRepository usuarios, UsuarioAtual usuarioAtual) {
        this.categorias = categorias;
        this.transacoes = transacoes;
        this.usuarios = usuarios;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listar(TipoTransacao tipo) {
        Long usuarioId = usuarioAtual.id();
        List<Categoria> lista = (tipo == null)
                ? categorias.findByUsuarioIdOrderByNomeAsc(usuarioId)
                : categorias.findByUsuarioIdAndTipoOrderByNomeAsc(usuarioId, tipo);
        return lista.stream().map(CategoriaDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaDTO buscar(Long id) {
        return CategoriaDTO.de(obter(id));
    }

    @Transactional
    public CategoriaDTO criar(CategoriaDTO dto) {
        Long usuarioId = usuarioAtual.id();
        String nome = dto.nome().trim();
        if (categorias.existsByUsuarioIdAndNomeIgnoreCaseAndTipo(usuarioId, nome, dto.tipo())) {
            throw new RegraDeNegocioException("Já existe uma categoria de " + dto.tipo() + " com esse nome");
        }
        Categoria nova = new Categoria(usuarios.getReferenceById(usuarioId), nome, dto.tipo());
        return CategoriaDTO.de(categorias.save(nova));
    }

    @Transactional
    public CategoriaDTO atualizar(Long id, CategoriaDTO dto) {
        Long usuarioId = usuarioAtual.id();
        Categoria categoria = obter(id);
        String nome = dto.nome().trim();
        boolean mudou = !categoria.getNome().equalsIgnoreCase(nome) || categoria.getTipo() != dto.tipo();
        if (mudou && categorias.existsByUsuarioIdAndNomeIgnoreCaseAndTipo(usuarioId, nome, dto.tipo())) {
            throw new RegraDeNegocioException("Já existe uma categoria de " + dto.tipo() + " com esse nome");
        }
        if (categoria.getTipo() != dto.tipo() && transacoes.existsByCategoriaIdAndUsuarioId(id, usuarioId)) {
            throw new RegraDeNegocioException("Não é possível mudar o tipo de uma categoria que já tem movimentações");
        }
        categoria.setNome(nome);
        categoria.setTipo(dto.tipo());
        return CategoriaDTO.de(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = obter(id);
        if (transacoes.existsByCategoriaIdAndUsuarioId(id, usuarioAtual.id())) {
            throw new RegraDeNegocioException("Não é possível excluir uma categoria que possui movimentações");
        }
        categorias.delete(categoria);
    }

    // Categoria de outro usuario e tratada como inexistente (404), sem revelar que o ID existe.
    private Categoria obter(Long id) {
        return categorias.findByIdAndUsuarioId(id, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + id + " não encontrada"));
    }
}
