package com.ProyectoProcesosSoftware.dto;

import lombok.Data;

@Data
public class JwtResponseDTO {
    private String token;
    private String tipo = "Bearer";
    private Long id;
    private String email;
    private String rol;
}
