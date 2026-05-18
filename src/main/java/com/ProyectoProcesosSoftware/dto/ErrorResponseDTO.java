package com.ProyectoProcesosSoftware.dto;

import lombok.Data;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Respuesta de error estándar")
public class ErrorResponseDTO {
    @Schema(description = "Momento en que ocurrió el error", example = "2026-05-18T14:30:00")
    private LocalDateTime timestamp;
    @Schema(description = "Código de estado HTTP", example = "400")
    private int status;
    @Schema(description = "Mensaje de error", example = "Solicitud inválida")
    private String message;
}
