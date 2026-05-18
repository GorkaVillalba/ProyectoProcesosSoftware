package com.ProyectoProcesosSoftware.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ForgotPasswordDTO {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;
}