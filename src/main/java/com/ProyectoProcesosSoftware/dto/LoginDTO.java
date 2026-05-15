package com.ProyectoProcesosSoftware.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}