package com.ProyectoProcesosSoftware.dto;

import lombok.Data;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Información detallada del precio actual de un evento")
public class PrecioEventoDTO {
    
    @Schema(description = "ID del evento", example = "10")
    private Long eventoId;

    @Schema(description = "Precio base fijado por el organizador", example = "29.99")
    private BigDecimal precioBase;

    @Schema(description = "Precio actual del evento", example = "39.99")
    private BigDecimal precioActual;

    @Schema(description = "Estrategia de precios actual", example = "Última hora")
    private String estrategia;

    @Schema(description = "Nivel de precios", example = "Premium")
    private String nivel;

    @Schema(description = "Porcentaje de ocupación del evento", example = "75")
    private int porcentajeOcupacion;
}
