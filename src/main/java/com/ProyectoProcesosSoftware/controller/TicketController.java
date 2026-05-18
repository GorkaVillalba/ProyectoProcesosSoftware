package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.TicketResponseDTO;
import com.ProyectoProcesosSoftware.service.TicketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Entradas", description = "Compra y consulta de entradas a eventos")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/eventos/{eventoId}")
    @Operation(
        summary = "Comprar entrada",
        description = "Permite a un usuario autenticado (asistente) comprar una entrada para un evento específico."
    )
    public ResponseEntity<TicketResponseDTO> comprar(
            @PathVariable Long eventoId, Authentication auth) {
        Long asistenteId = Long.parseLong(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.comprarEntrada(eventoId, asistenteId));
    }

    @GetMapping("/mis-entradas")
    @Operation(
        summary = "Consultar mis entradas (alternativo)",
        description = "Devuelve la lista de entradas compradas por el usuario autenticado (endpoint alternativo)."
    )
    public ResponseEntity<List<TicketResponseDTO>> misEntradas(Authentication auth) {
        Long asistenteId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(ticketService.misEntradas(asistenteId));
    }
    
    @GetMapping("/my")
    @Operation(
        summary = "Consultar mis entradas",
        description = "Obtiene las entradas del usuario autenticado a través del token JWT."
    )
    public ResponseEntity<List<TicketResponseDTO>> getMisEntradas(Authentication auth) {
        Long usuarioId = Long.parseLong(auth.getName()); // extraído del token JWT
        return ResponseEntity.ok(ticketService.getMisEntradas(usuarioId));
    }   
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Cancelar entrada",
        description = "Permite a un usuario autenticado cancelar una entrada previamente comprada."
    )
    public ResponseEntity<TicketResponseDTO> cancelar(
            @PathVariable Long id, Authentication auth) {
        Long usuarioId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(ticketService.cancelarEntrada(id, usuarioId));
    }
}