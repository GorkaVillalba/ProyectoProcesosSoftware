package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.*;
import com.ProyectoProcesosSoftware.exception.*;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.pricing.PricingContext;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.anyInt;



import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

// T-17/T-22/T-24 tests (Persona 6)
@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PricingContext pricingContext;
    @InjectMocks private EventoService eventoService;

    private Usuario organizador;
    private Evento evento;
    private CrearEventoDTO crearDTO;
    private EditarEventoDTO editarDTO;

    @BeforeEach
    void setUp() {
        organizador = new Usuario();
        organizador.setId(1L);
        organizador.setNombre("María");
        organizador.setRol(Rol.ORGANIZADOR);

        evento = new Evento();
        evento.setId(1L);
        evento.setNombre("Concierto");
        evento.setFecha(LocalDate.of(2025,6,15));
        evento.setHora(LocalTime.of(20,0));
        evento.setUbicacion("Bilbao");
        evento.setAforoMaximo(500);
        evento.setEntradasVendidas(50);
        evento.setPrecioBase(new BigDecimal("30"));
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(organizador);

        crearDTO = new CrearEventoDTO();
        crearDTO.setNombre("Festival");
        crearDTO.setFecha(LocalDate.of(2025,7,20));
        crearDTO.setHora(LocalTime.of(19,0));
        crearDTO.setUbicacion("Plaza");
        crearDTO.setAforoMaximo(200);
        crearDTO.setPrecioBase(new BigDecimal("25"));

        editarDTO = new EditarEventoDTO();
        editarDTO.setNombre("Editado");
        editarDTO.setFecha(LocalDate.of(2025,6,20));
        editarDTO.setHora(LocalTime.of(21,0));
        editarDTO.setUbicacion("VIP");
        editarDTO.setAforoMaximo(600);
        editarDTO.setPrecioBase(new BigDecimal("35"));

        lenient().when(pricingContext.calcularPrecio(any(), anyInt(), anyInt()))
            .thenReturn(new BigDecimal("100.00"));
        lenient().when(pricingContext.nombreEstrategia(anyInt(), anyInt()))
            .thenReturn("EarlyBird");
    }

    @Test void crear_exitoso() {
        Evento guardado = new Evento();
        guardado.setId(10L); guardado.setNombre("Festival");
        guardado.setFecha(crearDTO.getFecha()); guardado.setHora(crearDTO.getHora());
        guardado.setUbicacion("Plaza"); guardado.setAforoMaximo(200);
        guardado.setEntradasVendidas(0); guardado.setPrecioBase(new BigDecimal("25"));
        guardado.setEstado(EstadoEvento.BORRADOR); guardado.setOrganizador(organizador);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));
        when(eventoRepository.save(any())).thenReturn(guardado);
        EventoResponseDTO r = eventoService.crearEvento(crearDTO, 1L);
        assertThat(r.getEstado()).isEqualTo("BORRADOR");
    }

    @Test void crear_asistente_403() {
        Usuario asist = new Usuario(); asist.setId(2L); asist.setRol(Rol.ASISTENTE);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(asist));
        assertThatThrownBy(() -> eventoService.crearEvento(crearDTO, 2L))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test void editar_exitoso() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        EventoResponseDTO r = eventoService.editarEvento(1L, editarDTO, 1L);
        assertThat(r.getNombre()).isEqualTo("Editado");
    }

    @Test void editar_ajeno_403() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        assertThatThrownBy(() -> eventoService.editarEvento(1L, editarDTO, 99L))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test void editar_reducirAforo_400() {
        editarDTO.setAforoMaximo(30);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        assertThatThrownBy(() -> eventoService.editarEvento(1L, editarDTO, 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test void eliminar_sinVentas() {
        Evento vacio = new Evento(); vacio.setId(2L); vacio.setEntradasVendidas(0);
        vacio.setOrganizador(organizador);
        when(eventoRepository.findById(2L)).thenReturn(Optional.of(vacio));
        eventoService.eliminarEvento(2L, 1L);
        verify(eventoRepository).delete(vacio);
    }

    @Test void eliminar_conVentas_409() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        assertThatThrownBy(() -> eventoService.eliminarEvento(1L, 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test void eliminar_ajeno_403() {
        Evento vacio = new Evento(); vacio.setId(2L); vacio.setEntradasVendidas(0);
        vacio.setOrganizador(organizador);
        when(eventoRepository.findById(2L)).thenReturn(Optional.of(vacio));
        assertThatThrownBy(() -> eventoService.eliminarEvento(2L, 99L))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    @DisplayName("Crear evento con usuario no encontrado lanza ResourceNotFoundException")
    void crear_usuarioNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.crearEvento(crearDTO, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("Listar eventos devuelve página de resultados")
    void listarEventos_devuelvePagina() {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);

        org.springframework.data.domain.Page<Evento> paginaMock =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(evento));

        when(eventoRepository.findByEstadoAndFiltros(
                EstadoEvento.PUBLICADO, null, null, pageable))
                .thenReturn(paginaMock);

        org.springframework.data.domain.Page<EventoResponseDTO> resultado =
                eventoService.listarEventos(null, null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNombre()).isEqualTo("Concierto");
    }

    @Test
    @DisplayName("Listar eventos con filtros aplica nombre y ubicacion")
    void listarEventos_conFiltros() {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);

        org.springframework.data.domain.Page<Evento> paginaMock =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(evento));

        when(eventoRepository.findByEstadoAndFiltros(
                EstadoEvento.PUBLICADO, "Concierto", "Bilbao", pageable))
                .thenReturn(paginaMock);

        org.springframework.data.domain.Page<EventoResponseDTO> resultado =
                eventoService.listarEventos("Concierto", "Bilbao", pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getUbicacion()).isEqualTo("Bilbao");
    }

    @Test
    @DisplayName("Listar eventos sin resultados devuelve página vacía")
    void listarEventos_sinResultados() {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);

        when(eventoRepository.findByEstadoAndFiltros(
                EstadoEvento.PUBLICADO, "inexistente", null, pageable))
                .thenReturn(org.springframework.data.domain.Page.empty());

        org.springframework.data.domain.Page<EventoResponseDTO> resultado =
                eventoService.listarEventos("inexistente", null, pageable);

        assertThat(resultado.getContent()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────
    // obtenerPrecio — cubre switch (Base / +25% / +50%), 404 y aforo 0
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerPrecio con evento no existente lanza ResourceNotFoundException")
    void obtenerPrecio_eventoNoExiste_lanza404() {
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.obtenerPrecio(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Evento");
    }

    @Test
    @DisplayName("obtenerPrecio EarlyBird devuelve nivel Base y % de ocupación correcto")
    void obtenerPrecio_earlyBird_nivelBase() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(pricingContext.nombreEstrategia(anyInt(), anyInt())).thenReturn("EarlyBird");
        when(pricingContext.calcularPrecio(any(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("30.00"));

        PrecioEventoDTO dto = eventoService.obtenerPrecio(1L);

        assertThat(dto.getEventoId()).isEqualTo(1L);
        assertThat(dto.getEstrategia()).isEqualTo("EarlyBird");
        assertThat(dto.getNivel()).isEqualTo("Base");
        assertThat(dto.getPrecioBase()).isEqualByComparingTo("30");
        assertThat(dto.getPrecioActual()).isEqualByComparingTo("30.00");
        // entradasVendidas=50, aforo=500 -> 10%
        assertThat(dto.getPorcentajeOcupacion()).isEqualTo(10);
    }

    @Test
    @DisplayName("obtenerPrecio Regular devuelve nivel +25%")
    void obtenerPrecio_regular_nivelMas25() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(pricingContext.nombreEstrategia(anyInt(), anyInt())).thenReturn("Regular");
        when(pricingContext.calcularPrecio(any(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("37.50"));

        PrecioEventoDTO dto = eventoService.obtenerPrecio(1L);

        assertThat(dto.getEstrategia()).isEqualTo("Regular");
        assertThat(dto.getNivel()).isEqualTo("+25%");
        assertThat(dto.getPrecioActual()).isEqualByComparingTo("37.50");
    }

    @Test
    @DisplayName("obtenerPrecio LastMinute devuelve nivel +50%")
    void obtenerPrecio_lastMinute_nivelMas50() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(pricingContext.nombreEstrategia(anyInt(), anyInt())).thenReturn("LastMinute");
        when(pricingContext.calcularPrecio(any(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("45.00"));

        PrecioEventoDTO dto = eventoService.obtenerPrecio(1L);

        assertThat(dto.getEstrategia()).isEqualTo("LastMinute");
        assertThat(dto.getNivel()).isEqualTo("+50%");
        assertThat(dto.getPrecioActual()).isEqualByComparingTo("45.00");
    }

    @Test
    @DisplayName("obtenerPrecio con aforo 0 devuelve porcentaje de ocupación 0")
    void obtenerPrecio_aforoCero_porcentajeCero() {
        evento.setAforoMaximo(0);
        evento.setEntradasVendidas(0);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(pricingContext.nombreEstrategia(anyInt(), anyInt())).thenReturn("EarlyBird");
        when(pricingContext.calcularPrecio(any(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("30.00"));

        PrecioEventoDTO dto = eventoService.obtenerPrecio(1L);

        assertThat(dto.getPorcentajeOcupacion()).isEqualTo(0);
        assertThat(dto.getNivel()).isEqualTo("Base");
    }
}