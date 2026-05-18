package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.EstadisticasOrganizadorDTO;
import com.ProyectoProcesosSoftware.service.EstadisticasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Estadísticas", description = "Panel de estadísticas para organizadores")
@RestController
@RequestMapping("/api/users/me")
public class EstadisticasController {
	

    @Autowired
    private EstadisticasService estadisticasService;

    @Operation(
        summary = "Obtener estadísticas del organizador",
        description = "Devuelve el resumen agregado de eventos, entradas vendidas, ingresos y ocupación media del organizador autenticado. Restringido a usuarios con rol ORGANIZADOR."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estadísticas calculadas correctamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "El usuario no es ORGANIZADOR")
    })
    @GetMapping("/stats")
    public ResponseEntity<EstadisticasOrganizadorDTO> getStats(Authentication auth) {
        Long organizadorId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(estadisticasService.obtenerEstadisticasOrganizador(organizadorId));
    }
}