package com.ProyectoProcesosSoftware.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @NotBlank private String token;
    @NotBlank @Size(min = 6) private String nuevaPassword;
}
