package com.ProyectoProcesosSoftware.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ResenaResponseDTO {

    private Long id;

    // Datos del evento
    private Long eventoId;
    private String eventoNombre;

    // Datos del asistente
    private Long asistenteId;
    private String asistenteNombre;

    // Reseña
    private Integer puntuacion;
    private String comentario;
    private LocalDateTime fechaCreacion;
}