package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.CrearResenaDTO;
import com.ProyectoProcesosSoftware.dto.ResenaResponseDTO;
import com.ProyectoProcesosSoftware.service.ResenaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reseñas", description = "Gestión de reseñas y valoraciones de eventos")
@RestController
@RequestMapping("/api/events")
public class ResenaController {

    @Autowired
    private ResenaService resenaService;

    // POST /api/events/{id}/reviews — asistente autenticado
    @Operation(
        summary = "Crear reseña",
        description = "Permite a un asistente con entrada válida dejar una reseña sobre un evento."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reseña creada correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o regla de negocio incumplida"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "El usuario no es ASISTENTE"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
        @ApiResponse(responseCode = "409", description = "Ya existe una reseña del asistente para este evento")
    })
    @PostMapping("/{id}/reviews")
    public ResponseEntity<ResenaResponseDTO> crearResena(
            @PathVariable Long id,
            @Valid @RequestBody CrearResenaDTO dto,
            Authentication auth) {
        Long asistenteId = Long.parseLong(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resenaService.crearResena(id, asistenteId, dto));
    }

    // GET /api/events/{id}/reviews — público, paginado
    @Operation(
        summary = "Listar reseñas de un evento",
        description = "Devuelve las reseñas de un evento de forma paginada. No requiere autenticación."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de reseñas"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    @GetMapping("/{id}/reviews")
    public ResponseEntity<Page<ResenaResponseDTO>> listarResenas(
            @PathVariable Long id,
            Pageable pageable) {
        return ResponseEntity.ok(resenaService.listarResenasEvento(id, pageable));
    }
}