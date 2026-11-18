package com.santosdev.financecontrol.repository;

import com.santosdev.financecontrol.model.Categoria;
import com.santosdev.financecontrol.model.TipoTransacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByTipoOrderByNomeAsc(TipoTransacao tipo);

    List<Categoria> findAllByOrderByNomeAsc();

    boolean existsByNomeIgnoreCaseAndTipo(String nome, TipoTransacao tipo);
}
