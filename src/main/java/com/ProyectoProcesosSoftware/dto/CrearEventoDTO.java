package com.ProyectoProcesosSoftware.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Datos para crear un nuevo evento")
public class CrearEventoDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Schema(description = "Título del evento", example = "Concierto de jazz")

    private String nombre;

    @Schema(description = "Descripción del evento", example = "Un emocionante concierto de jazz en vivo")
    private String descripcion;

    @NotNull(message = "La fecha es obligatoria")
    @Future(message = "La fecha del evento debe ser futura")
   @Schema(description = "Fecha del evento (YYYY-MM-DD)", example = "2026-07-15")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    @Schema(description = "Hora del evento (HH:mm)", example = "20:00")
    private LocalTime hora;

    @NotBlank(message = "La ubicación es obligatoria")
    @Schema(description = "Ubicación del evento", example = "Teatro principal")
    private String ubicacion;

    @NotNull(message = "El aforo máximo es obligatorio")
    @Min(value = 1, message = "El aforo máximo debe ser al menos 1")
    @Schema(description = "Aforo máximo del evento", example = "500")
    private Integer aforoMaximo;

    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio base no puede ser negativo")
    @Schema(description = "Precio de la entrada (0.00 si es gratuito)", example = "29.99")
    private BigDecimal precioBase;
}