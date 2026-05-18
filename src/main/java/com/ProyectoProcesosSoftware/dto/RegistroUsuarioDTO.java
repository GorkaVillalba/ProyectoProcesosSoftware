package com.ProyectoProcesosSoftware.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Datos para registrar un nuevo usuario")

public class RegistroUsuarioDTO {
    @NotBlank(message = "El nombre es obligatorio")
    @Schema(description = "Nombre completo del usuario", example = "Alicia García")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Schema(description = "Correo electrónico (debe ser único)", example = "alicia@demo.com")

    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Schema(description = "Contraseña (mínimo 6 caracteres)", example = "ClaveSegura1")

    private String password;

    @NotNull(message = "El rol es obligatorio")
    @Schema(description = "Rol del usuario: ASISTENTE u ORGANIZADOR", example = "ASISTENTE", allowableValues = {"ASISTENTE", "ORGANIZADOR"})

    private String rol;
}
