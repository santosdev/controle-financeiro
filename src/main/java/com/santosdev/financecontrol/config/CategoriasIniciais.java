package com.santosdev.financecontrol.config;

import com.santosdev.financecontrol.model.Categoria;
import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.repository.CategoriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cadastra categorias padrao na primeira execucao, somente se a tabela estiver vazia.
 */
@Configuration
public class CategoriasIniciais {

    @Bean
    CommandLineRunner carregarCategorias(CategoriaRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.saveAll(List.of(
                    new Categoria("Salário", TipoTransacao.RECEITA),
                    new Categoria("Freelance", TipoTransacao.RECEITA),
                    new Categoria("Investimentos", TipoTransacao.RECEITA),
                    new Categoria("Outras receitas", TipoTransacao.RECEITA),
                    new Categoria("Alimentação", TipoTransacao.DESPESA),
                    new Categoria("Moradia", TipoTransacao.DESPESA),
                    new Categoria("Transporte", TipoTransacao.DESPESA),
                    new Categoria("Saúde", TipoTransacao.DESPESA),
                    new Categoria("Educação", TipoTransacao.DESPESA),
                    new Categoria("Lazer", TipoTransacao.DESPESA),
                    new Categoria("Outras despesas", TipoTransacao.DESPESA)));
        };
    }
}
