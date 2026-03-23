package com.ProyectoProcesosSoftware.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventoResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private LocalDate fecha;
    private LocalTime hora;
    private String ubicacion;
    private Integer aforoMaximo;
    private Integer entradasVendidas;
    private Integer plazasDisponibles;
    private BigDecimal precioBase;
    private String estado;
    private String organizadorNombre;
    private Long organizadorId;
}
