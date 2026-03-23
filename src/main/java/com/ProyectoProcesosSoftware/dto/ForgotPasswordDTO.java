package com.ProyectoProcesosSoftware.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ForgotPasswordDTO {
    @NotBlank @Email
    private String email;
}
