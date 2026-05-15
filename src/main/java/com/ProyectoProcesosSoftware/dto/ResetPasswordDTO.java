package com.ProyectoProcesosSoftware.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ResetPasswordDTO {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String nuevaPassword;
}