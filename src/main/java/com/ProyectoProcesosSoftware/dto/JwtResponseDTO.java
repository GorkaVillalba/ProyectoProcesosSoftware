package com.ProyectoProcesosSoftware.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Respuesta con el token JWT y datos básicos del usuario")
public class JwtResponseDTO {
    
    @Schema(description = "Token JWT para autenticar peticiones", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Tipo de token (siempre 'Bearer')", example = "Bearer")
    private String tipo = "Bearer";

    @Schema(description = "ID del usuario", example = "1")
    private Long id;

    @Schema(description = "Email del usuario", example = "alice@demo.com")
    private String email;

    @Schema(description = "Rol del usuario", example = "ASISTENTE", allowableValues = {"ASISTENTE", "ORGANIZADOR"})
    private String rol;

}
