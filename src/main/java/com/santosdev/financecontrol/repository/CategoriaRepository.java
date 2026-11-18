package com.santosdev.financecontrol.repository;

import com.santosdev.financecontrol.model.Categoria;
import com.santosdev.financecontrol.model.TipoTransacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Todas as consultas filtram pelo dono: um usuario nunca enxerga as categorias de outro. */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByUsuarioIdOrderByNomeAsc(Long usuarioId);

    List<Categoria> findByUsuarioIdAndTipoOrderByNomeAsc(Long usuarioId, TipoTransacao tipo);

    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndNomeIgnoreCaseAndTipo(Long usuarioId, String nome, TipoTransacao tipo);
}
