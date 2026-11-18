package com.santosdev.financecontrol.repository;

import com.santosdev.financecontrol.model.TipoToken;
import com.santosdev.financecontrol.model.TokenUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenUsuarioRepository extends JpaRepository<TokenUsuario, Long> {

    @Query("SELECT t FROM TokenUsuario t JOIN FETCH t.usuario WHERE t.tokenHash = :hash AND t.tipo = :tipo")
    Optional<TokenUsuario> buscarPorHash(@Param("hash") String hash, @Param("tipo") TipoToken tipo);

    Optional<TokenUsuario> findFirstByUsuarioIdAndTipoOrderByCriadoEmDesc(Long usuarioId, TipoToken tipo);

    @Modifying
    @Query("DELETE FROM TokenUsuario t WHERE t.usuario.id = :usuarioId AND t.tipo = :tipo")
    void apagarDoUsuario(@Param("usuarioId") Long usuarioId, @Param("tipo") TipoToken tipo);
}
