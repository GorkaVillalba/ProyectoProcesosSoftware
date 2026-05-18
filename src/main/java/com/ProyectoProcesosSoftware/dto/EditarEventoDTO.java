package com.ProyectoProcesosSoftware.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Datos para actualizar un evento existente (todos los campos son obligatorios para la edición)")
public class EditarEventoDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Schema(description = "Nuevo título del evento", example = "Festival de jazz internacional")
    private String nombre;

    @Schema(description = "Nueva descripción", example = "Ahora con artistas invitados internacionales.")
    private String descripcion;


    @NotNull(message = "La fecha es obligatoria")
    @Schema(description = "Nueva fecha (YYYY-MM-DD). La validación de fecha pasada se realiza en el servicio.", example = "2026-08-20")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    @Schema(description = "Nueva hora (HH:mm)", example = "21:00")
    private LocalTime hora;

    @NotBlank(message = "La ubicación es obligatoria")
    @Schema(description = "Nueva ubicación del evento", example = "Centro de convenciones")
    private String ubicacion;

    @NotNull(message = "El aforo máximo es obligatorio")
    @Min(value = 1, message = "El aforo máximo debe ser al menos 1")
    @Schema(description = "Nuevo aforo máximo del evento", example = "600")
    private Integer aforoMaximo;

    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio base no puede ser negativo")
    @Schema(description = "Nuevo precio de la entrada (0.00 si es gratuito)", example = "39.99")
    private BigDecimal precioBase;
}