package com.santosdev.financecontrol.repository;

import com.santosdev.financecontrol.model.ProvedorLogin;
import com.santosdev.financecontrol.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    long countByProvedor(ProvedorLogin provedor);

    @Query("SELECT u.id FROM Usuario u WHERE u.provedor = :provedor AND u.criadoEm < :limite")
    List<Long> idsCriadosAntesDe(@Param("provedor") ProvedorLogin provedor, @Param("limite") Instant limite);
}
