package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.EstadisticasOrganizadorDTO;
import com.ProyectoProcesosSoftware.service.EstadisticasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Estadísticas", description = "Panel de métricas para organizadores")
public class EstadisticasController {

    @Autowired
    private EstadisticasService estadisticasService;

    @GetMapping("/me/stats")
    @Operation(
        summary = "Estadísticas del organizador autenticado",
        description = "Devuelve numeroEventos, entradasVendidasTotales, ingresosTotales y porcentajeOcupacionMedia. Solo accesible por usuarios con rol ORGANIZADOR.")
    @ApiResponse(responseCode = "200", description = "Estadísticas calculadas",
            content = @Content(schema = @Schema(implementation = EstadisticasOrganizadorDTO.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es ORGANIZADOR")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    public ResponseEntity<EstadisticasOrganizadorDTO> miPanel(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(estadisticasService.obtenerEstadisticasOrganizador(userId));
    }
}