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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Login y recuperación de contraseña")
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
    @Operation(
        summary = "Iniciar sesión",
        description = "Autentica al usuario con email y contraseña, y devuelve un token JWT junto con los datos básicos del usuario."
    )
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
        content = @Content(schema = @Schema(implementation = ValidationErrorResponseDTO.class)))
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

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Solicitar recuperación de contraseña",
        description = "Envía un correo con instrucciones para restablecer la contraseña si el email existe en el sistema."
    )
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
        content = @Content(schema = @Schema(implementation = ValidationErrorResponseDTO.class)))
    public ResponseEntity<MessageResponseDTO> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        passwordRecoveryService.generarTokenRecuperacion(dto.getEmail());
        return ResponseEntity.ok(new MessageResponseDTO(
                "Si el email está registrado, recibirás instrucciones para restablecer tu contraseña"));
    }

    @PostMapping("/reset-password")
        @Operation(
        summary = "Restablecer contraseña",
        description = "Cambia la contraseña del usuario usando un token de recuperación válido."
    )
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
        content = @Content(schema = @Schema(implementation = ValidationErrorResponseDTO.class)))
    public ResponseEntity<MessageResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        passwordRecoveryService.cambiarPassword(dto.getToken(), dto.getNuevaPassword());
        return ResponseEntity.ok(new MessageResponseDTO("Contraseña restablecida correctamente"));
    }
}
