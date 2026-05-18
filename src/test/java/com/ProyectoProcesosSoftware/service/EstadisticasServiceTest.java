package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.EstadisticasOrganizadorDTO;
import com.ProyectoProcesosSoftware.exception.UnauthorizedActionException;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.TicketRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * T-28.5: Tests unitarios de EstadisticasService.
 */
@ExtendWith(MockitoExtension.class)
class EstadisticasServiceTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private EstadisticasService estadisticasService;

    private Usuario organizador;

    @BeforeEach
    void setUp() {
        organizador = new Usuario();
        organizador.setId(1L);
        organizador.setNombre("Org Test");
        organizador.setRol(Rol.ORGANIZADOR);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Evento eventoFake(int vendidas, int aforo, BigDecimal precio) {
        Evento e = new Evento();
        e.setId((long)(Math.random() * 1000));
        e.setNombre("Evento");
        e.setFecha(LocalDate.now().plusMonths(1));
        e.setHora(LocalTime.of(20, 0));
        e.setUbicacion("Bilbao");
        e.setAforoMaximo(aforo);
        e.setEntradasVendidas(vendidas);
        e.setPrecioBase(precio);
        e.setEstado(EstadoEvento.PUBLICADO);
        e.setOrganizador(organizador);
        return e;
    }

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("organizador_sinEventos_devuelveCeros: todos los campos a 0 cuando no hay eventos")
    void organizador_sinEventos_devuelveCeros() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));
        when(eventoRepository.findByOrganizadorId(1L)).thenReturn(List.of());

        EstadisticasOrganizadorDTO dto = estadisticasService.obtenerEstadisticasOrganizador(1L);

        assertThat(dto.getNumeroEventos()).isEqualTo(0L);
        assertThat(dto.getEntradasVendidasTotales()).isEqualTo(0L);
        assertThat(dto.getIngresosTotales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getPorcentajeOcupacionMedia()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(ticketRepository, never()).sumarIngresosByOrganizadorId(any());
    }

    @Test
    @DisplayName("organizador_conEventosSinVentas_ingresosCero: eventos con 0 ventas devuelven ingresos 0 y ocupación 0")
    void organizador_conEventosSinVentas_ingresosCero() {
        Evento e1 = eventoFake(0, 100, new BigDecimal("50.00"));
        Evento e2 = eventoFake(0, 200, new BigDecimal("30.00"));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));
        when(eventoRepository.findByOrganizadorId(1L)).thenReturn(List.of(e1, e2));
        when(ticketRepository.sumarIngresosByOrganizadorId(1L)).thenReturn(BigDecimal.ZERO);

        EstadisticasOrganizadorDTO dto = estadisticasService.obtenerEstadisticasOrganizador(1L);

        assertThat(dto.getNumeroEventos()).isEqualTo(2L);
        assertThat(dto.getEntradasVendidasTotales()).isEqualTo(0L);
        assertThat(dto.getIngresosTotales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getPorcentajeOcupacionMedia()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("organizador_conVentas_calculaCorrectamente: entradas, ingresos y ocupación son correctos")
    void organizador_conVentas_calculaCorrectamente() {
        // Evento 1: 50 vendidas de 100 = 50% ocupación
        Evento e1 = eventoFake(50, 100, new BigDecimal("40.00"));
        // Evento 2: 75 vendidas de 100 = 75% ocupación
        Evento e2 = eventoFake(75, 100, new BigDecimal("60.00"));
        // Ocupación media esperada: (50 + 75) / 2 = 62.5%

        BigDecimal ingresosReales = new BigDecimal("7250.00"); // suma de precioFinal de tickets VALIDO

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));
        when(eventoRepository.findByOrganizadorId(1L)).thenReturn(List.of(e1, e2));
        when(ticketRepository.sumarIngresosByOrganizadorId(1L)).thenReturn(ingresosReales);

        EstadisticasOrganizadorDTO dto = estadisticasService.obtenerEstadisticasOrganizador(1L);

        assertThat(dto.getNumeroEventos()).isEqualTo(2L);
        assertThat(dto.getEntradasVendidasTotales()).isEqualTo(125L); // 50 + 75
        assertThat(dto.getIngresosTotales()).isEqualByComparingTo(new BigDecimal("7250.00"));
        assertThat(dto.getPorcentajeOcupacionMedia()).isEqualByComparingTo(new BigDecimal("62.50"));
    }

    @Test
    @DisplayName("organizador_conVentas_ingresoNulo_trataComoZero: si el repo devuelve null, ingresos = 0")
    void organizador_conVentas_ingresoNulo_trataComoZero() {
        Evento e1 = eventoFake(10, 100, new BigDecimal("50.00"));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));
        when(eventoRepository.findByOrganizadorId(1L)).thenReturn(List.of(e1));
        when(ticketRepository.sumarIngresosByOrganizadorId(1L)).thenReturn(null);

        EstadisticasOrganizadorDTO dto = estadisticasService.obtenerEstadisticasOrganizador(1L);

        assertThat(dto.getIngresosTotales()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("usuario_noOrganizador_lanzaUnauthorized")
    void usuario_noOrganizador_lanzaUnauthorized() {
        organizador.setRol(Rol.ASISTENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));

        assertThatThrownBy(() -> estadisticasService.obtenerEstadisticasOrganizador(1L))
                .isInstanceOf(UnauthorizedActionException.class);
    }
}