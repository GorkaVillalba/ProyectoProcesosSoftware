package com.ProyectoProcesosSoftware.dto;

import lombok.Data;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Datos del perfil de un usuario")
public class UsuarioResponseDTO {

    @Schema(description = "Identificador único del usuario", example = "1")
    private Long id;
    
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
    private String nombre;

    @Schema(description = "Correo electrónico", example = "alice@demo.com")
    private String email;

    @Schema(description = "Rol del usuario", example = "ASISTENTE", allowableValues = {"ASISTENTE", "ORGANIZADOR"})
    private String rol;

    @Schema(description = "Fecha y hora de registro en el sistema", example = "2026-01-15T10:30:00")
    private LocalDateTime fechaRegistro;
    
}
