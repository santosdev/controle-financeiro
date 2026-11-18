package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.dto.CategoriaDTO;
import com.santosdev.financecontrol.exception.RecursoNaoEncontradoException;
import com.santosdev.financecontrol.exception.RegraDeNegocioException;
import com.santosdev.financecontrol.model.Categoria;
import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.repository.CategoriaRepository;
import com.santosdev.financecontrol.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final TransacaoRepository transacoes;

    public CategoriaService(CategoriaRepository categorias, TransacaoRepository transacoes) {
        this.categorias = categorias;
        this.transacoes = transacoes;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listar(TipoTransacao tipo) {
        List<Categoria> lista = (tipo == null)
                ? categorias.findAllByOrderByNomeAsc()
                : categorias.findByTipoOrderByNomeAsc(tipo);
        return lista.stream().map(CategoriaDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaDTO buscar(Long id) {
        return CategoriaDTO.de(obter(id));
    }

    @Transactional
    public CategoriaDTO criar(CategoriaDTO dto) {
        String nome = dto.nome().trim();
        if (categorias.existsByNomeIgnoreCaseAndTipo(nome, dto.tipo())) {
            throw new RegraDeNegocioException("Já existe uma categoria de " + dto.tipo() + " com esse nome");
        }
        return CategoriaDTO.de(categorias.save(new Categoria(nome, dto.tipo())));
    }

    @Transactional
    public CategoriaDTO atualizar(Long id, CategoriaDTO dto) {
        Categoria categoria = obter(id);
        String nome = dto.nome().trim();
        boolean mudou = !categoria.getNome().equalsIgnoreCase(nome) || categoria.getTipo() != dto.tipo();
        if (mudou && categorias.existsByNomeIgnoreCaseAndTipo(nome, dto.tipo())) {
            throw new RegraDeNegocioException("Já existe uma categoria de " + dto.tipo() + " com esse nome");
        }
        if (categoria.getTipo() != dto.tipo() && transacoes.existsByCategoriaId(id)) {
            throw new RegraDeNegocioException("Não é possível mudar o tipo de uma categoria que já tem movimentações");
        }
        categoria.setNome(nome);
        categoria.setTipo(dto.tipo());
        return CategoriaDTO.de(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = obter(id);
        if (transacoes.existsByCategoriaId(id)) {
            throw new RegraDeNegocioException("Não é possível excluir uma categoria que possui movimentações");
        }
        categorias.delete(categoria);
    }

    private Categoria obter(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + id + " não encontrada"));
    }
}
