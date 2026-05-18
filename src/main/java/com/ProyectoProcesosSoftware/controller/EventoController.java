package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.*;
import com.ProyectoProcesosSoftware.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/events")
@Tag(name = "Eventos", description = "Gestión de eventos: creación, consulta, edición y eliminación")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @PostMapping
        @Operation(
        summary = "Crear evento",
        description = "Crea un nuevo evento. Requiere autenticación con rol de organizador."
    )
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
        content = @Content(schema = @Schema(implementation = ValidationErrorResponseDTO.class)))
    public ResponseEntity<EventoResponseDTO> crear(
            @Valid @RequestBody CrearEventoDTO dto, Authentication auth) {
        Long orgId = Long.parseLong(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crearEvento(dto, orgId));
    }

    @GetMapping
        @Operation(
        summary = "Listar eventos",
        description = "Obtiene una página de eventos. Se puede filtrar por nombre y ubicación."
    )
    public ResponseEntity<Page<EventoResponseDTO>> listar(
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "ubicacion", required = false) String ubicacion,
            Pageable pageable) {
        return ResponseEntity.ok(eventoService.listarEventos(nombre, ubicacion, pageable));
    }

    @GetMapping("/{id}")
        @Operation(
        summary = "Obtener detalle del evento",
        description = "Devuelve la información completa de un evento específico."
    )
    public ResponseEntity<EventoResponseDTO> detalle(@PathVariable("id") Long id) {
        return ResponseEntity.ok(eventoService.obtenerDetalle(id));
    }

    @GetMapping("/{id}/price")
        @Operation(
        summary = "Consultar precio del evento",
        description = "Obtiene el precio actual del evento identificado por su ID."
    )
    public ResponseEntity<PrecioEventoDTO> precio(@PathVariable("id") Long id) {
        return ResponseEntity.ok(eventoService.obtenerPrecio(id));
    }

    @PutMapping("/{id}")
        @Operation(
        summary = "Editar evento",
        description = "Actualiza los datos de un evento existente. Solo el organizador propietario puede editarlo."
    )
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
        content = @Content(schema = @Schema(implementation = ValidationErrorResponseDTO.class)))
    public ResponseEntity<EventoResponseDTO> editar(
            @PathVariable Long id, @Valid @RequestBody EditarEventoDTO dto, Authentication auth) {
        Long orgId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(eventoService.editarEvento(id, dto, orgId));
    }

    @DeleteMapping("/{id}")
        @Operation(
        summary = "Eliminar evento",
        description = "Elimina un evento existente. Solo el organizador propietario puede eliminarlo."
    )
    public ResponseEntity<MessageResponseDTO> eliminar(@PathVariable Long id, Authentication auth) {
        Long orgId = Long.parseLong(auth.getName());
        eventoService.eliminarEvento(id, orgId);
        return ResponseEntity.ok(new MessageResponseDTO("Evento eliminado correctamente"));
    }
}
