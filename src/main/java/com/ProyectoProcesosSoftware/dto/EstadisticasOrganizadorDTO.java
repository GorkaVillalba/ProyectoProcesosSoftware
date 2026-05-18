package com.ProyectoProcesosSoftware.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "Estadísticas agregadas de un organizador sobre sus eventos")
public class EstadisticasOrganizadorDTO {

    @Schema(description = "Número total de eventos creados por el organizador", example = "12")
    private Long numeroEventos;

    @Schema(description = "Suma total de entradas vendidas en todos los eventos del organizador", example = "850")
    private Long entradasVendidasTotales;

    @Schema(description = "Ingresos totales generados por la venta de entradas (en euros)", example = "21250.00")
    private BigDecimal ingresosTotales;

    @Schema(description = "Porcentaje medio de ocupación sobre todos los eventos del organizador (0-100)", example = "74.30")
    private BigDecimal porcentajeOcupacionMedia;
}