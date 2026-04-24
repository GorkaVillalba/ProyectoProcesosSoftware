package com.ProyectoProcesosSoftware.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PrecioEventoDTO {
    private Long eventoId;
    private BigDecimal precioBase;
    private BigDecimal precioActual;
    private String estrategia;
    private String nivel;
    private int porcentajeOcupacion;
}
