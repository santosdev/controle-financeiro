package com.santosdev.financecontrol.repository;

import com.santosdev.financecontrol.model.Categoria;
import com.santosdev.financecontrol.model.TipoTransacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Todas as consultas filtram pelo dono: um usuario nunca enxerga as categorias de outro. */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByUsuarioIdOrderByNomeAsc(Long usuarioId);

    List<Categoria> findByUsuarioIdAndTipoOrderByNomeAsc(Long usuarioId, TipoTransacao tipo);

    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndNomeIgnoreCaseAndTipo(Long usuarioId, String nome, TipoTransacao tipo);

    /** Usado ao excluir a conta, depois de apagar as movimentacoes do usuario. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Categoria c WHERE c.usuario.id = :usuarioId")
    void apagarDoUsuario(@Param("usuarioId") Long usuarioId);
}
