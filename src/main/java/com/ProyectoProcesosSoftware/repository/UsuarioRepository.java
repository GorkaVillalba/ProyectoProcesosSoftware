package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Método que busca si el email existe
    boolean existsByEmail(String email);
}