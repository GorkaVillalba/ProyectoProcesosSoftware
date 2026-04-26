package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.TicketResponseDTO;
import com.ProyectoProcesosSoftware.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")

public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/{id}/tickets") 
    public ResponseEntity<TicketResponseDTO> comprar(
            @PathVariable Long eventoId, Authentication auth) {
        Long asistenteId = Long.parseLong(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.comprarEntrada(eventoId, asistenteId));
    }

    @GetMapping("/mis-entradas")
    public ResponseEntity<List<TicketResponseDTO>> misEntradas(Authentication auth) {
        Long asistenteId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(ticketService.misEntradas(asistenteId));
    }
    
    // GET /api/tickets/my — consultar entradas del usuario autenticado
    @GetMapping("/my")
    public ResponseEntity<List<TicketResponseDTO>> getMisEntradas(Authentication auth) {
        Long usuarioId = Long.parseLong(auth.getName()); // extraído del token JWT
        return ResponseEntity.ok(ticketService.getMisEntradas(usuarioId));
    }
}