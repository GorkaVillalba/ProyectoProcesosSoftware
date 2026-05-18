package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.CrearEventoDTO;
import com.ProyectoProcesosSoftware.dto.EditarEventoDTO;
import com.ProyectoProcesosSoftware.dto.EventoResponseDTO;
import com.ProyectoProcesosSoftware.dto.MessageResponseDTO;
import com.ProyectoProcesosSoftware.dto.PrecioEventoDTO;
import com.ProyectoProcesosSoftware.service.EventoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoControllerTest {

    @Mock private EventoService eventoService;
    @Mock private Authentication authentication;

    @InjectMocks private EventoController eventoController;

    private EventoResponseDTO eventoResponse;

    @BeforeEach
    void setUp() {
        eventoResponse = new EventoResponseDTO();
        eventoResponse.setId(10L);
        eventoResponse.setNombre("Evento Test");
    }

    @Test
    @DisplayName("crear: devuelve 201 con el evento creado y usa el id del Authentication")
    void crear() {
        CrearEventoDTO dto = new CrearEventoDTO();
        dto.setNombre("Evento Test");
        dto.setFecha(LocalDate.now().plusDays(30));
        dto.setHora(LocalTime.of(20, 0));
        dto.setUbicacion("Madrid");
        dto.setAforoMaximo(100);
        dto.setPrecioBase(BigDecimal.valueOf(20));

        when(authentication.getName()).thenReturn("3");
        when(eventoService.crearEvento(dto, 3L)).thenReturn(eventoResponse);

        ResponseEntity<EventoResponseDTO> response = eventoController.crear(dto, authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(eventoResponse);
    }

    @Test
    @DisplayName("listar: delega filtros y paginación al servicio")
    void listar() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<EventoResponseDTO> page = new PageImpl<>(List.of(eventoResponse));

        when(eventoService.listarEventos("conf", "Madrid", pageable)).thenReturn(page);

        ResponseEntity<Page<EventoResponseDTO>> response =
                eventoController.listar("conf", "Madrid", pageable);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getContent()).containsExactly(eventoResponse);
    }

    @Test
    @DisplayName("detalle: devuelve evento por id")
    void detalle() {
        when(eventoService.obtenerDetalle(10L)).thenReturn(eventoResponse);

        ResponseEntity<EventoResponseDTO> response = eventoController.detalle(10L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(eventoResponse);
    }

    @Test
    @DisplayName("precio: devuelve PrecioEventoDTO calculado por el servicio")
    void precio() {
        PrecioEventoDTO precio = new PrecioEventoDTO();
        precio.setEventoId(10L);
        precio.setPrecioActual(BigDecimal.valueOf(15));

        when(eventoService.obtenerPrecio(10L)).thenReturn(precio);

        ResponseEntity<PrecioEventoDTO> response = eventoController.precio(10L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(precio);
    }

    @Test
    @DisplayName("editar: pasa al servicio el id, dto y orgId del Authentication")
    void editar() {
        EditarEventoDTO dto = new EditarEventoDTO();
        dto.setNombre("Editado");
        dto.setFecha(LocalDate.now().plusDays(10));
        dto.setHora(LocalTime.of(18, 0));
        dto.setUbicacion("Bilbao");
        dto.setAforoMaximo(50);
        dto.setPrecioBase(BigDecimal.valueOf(25));

        when(authentication.getName()).thenReturn("4");
        when(eventoService.editarEvento(10L, dto, 4L)).thenReturn(eventoResponse);

        ResponseEntity<EventoResponseDTO> response =
                eventoController.editar(10L, dto, authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(eventoResponse);
    }

    @Test
    @DisplayName("eliminar: llama al servicio con el orgId del Authentication y responde con mensaje")
    void eliminar() {
        when(authentication.getName()).thenReturn("9");

        ResponseEntity<MessageResponseDTO> response =
                eventoController.eliminar(10L, authentication);

        verify(eventoService).eliminarEvento(10L, 9L);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Evento eliminado correctamente");
    }
}