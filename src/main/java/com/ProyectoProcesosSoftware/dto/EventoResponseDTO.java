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
    private BigDecimal precioActual;
    private String estrategiaPrecio;
    private String estado;
    private String organizadorNombre;
    private Long organizadorId;

    /**
     * Media de las puntuaciones de las reseñas del evento.
     * Será 0.0 si el evento aún no tiene reseñas.
     */
    private BigDecimal puntuacionMedia;

    /**
     * Número total de reseñas del evento.
     * Será 0 si el evento aún no tiene reseñas.
     */
    private Long numeroResenas;
}