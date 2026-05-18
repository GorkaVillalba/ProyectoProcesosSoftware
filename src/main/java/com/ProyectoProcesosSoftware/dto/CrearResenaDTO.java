package com.ProyectoProcesosSoftware.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Datos necesarios para crear una reseña sobre un evento")
public class CrearResenaDTO {

    @Schema(
        description = "Puntuación del evento entre 1 (muy malo) y 5 (excelente)",
        example = "4",
        minimum = "1",
        maximum = "5",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "La puntuación es obligatoria")
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    private Integer puntuacion;

    @Schema(
        description = "Comentario opcional sobre el evento (máximo 500 caracteres)",
        example = "Excelente organización y ambiente increíble.",
        maxLength = 500,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
    private String comentario;
}