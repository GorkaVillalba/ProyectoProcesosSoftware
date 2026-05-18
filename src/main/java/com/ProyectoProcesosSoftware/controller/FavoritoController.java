package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.EventoResponseDTO;
import com.ProyectoProcesosSoftware.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Favoritos", description = "Gestión de la lista de eventos favoritos del usuario")
@RestController
public class FavoritoController {

    @Autowired
    private FavoritoService favoritoService;

    // POST /api/events/{id}/favorite
    @Operation(
        summary = "Añadir evento a favoritos",
        description = "Marca un evento como favorito para el usuario autenticado. Idempotente: no falla si ya estaba marcado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Evento añadido a favoritos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    })
    @PostMapping("/api/events/{id}/favorite")
    public ResponseEntity<Void> agregar(
            @PathVariable Long id,
            Authentication auth) {
        Long usuarioId = Long.parseLong(auth.getName());
        favoritoService.agregar(usuarioId, id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // DELETE /api/events/{id}/favorite
    @Operation(
        summary = "Eliminar evento de favoritos",
        description = "Elimina un evento de la lista de favoritos del usuario autenticado. Idempotente: no falla si no estaba marcado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Evento eliminado de favoritos"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @DeleteMapping("/api/events/{id}/favorite")
    public ResponseEntity<Void> quitar(
            @PathVariable Long id,
            Authentication auth) {
        Long usuarioId = Long.parseLong(auth.getName());
        favoritoService.quitar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/users/me/favorites
    @Operation(
        summary = "Listar eventos favoritos",
        description = "Devuelve la lista de eventos marcados como favoritos por el usuario autenticado, con precio dinámico y valoraciones."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de eventos favoritos"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping("/api/users/me/favorites")
    public ResponseEntity<List<EventoResponseDTO>> listarFavoritos(Authentication auth) {
        Long usuarioId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(favoritoService.listarFavoritos(usuarioId));
    }
}