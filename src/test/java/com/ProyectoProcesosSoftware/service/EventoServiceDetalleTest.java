package com.ProyectoProcesosSoftware.service;
import com.ProyectoProcesosSoftware.dto.EventoResponseDTO;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
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
import static org.mockito.Mockito.*;
/**
 * T-21 (Persona 2): Tests de obtenerDetalle()
 */
@ExtendWith(MockitoExtension.class)
class EventoServiceDetalleTest {
    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private EventoService eventoService;
    private Evento evento;
    private Usuario organizador;
    @BeforeEach
    void setUp() {
        organizador = new Usuario();
        organizador.setId(1L);
        organizador.setNombre("María");
        organizador.setRol(Rol.ORGANIZADOR);
        evento = new Evento();
        evento.setId(1L);
        evento.setNombre("Concierto Rock");
        evento.setDescripcion("Gran concierto");
        evento.setFecha(LocalDate.of(2027, 6, 15));
        evento.setHora(LocalTime.of(20, 0));
        evento.setUbicacion("Bilbao Arena");
        evento.setAforoMaximo(500);
        evento.setEntradasVendidas(150);
        evento.setPrecioBase(new BigDecimal("30.00"));
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(organizador);
    }
    @Test
    @DisplayName("Obtener detalle exitoso - devuelve EventoResponseDTO")
    void obtenerDetalle_eventoExiste_devuelveDTO() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        EventoResponseDTO resultado = eventoService.obtenerDetalle(1L);
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Concierto Rock");
        assertThat(resultado.getAforoMaximo()).isEqualTo(500);
        assertThat(resultado.getEntradasVendidas()).isEqualTo(150);
        assertThat(resultado.getPlazasDisponibles()).isEqualTo(350);
        verify(eventoRepository).findById(1L);
    }
    @Test
    @DisplayName("Evento no existente - lanza ResourceNotFoundException 404")
    void obtenerDetalle_eventoNoExiste_lanzaExcepcion() {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> eventoService.obtenerDetalle(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Evento no encontrado");
    }
    @Test
    @DisplayName("Plazas disponibles se calculan correctamente")
    void obtenerDetalle_plazasDisponibles_calculoCorrecto() {
        evento.setAforoMaximo(1000);
        evento.setEntradasVendidas(750);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        EventoResponseDTO resultado = eventoService.obtenerDetalle(1L);
        assertThat(resultado.getPlazasDisponibles()).isEqualTo(250);
    }
}