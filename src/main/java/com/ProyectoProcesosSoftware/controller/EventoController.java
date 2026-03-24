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

// ═══════════════════════════════════════════════════════════════
// EventoController - Endpoints de eventos
// T-17 (Persona 6): POST, PUT, DELETE
// T-18 (Persona 5): GET listado
// T-20 (Persona 1): GET detalle
// ═══════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/events")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @PostMapping
    public ResponseEntity<EventoResponseDTO> crear(
            @Valid @RequestBody CrearEventoDTO dto, Authentication auth) {
        Long orgId = Long.parseLong(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crearEvento(dto, orgId));
    }

    @GetMapping
    public ResponseEntity<Page<EventoResponseDTO>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String ubicacion,
            Pageable pageable) {
        return ResponseEntity.ok(eventoService.listarEventos(nombre, ubicacion, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> detalle(@PathVariable("id") Long id) {
        return ResponseEntity.ok(eventoService.obtenerDetalle(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> editar(
            @PathVariable Long id, @Valid @RequestBody EditarEventoDTO dto, Authentication auth) {
        Long orgId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(eventoService.editarEvento(id, dto, orgId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponseDTO> eliminar(@PathVariable Long id, Authentication auth) {
        Long orgId = Long.parseLong(auth.getName());
        eventoService.eliminarEvento(id, orgId);
        return ResponseEntity.ok(new MessageResponseDTO("Evento eliminado correctamente"));
    }
}
