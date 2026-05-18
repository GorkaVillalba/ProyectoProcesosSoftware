package com.ProyectoProcesosSoftware.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "Datos de una reseña publicada sobre un evento")
public class ResenaResponseDTO {

    @Schema(description = "Identificador único de la reseña", example = "1")
    private Long id;

    @Schema(description = "Identificador del evento reseñado", example = "42")
    private Long eventoId;

    @Schema(description = "Nombre del evento reseñado", example = "Concierto de Jazz en Bilbao")
    private String eventoNombre;

    @Schema(description = "Identificador del asistente que publicó la reseña", example = "7")
    private Long asistenteId;

    @Schema(description = "Nombre del asistente que publicó la reseña", example = "María López")
    private String asistenteNombre;

    @Schema(description = "Puntuación otorgada al evento (1-5)", example = "5", minimum = "1", maximum = "5")
    private Integer puntuacion;

    @Schema(description = "Comentario opcional del asistente", example = "Increíble experiencia, muy recomendado.")
    private String comentario;

    @Schema(description = "Fecha y hora en que se publicó la reseña", example = "2025-06-15T21:30:00")
    private LocalDateTime fechaCreacion;
}