package com.ProyectoProcesosSoftware.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.ProyectoProcesosSoftware.model.TicketStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Datos de una entrada comprada")
public class TicketResponseDTO {
    @Schema(description = "ID interno de la entrada", example = "5501")
    private Long id;
    @Schema(description = "Código único UUID de la entrada", example = "b3f8a210-5c1e-4f2a-9d3e-1a2b3c4d5e6f")
    private String uuid;
    @Schema(description = "ID del evento asociado", example = "10")
    private Long eventoId;
    @Schema(description = "Nombre del evento", example = "Concierto de rock")
    private String eventoNombre;
    @Schema(description = "Fecha del evento", example = "2023-12-25")
    private LocalDate eventoFecha;
    @Schema(description = "Hora del evento", example = "20:00")
    private LocalTime eventoHora;
    @Schema(description = "Ubicación del evento", example = "Auditorio Nacional, Madrid")
    private String eventoUbicacion;

    // Datos del asistente
    @Schema(description = "ID del asistente", example = "1001")
    private Long asistenteId;
    @Schema(description = "Nombre del asistente", example = "Juan Pérez")
    private String asistenteNombre;

    // Datos de la compra
    @Schema(description = "Precio final de la entrada", example = "39.99")
    private BigDecimal precioFinal;
    @Schema(description = "Estrategia de precios aplicada", example = "Última hora")
    private String estrategiaPrecio;
    @Schema(description = "Fecha de la compra", example = "2023-10-15T20:00:00")
    private LocalDateTime fechaCompra;
    @Schema(description = "Estado actual de la entrada", example = "VÁLIDA", allowableValues = {"VÁLIDA", "CANCELADA", "USADA"})
    private TicketStatus estado;
}