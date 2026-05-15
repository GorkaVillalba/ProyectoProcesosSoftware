package com.ProyectoProcesosSoftware.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EditarEventoDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    // La validación cruzada "fecha pasada solo si FINALIZADO" vive en
    // EventoService.editarEvento porque depende del estado actual del evento.
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    @NotBlank(message = "La ubicación es obligatoria")
    private String ubicacion;

    @NotNull(message = "El aforo máximo es obligatorio")
    @Min(value = 1, message = "El aforo máximo debe ser al menos 1")
    private Integer aforoMaximo;

    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio base no puede ser negativo")
    private BigDecimal precioBase;
}