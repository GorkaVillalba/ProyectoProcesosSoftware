package com.ProyectoProcesosSoftware.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Información completa de un evento, incluyendo estado, precios y organizador")
public class EventoResponseDTO {

    @Schema(description = "ID único del evento", example = "10")
    private Long id;

    @Schema(description = "Nombre del evento", example = "Concierto de rock")
    private String nombre;

    @Schema(description = "Descripción detallada", example = "Una noche mágica con los mejores artistas de jazz.")
    private String descripcion;

    @Schema(description = "Fecha de celebración", example = "2026-07-15")
    private LocalDate fecha;

    @Schema(description = "Hora de celebración", example = "20:00")
    private LocalTime hora;

    @Schema(description = "Ubicación del evento", example = "Teatro Principal")
    private String ubicacion;

    @Schema(description = "Aforo máximo permitido", example = "500")
    private Integer aforoMaximo;

    @Schema(description = "Número de entradas vendidas", example = "300")
    private Integer entradasVendidas;

    @Schema(description = "Número de plazas disponibles", example = "200")
    private Integer plazasDisponibles;

    @Schema(description = "Precio base del evento", example = "50.00")
    private BigDecimal precioBase;

    @Schema(description = "Precio actual del evento (puede variar según la estrategia de precios)", example = "65.00")
    private BigDecimal precioActual;

    @Schema(description = "Estrategia de precios aplicada", example = "DINÁMICO")
    private String estrategiaPrecio;
    
    @Schema(description = "Estado del evento", example = "PROGRAMADO")
    private String estado;

    @Schema(description = "Nombre del organizador", example = "John Doe")
    private String organizadorNombre;
    
    @Schema(description = "ID del organizador", example = "5")
    private Long organizadorId;

    @Schema(description = "Puntuación media de las reseñas (0.0 si no hay reseñas)", example = "4.5")
    private BigDecimal puntuacionMedia;

    @Schema(description = "Número total de reseñas del evento", example = "12")
    private Long numeroResenas;

}