package com.ProyectoProcesosSoftware.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Respuesta simple con un mensaje informativo")
public class MessageResponseDTO {
    @Schema(description = "Mensaje descriptivo", example = "Operación realizada correctamente")
    private String message;
}
