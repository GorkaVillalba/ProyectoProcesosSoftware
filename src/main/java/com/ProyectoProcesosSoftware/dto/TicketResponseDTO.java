package com.ProyectoProcesosSoftware.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.ProyectoProcesosSoftware.model.TicketStatus;

@Data
public class TicketResponseDTO {
    private Long id;
    private String uuid;

    // Datos del evento
    private Long eventoId;
    private String eventoNombre;
    private LocalDate eventoFecha;
    private LocalTime eventoHora;
    private String eventoUbicacion;

    // Datos del asistente
    private Long asistenteId;
    private String asistenteNombre;

    // Datos de la compra
    private BigDecimal precioFinal;
    private String estrategiaPrecio;
    private LocalDateTime fechaCompra;
    private TicketStatus estado;
}