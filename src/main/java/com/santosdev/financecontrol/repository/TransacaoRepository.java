package com.santosdev.financecontrol.repository;

import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    boolean existsByCategoriaId(Long categoriaId);

    // Parametros nulos sao ignorados, entao todos os filtros sao opcionais.
    @Query("""
            SELECT t FROM Transacao t JOIN FETCH t.categoria c
            WHERE (:inicio IS NULL OR t.data >= :inicio)
              AND (:fim IS NULL OR t.data <= :fim)
              AND (:categoriaId IS NULL OR c.id = :categoriaId)
              AND (:tipo IS NULL OR t.tipo = :tipo)
            ORDER BY t.data DESC, t.id DESC
            """)
    List<Transacao> filtrar(@Param("inicio") LocalDate inicio,
                            @Param("fim") LocalDate fim,
                            @Param("categoriaId") Long categoriaId,
                            @Param("tipo") TipoTransacao tipo);

    // Dashboard: cada linha e [id da categoria, nome da categoria, soma dos valores], da maior para a menor.
    @Query("""
            SELECT c.id, c.nome, SUM(t.valor)
            FROM Transacao t JOIN t.categoria c
            WHERE t.tipo = :tipo
              AND (:inicio IS NULL OR t.data >= :inicio)
              AND (:fim IS NULL OR t.data <= :fim)
            GROUP BY c.id, c.nome
            ORDER BY SUM(t.valor) DESC, c.nome ASC
            """)
    List<Object[]> totaisPorCategoria(@Param("tipo") TipoTransacao tipo,
                                      @Param("inicio") LocalDate inicio,
                                      @Param("fim") LocalDate fim);

    // Dashboard: cada linha e [ano, mes, tipo, soma dos valores].
    @Query("""
            SELECT YEAR(t.data), MONTH(t.data), t.tipo, SUM(t.valor)
            FROM Transacao t
            WHERE (:inicio IS NULL OR t.data >= :inicio)
              AND (:fim IS NULL OR t.data <= :fim)
            GROUP BY YEAR(t.data), MONTH(t.data), t.tipo
            """)
    List<Object[]> totaisPorMes(@Param("inicio") LocalDate inicio,
                                @Param("fim") LocalDate fim);
}
