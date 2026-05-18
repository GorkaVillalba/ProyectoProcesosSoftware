package com.ProyectoProcesosSoftware.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * US-28 / T-28.1: agregados básicos para el panel del organizador.
 * Si el organizador no tiene eventos, todos los campos valen 0.
 */
@Data
@Schema(description = "Estadísticas agregadas de un organizador")
public class EstadisticasOrganizadorDTO {

    @Schema(description = "Número total de eventos del organizador", example = "12")
    private long numeroEventos;

    @Schema(description = "Suma de entradas vendidas en todos sus eventos", example = "1850")
    private long entradasVendidasTotales;

    @Schema(description = "Ingresos totales (suma de precioFinal de tickets VALIDO)", example = "92450.50")
    private BigDecimal ingresosTotales;

    @Schema(description = "Porcentaje medio de ocupación (entradasVendidas / aforoMaximo)", example = "67.30")
    private BigDecimal porcentajeOcupacionMedia;
}