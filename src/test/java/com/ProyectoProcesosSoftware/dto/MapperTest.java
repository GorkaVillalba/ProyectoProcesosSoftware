package com.ProyectoProcesosSoftware.dto;

import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.pricing.PricingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MapperTest {

    private PricingContext pricingContext;
    private Usuario usuario;
    private Evento evento;

    @BeforeEach
    void setUp() {
        pricingContext = mock(PricingContext.class);
        when(pricingContext.calcularPrecio(any(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("125.00"));
        when(pricingContext.nombreEstrategia(anyInt(), anyInt()))
                .thenReturn("Regular");

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Juan García");
        usuario.setEmail("juan@example.com");
        usuario.setRol(Rol.ASISTENTE);
        usuario.setFechaRegistro(LocalDateTime.of(2025, 1, 15, 10, 0));

        evento = new Evento();
        evento.setId(10L);
        evento.setNombre("Concierto Rock");
        evento.setDescripcion("Gran concierto");
        evento.setFecha(LocalDate.of(2025, 6, 15));
        evento.setHora(LocalTime.of(20, 0));
        evento.setUbicacion("Bilbao Arena");
        evento.setAforoMaximo(500);
        evento.setEntradasVendidas(150);
        evento.setPrecioBase(new BigDecimal("100.00"));
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(usuario);
    }

    // ── UsuarioMapper ──

    @Test
    @DisplayName("UsuarioMapper mapea todos los campos correctamente")
    void usuarioMapper_mapeaCamposCorrectamente() {
        UsuarioResponseDTO dto = UsuarioMapper.toResponseDTO(usuario);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNombre()).isEqualTo("Juan García");
        assertThat(dto.getEmail()).isEqualTo("juan@example.com");
        assertThat(dto.getRol()).isEqualTo("ASISTENTE");
        assertThat(dto.getFechaRegistro()).isEqualTo(LocalDateTime.of(2025, 1, 15, 10, 0));
    }

    // ── EventoMapper ──

    @Test
    @DisplayName("EventoMapper mapea todos los campos correctamente")
    void eventoMapper_mapeaCamposCorrectamente() {
        EventoResponseDTO dto = EventoMapper.toResponseDTO(evento, pricingContext);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getNombre()).isEqualTo("Concierto Rock");
        assertThat(dto.getDescripcion()).isEqualTo("Gran concierto");
        assertThat(dto.getFecha()).isEqualTo(LocalDate.of(2025, 6, 15));
        assertThat(dto.getHora()).isEqualTo(LocalTime.of(20, 0));
        assertThat(dto.getUbicacion()).isEqualTo("Bilbao Arena");
        assertThat(dto.getAforoMaximo()).isEqualTo(500);
        assertThat(dto.getEntradasVendidas()).isEqualTo(150);
        assertThat(dto.getEstado()).isEqualTo("PUBLICADO");
        assertThat(dto.getOrganizadorNombre()).isEqualTo("Juan García");
        assertThat(dto.getOrganizadorId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("EventoMapper calcula plazas disponibles correctamente")
    void eventoMapper_plazasDisponibles() {
        EventoResponseDTO dto = EventoMapper.toResponseDTO(evento, pricingContext);
        assertThat(dto.getPlazasDisponibles()).isEqualTo(350);
    }

    @Test
    @DisplayName("EventoMapper asigna precio dinámico y estrategia del PricingContext")
    void eventoMapper_precioDinamico() {
        EventoResponseDTO dto = EventoMapper.toResponseDTO(evento, pricingContext);
        assertThat(dto.getPrecioActual()).isEqualByComparingTo("125.00");
        assertThat(dto.getEstrategiaPrecio()).isEqualTo("Regular");
    }
}