package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.model.Categoria;
import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.model.Usuario;
import com.santosdev.financecontrol.repository.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Cada usuario novo ja comeca com um conjunto de categorias, que ele pode editar ou apagar. */
@Service
public class CategoriasPadraoService {

    private final CategoriaRepository categorias;

    public CategoriasPadraoService(CategoriaRepository categorias) {
        this.categorias = categorias;
    }

    public void criarPara(Usuario usuario) {
        categorias.saveAll(List.of(
                new Categoria(usuario, "Salário", TipoTransacao.RECEITA),
                new Categoria(usuario, "Freelance", TipoTransacao.RECEITA),
                new Categoria(usuario, "Investimentos", TipoTransacao.RECEITA),
                new Categoria(usuario, "Outras receitas", TipoTransacao.RECEITA),
                new Categoria(usuario, "Alimentação", TipoTransacao.DESPESA),
                new Categoria(usuario, "Moradia", TipoTransacao.DESPESA),
                new Categoria(usuario, "Transporte", TipoTransacao.DESPESA),
                new Categoria(usuario, "Saúde", TipoTransacao.DESPESA),
                new Categoria(usuario, "Educação", TipoTransacao.DESPESA),
                new Categoria(usuario, "Lazer", TipoTransacao.DESPESA),
                new Categoria(usuario, "Outras despesas", TipoTransacao.DESPESA)));
    }
}
