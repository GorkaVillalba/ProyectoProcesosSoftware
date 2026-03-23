package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.*;
import com.ProyectoProcesosSoftware.model.Usuario;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import com.ProyectoProcesosSoftware.security.JwtService;
import com.ProyectoProcesosSoftware.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

// ═══════════════════════════════════════════════════════════════
// T-10 (Persona 4): Login endpoint
// T-27 (Persona 6): Password recovery endpoints (se añaden aquí)
// CREAR en controller/AuthController.java
// ═══════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElse(null);

        if (usuario == null || !passwordEncoder.matches(dto.getPassword(), usuario.getPassword())) {
            return ResponseEntity.status(401).body(new MessageResponseDTO("Credenciales incorrectas"));
        }

        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol().name());

        JwtResponseDTO response = new JwtResponseDTO();
        response.setToken(token);
        response.setId(usuario.getId());
        response.setEmail(usuario.getEmail());
        response.setRol(usuario.getRol().name());

        return ResponseEntity.ok(response);
    }

    // T-27 (Persona 6): Recuperación de contraseña
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDTO> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        passwordRecoveryService.generarTokenRecuperacion(dto.getEmail());
        return ResponseEntity.ok(new MessageResponseDTO(
                "Si el email está registrado, recibirás instrucciones para restablecer tu contraseña"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        passwordRecoveryService.cambiarPassword(dto.getToken(), dto.getNuevaPassword());
        return ResponseEntity.ok(new MessageResponseDTO("Contraseña restablecida correctamente"));
    }
}
