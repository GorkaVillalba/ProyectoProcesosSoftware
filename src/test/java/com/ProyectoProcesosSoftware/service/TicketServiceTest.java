package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.TicketResponseDTO;
import com.ProyectoProcesosSoftware.exception.BusinessRuleException;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.exception.UnauthorizedActionException;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.pricing.PricingContext;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.TicketRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * T-18: Tests unitarios del servicio de compra de entradas.
 *
 * Cubre los escenarios exigidos por la tarea:
 *  - Compra exitosa con estado VALIDO y precio correcto.
 *  - Evento no existente (404 / ResourceNotFoundException).
 *  - Evento no PUBLICADO (409 / BusinessRuleException).
 *  - Usuario no ASISTENTE (403 / UnauthorizedActionException).
 *  - Aforo completo (409 / BusinessRuleException).
 *  - Compra que completa aforo -> estado AGOTADO.
 *  - entradasVendidas se incrementa.
 *  - Integración correcta con PricingContext (Strategy Pattern).
 */
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

    // ---------------------------------------------------------------------
    // comprarEntrada — casos de éxito
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("comprarEntrada — casos de éxito")
    class CompraExitosa {

        @Test
        @DisplayName("Compra válida: devuelve DTO con precio final y nombre de estrategia")
        void comprar_exitoso_devuelveDTO() {
            stubCompraValida(new BigDecimal("50.00"), "EarlyBird");

            TicketResponseDTO result = ticketService.comprarEntrada(1L, 2L);

            assertThat(result).isNotNull();
            assertThat(result.getPrecioFinal()).isEqualByComparingTo("50.00");
            assertThat(result.getEstrategiaPrecio()).isEqualTo("EarlyBird");
            assertThat(result.getEventoId()).isEqualTo(1L);
            assertThat(result.getEventoNombre()).isEqualTo("Concierto");
            assertThat(result.getAsistenteId()).isEqualTo(2L);
            assertThat(result.getAsistenteNombre()).isEqualTo("Juan");
        }

        @Test
        @DisplayName("El ticket creado queda con estado VALIDO y el precio devuelto por la strategy")
        void comprar_persisteTicketValidoConPrecioDeStrategy() {
            BigDecimal precioStrategy = new BigDecimal("62.50");
            stubCompraValida(precioStrategy, "Regular");

            ticketService.comprarEntrada(1L, 2L);

            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(captor.capture());
            Ticket guardado = captor.getValue();

            assertThat(guardado.getEstado()).isEqualTo(TicketStatus.VALIDO);
            assertThat(guardado.getPrecioFinal()).isEqualByComparingTo(precioStrategy);
            assertThat(guardado.getEvento()).isSameAs(evento);
            assertThat(guardado.getAsistente()).isSameAs(asistente);
        }

        @Test
        @DisplayName("entradasVendidas se incrementa en 1 y se persiste el evento")
        void comprar_incrementaEntradasVendidas() {
            stubCompraValida(new BigDecimal("50.00"), "EarlyBird");

            ticketService.comprarEntrada(1L, 2L);

            assertThat(evento.getEntradasVendidas()).isEqualTo(31);
            verify(eventoRepository).save(evento);
        }

        @Test
        @DisplayName("PricingContext recibe precioBase, entradasVendidas (antes de incrementar) y aforo")
        void comprar_invocaStrategyConArgumentosCorrectos() {
            stubCompraValida(new BigDecimal("50.00"), "EarlyBird");

            ticketService.comprarEntrada(1L, 2L);

            verify(pricingContext).calcularPrecio(
                    new BigDecimal("50.00"), 30, 100);
            verify(pricingContext).nombreEstrategia(30, 100);
        }

        @Test
        @DisplayName("Compra que no completa el aforo deja el evento en PUBLICADO")
        void comprar_noUltima_mantienePublicado() {
            stubCompraValida(new BigDecimal("50.00"), "EarlyBird");

            ticketService.comprarEntrada(1L, 2L);

            assertThat(evento.getEstado()).isEqualTo(EstadoEvento.PUBLICADO);
        }

        @Test
        @DisplayName("Compra que ocupa la última plaza pasa el evento a AGOTADO")
        void comprar_ultimaPlaza_cambiaAgotado() {
            evento.setEntradasVendidas(99);
            stubCompraValida(new BigDecimal("75.00"), "LastMinute");

            ticketService.comprarEntrada(1L, 2L);

            assertThat(evento.getEntradasVendidas()).isEqualTo(100);
            assertThat(evento.getEstado()).isEqualTo(EstadoEvento.AGOTADO);
        }

        private void stubCompraValida(BigDecimal precio, String estrategia) {
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
            when(ticketRepository.existsByEventoIdAndAsistenteId(1L, 2L)).thenReturn(false);
            when(pricingContext.calcularPrecio(any(), anyInt(), anyInt())).thenReturn(precio);
            when(pricingContext.nombreEstrategia(anyInt(), anyInt())).thenReturn(estrategia);
            when(ticketRepository.save(any())).thenAnswer(inv -> {
                Ticket t = inv.getArgument(0);
                t.setId(10L);
                return t;
            });
        }
    }

    // ---------------------------------------------------------------------
    // comprarEntrada — errores
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("comprarEntrada — validaciones y errores")
    class CompraErrores {

        @Test
        @DisplayName("Evento no existe -> ResourceNotFoundException (404)")
        void comprar_eventoNoExistente_404() {
            when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.comprarEntrada(999L, 2L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Evento");

            verify(ticketRepository, never()).save(any());
            verify(eventoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Evento en BORRADOR -> BusinessRuleException (409)")
        void comprar_eventoNoPublicado_BORRADOR() {
            evento.setEstado(EstadoEvento.BORRADOR);
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

            assertThatThrownBy(() -> ticketService.comprarEntrada(1L, 2L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("publicados");

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Evento en AGOTADO -> BusinessRuleException (no permite compra)")
        void comprar_eventoEnAgotado() {
            evento.setEstado(EstadoEvento.AGOTADO);
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

            assertThatThrownBy(() -> ticketService.comprarEntrada(1L, 2L))
                    .isInstanceOf(BusinessRuleException.class);

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Aforo completo -> BusinessRuleException (409)")
        void comprar_aforoCompleto_409() {
            evento.setEntradasVendidas(100);
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

            assertThatThrownBy(() -> ticketService.comprarEntrada(1L, 2L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("plazas");

            verify(usuarioRepository, never()).findById(any());
            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Usuario no existe -> ResourceNotFoundException (404)")
        void comprar_usuarioNoExistente_404() {
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.comprarEntrada(1L, 2L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuario");

            verify(ticketRepository, never()).save(any());
            verify(eventoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Usuario con rol ORGANIZADOR -> UnauthorizedActionException (403)")
        void comprar_organizador_403() {
            asistente.setRol(Rol.ORGANIZADOR);
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));

            assertThatThrownBy(() -> ticketService.comprarEntrada(1L, 2L))
                    .isInstanceOf(UnauthorizedActionException.class)
                    .hasMessageContaining("asistentes");

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ya tiene una entrada para este evento -> BusinessRuleException")
        void comprar_duplicado() {
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
            when(ticketRepository.existsByEventoIdAndAsistenteId(1L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> ticketService.comprarEntrada(1L, 2L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Ya tienes");

            verify(ticketRepository, never()).save(any());
            verify(eventoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si la validación falla, no se llama al PricingContext")
        void comprar_errorValidacion_noInvocaStrategy() {
            evento.setEstado(EstadoEvento.CANCELADO);
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

            assertThatThrownBy(() -> ticketService.comprarEntrada(1L, 2L))
                    .isInstanceOf(BusinessRuleException.class);

            verifyNoInteractions(pricingContext);
        }
    }

    // ---------------------------------------------------------------------
    // misEntradas / getMisEntradas
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Consulta de entradas del usuario")
    class Consulta {

        @Test
        @DisplayName("misEntradas devuelve los tickets del usuario mapeados a DTO")
        void misEntradas_mapea() {
            Ticket t = ticketMock(100L, new BigDecimal("50.00"));
            when(ticketRepository.findByAsistenteId(2L)).thenReturn(List.of(t));
            when(pricingContext.nombreEstrategia(anyInt(), anyInt())).thenReturn("EarlyBird");

            List<TicketResponseDTO> out = ticketService.misEntradas(2L);

            assertThat(out).hasSize(1);
            assertThat(out.get(0).getId()).isEqualTo(100L);
            assertThat(out.get(0).getEstrategiaPrecio()).isEqualTo("EarlyBird");
            verify(pricingContext, never()).calcularPrecio(any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("misEntradas con usuario sin tickets devuelve lista vacía")
        void misEntradas_listaVacia() {
            when(ticketRepository.findByAsistenteId(2L)).thenReturn(Collections.emptyList());

            List<TicketResponseDTO> out = ticketService.misEntradas(2L);

            assertThat(out).isEmpty();
            verifyNoInteractions(pricingContext);
        }

        @Test
        @DisplayName("getMisEntradas usa findByAsistenteIdOrderByFechaCompraDesc")
        void getMisEntradas_ordenDesc() {
            Ticket t = ticketMock(200L, new BigDecimal("75.00"));
            when(ticketRepository.findByAsistenteIdOrderByFechaCompraDesc(2L))
                    .thenReturn(List.of(t));
            when(pricingContext.nombreEstrategia(anyInt(), anyInt())).thenReturn("Regular");

            List<TicketResponseDTO> out = ticketService.getMisEntradas(2L);

            assertThat(out).hasSize(1);
            assertThat(out.get(0).getId()).isEqualTo(200L);
            assertThat(out.get(0).getEstrategiaPrecio()).isEqualTo("Regular");
            verify(ticketRepository).findByAsistenteIdOrderByFechaCompraDesc(2L);
        }

        private Ticket ticketMock(Long id, BigDecimal precio) {
            Ticket t = new Ticket();
            t.setId(id);
            t.setEvento(evento);
            t.setAsistente(asistente);
            t.setPrecioFinal(precio);
            t.setEstado(TicketStatus.VALIDO);
            return t;
        }
    }
}