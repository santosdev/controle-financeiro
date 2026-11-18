package com.santosdev.financecontrol.repository;

import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Todas as consultas filtram pelo dono: um usuario nunca enxerga as movimentacoes de outro. */
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    boolean existsByCategoriaIdAndUsuarioId(Long categoriaId, Long usuarioId);

    /** Usado ao excluir a conta. Precisa rodar antes de apagar as categorias, que as movimentacoes referenciam. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Transacao t WHERE t.usuario.id = :usuarioId")
    void apagarDoUsuario(@Param("usuarioId") Long usuarioId);

    @Query("SELECT t FROM Transacao t JOIN FETCH t.categoria WHERE t.id = :id AND t.usuario.id = :usuarioId")
    Optional<Transacao> buscarDoUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    // Parametros nulos sao ignorados, entao todos os filtros sao opcionais.
    // O CAST informa o tipo ao PostgreSQL, que nao deduz o tipo de um parametro usado so em "IS NULL".
    @Query("""
            SELECT t FROM Transacao t JOIN FETCH t.categoria c
            WHERE t.usuario.id = :usuarioId
              AND (CAST(:inicio AS LocalDate) IS NULL OR t.data >= :inicio)
              AND (CAST(:fim AS LocalDate) IS NULL OR t.data <= :fim)
              AND (CAST(:categoriaId AS Long) IS NULL OR c.id = :categoriaId)
              AND (:tipo IS NULL OR t.tipo = :tipo)
            ORDER BY t.data DESC, t.id DESC
            """)
    List<Transacao> filtrar(@Param("usuarioId") Long usuarioId,
                            @Param("inicio") LocalDate inicio,
                            @Param("fim") LocalDate fim,
                            @Param("categoriaId") Long categoriaId,
                            @Param("tipo") TipoTransacao tipo);

    // Dashboard: cada linha e [id da categoria, nome da categoria, soma dos valores], da maior para a menor.
    @Query("""
            SELECT c.id, c.nome, SUM(t.valor)
            FROM Transacao t JOIN t.categoria c
            WHERE t.usuario.id = :usuarioId
              AND t.tipo = :tipo
              AND (CAST(:inicio AS LocalDate) IS NULL OR t.data >= :inicio)
              AND (CAST(:fim AS LocalDate) IS NULL OR t.data <= :fim)
            GROUP BY c.id, c.nome
            ORDER BY SUM(t.valor) DESC, c.nome ASC
            """)
    List<Object[]> totaisPorCategoria(@Param("usuarioId") Long usuarioId,
                                      @Param("tipo") TipoTransacao tipo,
                                      @Param("inicio") LocalDate inicio,
                                      @Param("fim") LocalDate fim);

    // Dashboard: cada linha e [ano, mes, tipo, soma dos valores].
    @Query("""
            SELECT YEAR(t.data), MONTH(t.data), t.tipo, SUM(t.valor)
            FROM Transacao t
            WHERE t.usuario.id = :usuarioId
              AND (CAST(:inicio AS LocalDate) IS NULL OR t.data >= :inicio)
              AND (CAST(:fim AS LocalDate) IS NULL OR t.data <= :fim)
            GROUP BY YEAR(t.data), MONTH(t.data), t.tipo
            """)
    List<Object[]> totaisPorMes(@Param("usuarioId") Long usuarioId,
                                @Param("inicio") LocalDate inicio,
                                @Param("fim") LocalDate fim);
}
