package com.ProyectoProcesosSoftware.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Solicitud de recuperación de contraseña")
public class ForgotPasswordDTO {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Schema(description = "Correo electrónico registrado", example = "alice@demo.com")

    private String email;
}