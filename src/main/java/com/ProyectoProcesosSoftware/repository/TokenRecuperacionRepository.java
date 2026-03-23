package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {
    Optional<TokenRecuperacion> findByToken(String token);
    void deleteByUsuarioId(Long usuarioId);
}
