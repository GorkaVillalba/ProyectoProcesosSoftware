package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.TicketResponseDTO;
import com.ProyectoProcesosSoftware.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock private TicketService ticketService;
    @Mock private Authentication authentication;

    @InjectMocks private TicketController ticketController;

    private TicketResponseDTO ticketResponse;

    @BeforeEach
    void setUp() {
        ticketResponse = new TicketResponseDTO();
        ticketResponse.setId(100L);
        ticketResponse.setEventoId(10L);
        ticketResponse.setAsistenteId(2L);
    }

    @Test
    @DisplayName("comprar: devuelve 201 CREATED con el ticket comprado")
    void comprar() {
        when(authentication.getName()).thenReturn("2");
        when(ticketService.comprarEntrada(10L, 2L)).thenReturn(ticketResponse);

        ResponseEntity<TicketResponseDTO> response =
                ticketController.comprar(10L, authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(ticketResponse);
    }

    @Test
    @DisplayName("misEntradas: devuelve la lista de tickets del usuario autenticado")
    void misEntradas() {
        when(authentication.getName()).thenReturn("2");
        when(ticketService.misEntradas(2L)).thenReturn(List.of(ticketResponse));

        ResponseEntity<List<TicketResponseDTO>> response =
                ticketController.misEntradas(authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(ticketResponse);
    }

    @Test
    @DisplayName("getMisEntradas: devuelve los tickets ordenados por fecha de compra")
    void getMisEntradas() {
        when(authentication.getName()).thenReturn("2");
        when(ticketService.getMisEntradas(2L)).thenReturn(List.of(ticketResponse));

        ResponseEntity<List<TicketResponseDTO>> response =
                ticketController.getMisEntradas(authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(ticketResponse);
    }

    @Test
    @DisplayName("cancelar: delega en el servicio con id y usuarioId del token")
    void cancelar() {
        when(authentication.getName()).thenReturn("2");
        when(ticketService.cancelarEntrada(100L, 2L)).thenReturn(ticketResponse);

        ResponseEntity<TicketResponseDTO> response =
                ticketController.cancelar(100L, authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(ticketResponse);
    }
}