package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.EventoResponseDTO;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.pricing.PricingContext;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.FavoritoRepository;
import com.ProyectoProcesosSoftware.repository.ResenaRepository;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * T-27.5: Tests unitarios de FavoritoService.
 * Cubre: agregar, agregar duplicado (idempotente), quitar, quitar inexistente (idempotente), listar.
 */
@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock private FavoritoRepository favoritoRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ResenaRepository resenaRepository;
    @Mock private PricingContext pricingContext;
    @InjectMocks private FavoritoService favoritoService;

    private Usuario usuario;
    private Evento evento;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Usuario Test");
        usuario.setRol(Rol.ASISTENTE);

        Usuario organizador = new Usuario();
        organizador.setId(99L);
        organizador.setNombre("Org");
        organizador.setRol(Rol.ORGANIZADOR);

        evento = new Evento();
        evento.setId(10L);
        evento.setNombre("Evento Test");
        evento.setDescripcion("Descripción");
        evento.setFecha(LocalDate.now().plusMonths(1));
        evento.setHora(LocalTime.of(20, 0));
        evento.setUbicacion("Bilbao");
        evento.setAforoMaximo(100);
        evento.setEntradasVendidas(20);
        evento.setPrecioBase(new BigDecimal("50.00"));
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(organizador);
    }

    // ─── agregar ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("agregar")
    class Agregar {

        @Test
        @DisplayName("agregar_nuevo: persiste el favorito si no existía")
        void agregar_nuevo_persisteFavorito() {
            when(favoritoRepository.existsByUsuarioIdAndEventoId(1L, 10L)).thenReturn(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
            when(favoritoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            favoritoService.agregar(1L, 10L);

            ArgumentCaptor<Favorito> captor = ArgumentCaptor.forClass(Favorito.class);
            verify(favoritoRepository).save(captor.capture());
            assertThat(captor.getValue().getUsuario()).isSameAs(usuario);
            assertThat(captor.getValue().getEvento()).isSameAs(evento);
        }

        @Test
        @DisplayName("agregar_duplicado_esIdempotente: no lanza error ni guarda de nuevo si ya existe")
        void agregar_duplicado_esIdempotente() {
            when(favoritoRepository.existsByUsuarioIdAndEventoId(1L, 10L)).thenReturn(true);

            favoritoService.agregar(1L, 10L);

            verify(favoritoRepository, never()).save(any());
            verify(usuarioRepository, never()).findById(any());
            verify(eventoRepository, never()).findById(any());
        }

        @Test
        @DisplayName("agregar_usuarioNoExiste: lanza ResourceNotFoundException")
        void agregar_usuarioNoExiste_lanzaException() {
            when(favoritoRepository.existsByUsuarioIdAndEventoId(1L, 10L)).thenReturn(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> favoritoService.agregar(1L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuario");
            verify(favoritoRepository, never()).save(any());
        }

        @Test
        @DisplayName("agregar_eventoNoExiste: lanza ResourceNotFoundException")
        void agregar_eventoNoExiste_lanzaException() {
            when(favoritoRepository.existsByUsuarioIdAndEventoId(1L, 10L)).thenReturn(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(eventoRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> favoritoService.agregar(1L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Evento");
            verify(favoritoRepository, never()).save(any());
        }
    }

    // ─── quitar ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quitar")
    class Quitar {

        @Test
        @DisplayName("quitar_existente: llama a deleteByUsuarioIdAndEventoId")
        void quitar_existente_eliminaFavorito() {
            when(favoritoRepository.existsByUsuarioIdAndEventoId(1L, 10L)).thenReturn(true);

            favoritoService.quitar(1L, 10L);

            verify(favoritoRepository).deleteByUsuarioIdAndEventoId(1L, 10L);
        }

        @Test
        @DisplayName("quitar_inexistente_esIdempotente: no lanza error si no existía")
        void quitar_inexistente_esIdempotente() {
            when(favoritoRepository.existsByUsuarioIdAndEventoId(1L, 10L)).thenReturn(false);

            favoritoService.quitar(1L, 10L);

            verify(favoritoRepository, never()).deleteByUsuarioIdAndEventoId(any(), any());
        }
    }

    // ─── listarFavoritos ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("listarFavoritos")
    class ListarFavoritos {

        @Test
        @DisplayName("listarFavoritos: devuelve lista de EventoResponseDTO mapeada correctamente")
        void listarFavoritos_devuelveListaDTOs() {
            Favorito fav = new Favorito();
            fav.setId(1L);
            fav.setUsuario(usuario);
            fav.setEvento(evento);
            fav.setFechaAlta(LocalDateTime.now());

            when(favoritoRepository.findByUsuarioId(1L)).thenReturn(List.of(fav));
            when(resenaRepository.findMediaPuntuacionByEventoId(10L)).thenReturn(4.5);
            when(resenaRepository.countByEventoId(10L)).thenReturn(3L);
            when(pricingContext.calcularPrecio(any(), anyInt(), anyInt()))
                    .thenReturn(new BigDecimal("50.00"));
            when(pricingContext.nombreEstrategia(anyInt(), anyInt())).thenReturn("EarlyBird");

            List<EventoResponseDTO> result = favoritoService.listarFavoritos(1L);

            assertThat(result).hasSize(1);
            EventoResponseDTO dto = result.get(0);
            assertThat(dto.getId()).isEqualTo(10L);
            assertThat(dto.getNombre()).isEqualTo("Evento Test");
            assertThat(dto.getNumeroResenas()).isEqualTo(3L);
            assertThat(dto.getPuntuacionMedia()).isEqualByComparingTo("4.50");
        }

        @Test
        @DisplayName("listarFavoritos_sinFavoritos: devuelve lista vacía")
        void listarFavoritos_sinFavoritos_devuelveListaVacia() {
            when(favoritoRepository.findByUsuarioId(1L)).thenReturn(List.of());

            List<EventoResponseDTO> result = favoritoService.listarFavoritos(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("listarFavoritos_sinResenas: puntuacionMedia es 0.0 y numeroResenas es 0")
        void listarFavoritos_sinResenas_mediaEsCero() {
            Favorito fav = new Favorito();
            fav.setUsuario(usuario);
            fav.setEvento(evento);
            fav.setFechaAlta(LocalDateTime.now());

            when(favoritoRepository.findByUsuarioId(1L)).thenReturn(List.of(fav));
            when(resenaRepository.findMediaPuntuacionByEventoId(10L)).thenReturn(null);
            when(resenaRepository.countByEventoId(10L)).thenReturn(0L);
            when(pricingContext.calcularPrecio(any(), anyInt(), anyInt()))
                    .thenReturn(new BigDecimal("50.00"));
            when(pricingContext.nombreEstrategia(anyInt(), anyInt())).thenReturn("EarlyBird");

            List<EventoResponseDTO> result = favoritoService.listarFavoritos(1L);

            assertThat(result.get(0).getPuntuacionMedia()).isEqualByComparingTo("0.00");
            assertThat(result.get(0).getNumeroResenas()).isEqualTo(0L);
        }
    }
}