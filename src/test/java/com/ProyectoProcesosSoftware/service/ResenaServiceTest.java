package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.CrearResenaDTO;
import com.ProyectoProcesosSoftware.dto.ResenaResponseDTO;
import com.ProyectoProcesosSoftware.exception.BusinessRuleException;
import com.ProyectoProcesosSoftware.exception.DuplicateResourceException;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.exception.UnauthorizedActionException;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.ResenaRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * T-26.6: Tests unitarios del servicio de reseñas.
 *
 * Cubre los escenarios exigidos por la tarea:
 *  - crearResena_exitoso_devuelveDTO
 *  - crearResena_sinTicket_lanzaBusinessRule
 *  - crearResena_duplicada_lanzaBusinessRule
 *  - crearResena_puntuacionFueraDeRango_lanzaValidacion
 *  - obtenerMediaEvento_sinResenas_devuelveCero
 */
@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    @Mock private ResenaRepository resenaRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TicketRepository ticketRepository;
    @InjectMocks private ResenaService resenaService;

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
        evento.setNombre("Concierto Test");
        evento.setFecha(LocalDate.of(2025, 6, 1));
        evento.setHora(LocalTime.of(20, 0));
        evento.setUbicacion("Bilbao");
        evento.setAforoMaximo(100);
        evento.setEntradasVendidas(30);
        evento.setPrecioBase(new BigDecimal("50.00"));
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(organizador);

        asistente = new Usuario();
        asistente.setId(2L);
        asistente.setNombre("Juan Asistente");
        asistente.setRol(Rol.ASISTENTE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // crearResena — casos de éxito
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("crearResena — casos de éxito")
    class CrearResenaExitoso {

        @Test
        @DisplayName("crearResena_exitoso_devuelveDTO: reseña válida devuelve DTO con todos los campos")
        void crearResena_exitoso_devuelveDTO() {
            CrearResenaDTO dto = new CrearResenaDTO();
            dto.setPuntuacion(5);
            dto.setComentario("Excelente evento");

            stubCrearValida(dto);

            ResenaResponseDTO result = resenaService.crearResena(1L, 2L, dto);

            assertThat(result).isNotNull();
            assertThat(result.getEventoId()).isEqualTo(1L);
            assertThat(result.getEventoNombre()).isEqualTo("Concierto Test");
            assertThat(result.getAsistenteId()).isEqualTo(2L);
            assertThat(result.getAsistenteNombre()).isEqualTo("Juan Asistente");
            assertThat(result.getPuntuacion()).isEqualTo(5);
            assertThat(result.getComentario()).isEqualTo("Excelente evento");
            assertThat(result.getFechaCreacion()).isNotNull();
        }

        @Test
        @DisplayName("crearResena_exitoso: se persiste la Resena con los valores correctos")
        void crearResena_exitoso_persisteResena() {
            CrearResenaDTO dto = new CrearResenaDTO();
            dto.setPuntuacion(3);
            dto.setComentario("Bien");

            stubCrearValida(dto);

            resenaService.crearResena(1L, 2L, dto);

            ArgumentCaptor<Resena> captor = ArgumentCaptor.forClass(Resena.class);
            verify(resenaRepository).save(captor.capture());
            Resena guardada = captor.getValue();

            assertThat(guardada.getPuntuacion()).isEqualTo(3);
            assertThat(guardada.getComentario()).isEqualTo("Bien");
            assertThat(guardada.getEvento()).isSameAs(evento);
            assertThat(guardada.getAsistente()).isSameAs(asistente);
        }

        private void stubCrearValida(CrearResenaDTO dto) {
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(ticketRepository.existsByEventoIdAndAsistenteIdAndEstado(
                    1L, 2L, TicketStatus.VALIDO)).thenReturn(true);
            when(resenaRepository.existsByEventoIdAndAsistenteId(1L, 2L)).thenReturn(false);
            when(resenaRepository.save(any())).thenAnswer(inv -> {
                Resena r = inv.getArgument(0);
                r.setId(10L);
                r.setFechaCreacion(java.time.LocalDateTime.now());
                return r;
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // crearResena — validaciones y errores
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("crearResena — validaciones y errores")
    class CrearResenaErrores {

        @Test
        @DisplayName("crearResena_sinTicket_lanzaBusinessRule: asistente sin ticket válido lanza BusinessRuleException")
        void crearResena_sinTicket_lanzaBusinessRule() {
            CrearResenaDTO dto = dtoConPuntuacion(4);

            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(ticketRepository.existsByEventoIdAndAsistenteIdAndEstado(
                    1L, 2L, TicketStatus.VALIDO)).thenReturn(false);

            assertThatThrownBy(() -> resenaService.crearResena(1L, 2L, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("entrada válida");

            verify(resenaRepository, never()).save(any());
        }

        @Test
        @DisplayName("crearResena_duplicada_lanzaBusinessRule: reseña ya existente lanza DuplicateResourceException")
        void crearResena_duplicada_lanzaBusinessRule() {
            CrearResenaDTO dto = dtoConPuntuacion(4);

            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(ticketRepository.existsByEventoIdAndAsistenteIdAndEstado(
                    1L, 2L, TicketStatus.VALIDO)).thenReturn(true);
            when(resenaRepository.existsByEventoIdAndAsistenteId(1L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> resenaService.crearResena(1L, 2L, dto))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("reseña");

            verify(resenaRepository, never()).save(any());
        }

        @Test
        @DisplayName("crearResena_puntuacionFueraDeRango_lanzaValidacion: puntuacion=0 lanza BusinessRuleException")
        void crearResena_puntuacionCero_lanzaValidacion() {
            CrearResenaDTO dto = dtoConPuntuacion(0);

            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(ticketRepository.existsByEventoIdAndAsistenteIdAndEstado(
                    1L, 2L, TicketStatus.VALIDO)).thenReturn(true);
            when(resenaRepository.existsByEventoIdAndAsistenteId(1L, 2L)).thenReturn(false);

            assertThatThrownBy(() -> resenaService.crearResena(1L, 2L, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("1 y 5");

            verify(resenaRepository, never()).save(any());
        }

        @Test
        @DisplayName("crearResena_puntuacionFueraDeRango_lanzaValidacion: puntuacion=6 lanza BusinessRuleException")
        void crearResena_puntuacionSeis_lanzaValidacion() {
            CrearResenaDTO dto = dtoConPuntuacion(6);

            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(ticketRepository.existsByEventoIdAndAsistenteIdAndEstado(
                    1L, 2L, TicketStatus.VALIDO)).thenReturn(true);
            when(resenaRepository.existsByEventoIdAndAsistenteId(1L, 2L)).thenReturn(false);

            assertThatThrownBy(() -> resenaService.crearResena(1L, 2L, dto))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("1 y 5");

            verify(resenaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Usuario no ASISTENTE lanza UnauthorizedActionException")
        void crearResena_usuarioOrganizador_lanzaUnauthorized() {
            asistente.setRol(Rol.ORGANIZADOR);
            CrearResenaDTO dto = dtoConPuntuacion(3);

            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));

            assertThatThrownBy(() -> resenaService.crearResena(1L, 2L, dto))
                    .isInstanceOf(UnauthorizedActionException.class)
                    .hasMessageContaining("asistentes");

            verify(resenaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Evento no encontrado lanza ResourceNotFoundException")
        void crearResena_eventoNoExiste_404() {
            CrearResenaDTO dto = dtoConPuntuacion(3);

            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asistente));
            when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resenaService.crearResena(999L, 2L, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Evento");

            verify(resenaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Usuario no encontrado lanza ResourceNotFoundException")
        void crearResena_usuarioNoExiste_404() {
            CrearResenaDTO dto = dtoConPuntuacion(3);

            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resenaService.crearResena(1L, 99L, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuario");

            verify(resenaRepository, never()).save(any());
        }

        private CrearResenaDTO dtoConPuntuacion(int puntuacion) {
            CrearResenaDTO dto = new CrearResenaDTO();
            dto.setPuntuacion(puntuacion);
            dto.setComentario("Comentario test");
            return dto;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listarResenasEvento
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listarResenasEvento")
    class ListarResenas {

        @Test
        @DisplayName("Devuelve página de DTOs mapeados correctamente")
        void listarResenas_devuelvePaginaDTOs() {
            Resena r = resenaFake(10L, 5, "Muy bueno");
            Pageable pageable = PageRequest.of(0, 10);
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(resenaRepository.findByEventoId(1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(r)));

            Page<ResenaResponseDTO> result = resenaService.listarResenasEvento(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getPuntuacion()).isEqualTo(5);
            assertThat(result.getContent().get(0).getComentario()).isEqualTo("Muy bueno");
        }

        @Test
        @DisplayName("Evento inexistente lanza ResourceNotFoundException")
        void listarResenas_eventoNoExiste_404() {
            when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resenaService.listarResenasEvento(999L, Pageable.unpaged()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // obtenerMediaEvento
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerMediaEvento")
    class ObtenerMedia {

        @Test
        @DisplayName("obtenerMediaEvento_sinResenas_devuelveCero: AVG null → devuelve 0.0")
        void obtenerMediaEvento_sinResenas_devuelveCero() {
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(resenaRepository.findMediaPuntuacionByEventoId(1L)).thenReturn(null);

            Double media = resenaService.obtenerMediaEvento(1L);

            assertThat(media).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Con reseñas devuelve el promedio calculado por el repositorio")
        void obtenerMediaEvento_conResenas_devuelveMedia() {
            when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
            when(resenaRepository.findMediaPuntuacionByEventoId(1L)).thenReturn(4.5);

            Double media = resenaService.obtenerMediaEvento(1L);

            assertThat(media).isEqualTo(4.5);
        }

        @Test
        @DisplayName("Evento inexistente lanza ResourceNotFoundException")
        void obtenerMediaEvento_eventoNoExiste_404() {
            when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resenaService.obtenerMediaEvento(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Resena resenaFake(Long id, int puntuacion, String comentario) {
        Resena r = new Resena();
        r.setId(id);
        r.setEvento(evento);
        r.setAsistente(asistente);
        r.setPuntuacion(puntuacion);
        r.setComentario(comentario);
        r.setFechaCreacion(java.time.LocalDateTime.now());
        return r;
    }
}