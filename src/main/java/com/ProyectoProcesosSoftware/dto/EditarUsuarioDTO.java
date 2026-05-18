package com.ProyectoProcesosSoftware.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Datos para actualizar el perfil de usuario (nombre y email obligatorios)")

public class EditarUsuarioDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Schema(description = "Nuevo nombre completo", example = "Alicia G. Martínez")

    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Schema(description = "Nuevo correo electrónico", example = "alicia.martinez@demo.com")
    private String email;
}