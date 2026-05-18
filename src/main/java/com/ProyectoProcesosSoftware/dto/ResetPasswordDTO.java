package com.ProyectoProcesosSoftware.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Datos para restablecer la contraseña")
public class ResetPasswordDTO {

    @NotBlank(message = "El token es obligatorio")
    @Schema(description = "Token de recuperación enviado por correo", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Schema(description = "Nueva contraseña (mínimo 6 caracteres)", example = "NuevaClaveSegura1")
    private String nuevaPassword;
}