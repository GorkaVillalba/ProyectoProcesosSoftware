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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadisticasServiceTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private EstadisticasService service;

    private Usuario organizador;

    @BeforeEach
    void setUp() {
        organizador = new Usuario();
        organizador.setId(1L);
        organizador.setRol(Rol.ORGANIZADOR);
    }

    @Test
    @DisplayName("organizador_sinEventos_devuelveCeros")
    void organizador_sinEventos_devuelveCeros() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));
        when(eventoRepository.findByOrganizadorId(1L)).thenReturn(Collections.emptyList());

        EstadisticasOrganizadorDTO dto = service.obtenerEstadisticasOrganizador(1L);

        assertThat(dto.getNumeroEventos()).isZero();
        assertThat(dto.getEntradasVendidasTotales()).isZero();
        assertThat(dto.getIngresosTotales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getPorcentajeOcupacionMedia()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("organizador_conEventosSinVentas_ingresosCero")
    void organizador_conEventosSinVentas_ingresosCero() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));
        Evento e = nuevoEvento(100, 0);
        when(eventoRepository.findByOrganizadorId(1L)).thenReturn(List.of(e));
        when(ticketRepository.sumarIngresosByOrganizadorId(1L)).thenReturn(BigDecimal.ZERO);

        EstadisticasOrganizadorDTO dto = service.obtenerEstadisticasOrganizador(1L);

        assertThat(dto.getNumeroEventos()).isEqualTo(1);
        assertThat(dto.getEntradasVendidasTotales()).isZero();
        assertThat(dto.getIngresosTotales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getPorcentajeOcupacionMedia()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("organizador_conVentas_calculaCorrectamente")
    void organizador_conVentas_calculaCorrectamente() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));
        // Evento 1: 100 aforo, 50 vendidas (50%). Evento 2: 200 aforo, 100 vendidas (50%).
        when(eventoRepository.findByOrganizadorId(1L)).thenReturn(List.of(
                nuevoEvento(100, 50),
                nuevoEvento(200, 100)
        ));
        when(ticketRepository.sumarIngresosByOrganizadorId(1L))
                .thenReturn(new BigDecimal("7500.00"));

        EstadisticasOrganizadorDTO dto = service.obtenerEstadisticasOrganizador(1L);

        assertThat(dto.getNumeroEventos()).isEqualTo(2);
        assertThat(dto.getEntradasVendidasTotales()).isEqualTo(150);
        assertThat(dto.getIngresosTotales()).isEqualByComparingTo("7500.00");
        assertThat(dto.getPorcentajeOcupacionMedia()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("usuario_noOrganizador_lanza403")
    void usuario_noOrganizador_lanza403() {
        organizador.setRol(Rol.ASISTENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));

        assertThatThrownBy(() -> service.obtenerEstadisticasOrganizador(1L))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("organizadores");
    }

    private Evento nuevoEvento(int aforo, int vendidas) {
        Evento e = new Evento();
        e.setAforoMaximo(aforo);
        e.setEntradasVendidas(vendidas);
        e.setOrganizador(organizador);
        return e;
    }
}