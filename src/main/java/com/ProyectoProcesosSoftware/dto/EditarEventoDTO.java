package com.ProyectoProcesosSoftware.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EditarEventoDTO {
    @NotBlank private String nombre;
    private String descripcion;
    @NotNull private LocalDate fecha;
    @NotNull private LocalTime hora;
    @NotBlank private String ubicacion;
    @NotNull @Min(1) private Integer aforoMaximo;
    @NotNull @DecimalMin("0.0") private BigDecimal precioBase;
}
