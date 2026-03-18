package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.model.Usuario;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Necesitaremos configurar esto en el siguiente paso

    public Usuario registrar(Usuario usuario) {
        // 1. Validar si el email ya existe
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new RuntimeException("Email ya registrado"); 
        }

        // 2. Hashear la contraseña (Seguridad)
        String passwordHasheada = passwordEncoder.encode(usuario.getPassword());
        usuario.setPassword(passwordHasheada);

        // 3. Guardar (la fecha se pone sola por el @PrePersist del modelo)
        return usuarioRepository.save(usuario);
    }
}