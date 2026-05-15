package com.ProyectoProcesosSoftware.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * US-20: respuesta estructurada para errores 400 de validación.
 * El campo {@code errors} mapea el nombre de cada campo inválido al mensaje
 * concreto que ha producido la anotación, para que el cliente pueda mostrarlo
 * junto al input correspondiente.
 */
@Data
public class ValidationErrorResponseDTO {
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private Map<String, String> errors;
}