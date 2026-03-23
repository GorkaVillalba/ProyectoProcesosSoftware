package com.ProyectoProcesosSoftware.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EditarUsuarioDTO {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotBlank @Email
    private String email;
}
