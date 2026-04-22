package com.ProyectoProcesosSoftware.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketResponseDTO {
    private Long id;
    private String uuid;
    private Long eventoId;
    private String eventoNombre;
    private Long asistenteId;
    private String asistenteNombre;
    private BigDecimal precioFinal;
    private String estrategiaPrecio;
    private LocalDateTime fechaCompra;
}