package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.TicketResponseDTO;
import com.ProyectoProcesosSoftware.exception.BusinessRuleException;
import com.ProyectoProcesosSoftware.exception.UnauthorizedActionException;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.pricing.PricingContext;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PricingContext pricingContext;
    @InjectMocks private TicketService ticketService;

    private Evento evento;
    private Usuario asistente;

    @BeforeEach
    void setUp() {
        Usuario organizador = new Usuario();
        organizador.setId(99L);
        organizador.setNombre("Org");
        organizador.setRol(Rol.ORGANIZADOR);

        evento = new Evento();
        evento.setId(1L);
        evento.setNombre("Concierto");
        evento.setFecha(LocalDate.of(2027, 6, 1));
        evento.setHora(LocalTime.of(20, 0));
        evento.setUbicacion("Bilbao");
        evento.setAforoMaximo(100);
        evento.setEntradasVendidas(30);
        evento.setPrecioBase(new BigDecimal("50.00"));
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(organizador);

        asistente = new Usuario();
        asistente.setId(2L);
        asistente.setNombre("Juan");
        asistente.setRol(Rol.ASISTENTE);
    }

    @Test
    @DisplayName("Compra exitosa → devuelve TicketResponseDTO con precioFinal")
    void comprar_exitoso() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
        when(ticketRepository.existsByEventoIdAndAsistenteId(1L, 2L)).thenReturn(false);
        when(pricingContext.calcularPrecio(any(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("50.00"));
        when(pricingContext.nombreEstrategia(anyInt(), anyInt())).thenReturn("EarlyBird");
        when(ticketRepository.save(any())).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        TicketResponseDTO result = ticketService.comprarEntrada(1L, 2L);

        assertThat(result.getPrecioFinal()).isEqualByComparingTo("50.00");
        assertThat(result.getEstrategiaPrecio()).isEqualTo("EarlyBird");
        verify(ticketRepository).save(any());
        verify(eventoRepository).save(evento);
    }

    @Test
    @DisplayName("Evento agotado → lanza BusinessRuleException")
    void comprar_eventoAgotado() {
        evento.setEntradasVendidas(100);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> ticketService.comprarEntrada(1L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("plazas");
    }

    @Test
    @DisplayName("Evento no publicado → lanza BusinessRuleException")
    void comprar_eventoNoPub() {
        evento.setEstado(EstadoEvento.BORRADOR);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> ticketService.comprarEntrada(1L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("publicados");
    }

    @Test
    @DisplayName("Organizador intenta comprar → lanza UnauthorizedActionException")
    void comprar_organizador_403() {
        asistente.setRol(Rol.ORGANIZADOR);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
        lenient().when(ticketRepository.existsByEventoIdAndAsistenteId(1L, 2L)).thenReturn(false); // ← lenient()

        assertThatThrownBy(() -> ticketService.comprarEntrada(1L, 2L))
            .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    @DisplayName("Ya tiene entrada → lanza BusinessRuleException")
    void comprar_duplicado() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
        when(ticketRepository.existsByEventoIdAndAsistenteId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> ticketService.comprarEntrada(1L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ya tienes");
    }

    @Test
    @DisplayName("Última plaza → estado cambia a AGOTADO")
    void comprar_ultimaPlaza_agotado() {
        evento.setEntradasVendidas(99);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
        when(ticketRepository.existsByEventoIdAndAsistenteId(1L, 2L)).thenReturn(false);
        when(pricingContext.calcularPrecio(any(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("75.00"));
        when(pricingContext.nombreEstrategia(anyInt(), anyInt())).thenReturn("LastMinute");
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ticketService.comprarEntrada(1L, 2L);

        assertThat(evento.getEstado()).isEqualTo(EstadoEvento.AGOTADO);
    }
}