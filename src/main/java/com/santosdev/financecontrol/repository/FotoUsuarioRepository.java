package com.santosdev.financecontrol.repository;

import com.santosdev.financecontrol.model.FotoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface FotoUsuarioRepository extends JpaRepository<FotoUsuario, Long> {

    /** So a data, sem carregar a imagem: serve de "versao" para o navegador atualizar o cache. */
    @Query("SELECT f.atualizadaEm FROM FotoUsuario f WHERE f.usuarioId = :usuarioId")
    Optional<Instant> buscarAtualizadaEm(@Param("usuarioId") Long usuarioId);
}
