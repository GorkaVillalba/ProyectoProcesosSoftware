package com.ProyectoProcesosSoftware.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;


@Data
@Schema(description = "Errores de validación de los datos de entrada")
public class ValidationErrorResponseDTO {
    @Schema(description = "Momento en que ocurrió el error", example = "2026-05-18T14:30:00")
    private LocalDateTime timestamp;
    @Schema(description = "Código de estado HTTP", example = "400")
    private int status;
    @Schema(description = "Mensaje de error", example = "Datos de entrada inválidos")
    private String message;
    @Schema(description = "Detalles de los errores de validación")
    private Map<String, String> errors;
}