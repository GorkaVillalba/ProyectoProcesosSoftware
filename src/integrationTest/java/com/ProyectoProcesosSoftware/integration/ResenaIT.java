package com.ProyectoProcesosSoftware.integration;

import com.ProyectoProcesosSoftware.dto.CrearResenaDTO;
import com.ProyectoProcesosSoftware.dto.ResenaResponseDTO;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.ResenaRepository;
import com.ProyectoProcesosSoftware.repository.TicketRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import com.ProyectoProcesosSoftware.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * T-26.7: Tests de integración end-to-end del sistema de reseñas.
 *
 * Flujo completo:
 *   1. Registrar asistente
 *   2. Comprar entrada para el evento
 *   3. Crear reseña → 201
 *   4. Consultar listado → GET /api/events/{id}/reviews
 *   5. Consultar media vía GET detalle del evento
 *
 * Contexto Spring Boot completo sobre H2 (perfil dev).
 * La BD se limpia entre tests para aislar el estado.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("dev")
class ResenaIT {

    @Autowired private TestRestTemplate rest;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private ResenaRepository resenaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario organizador;
    private Usuario asistente;
    private Evento evento;

    @BeforeEach
    void setUp() {
        resenaRepository.deleteAll();
        ticketRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();

        organizador = nuevoUsuario("org@resena.com", "Organizador", Rol.ORGANIZADOR);
        asistente   = nuevoUsuario("asistente@resena.com", "Asistente Test", Rol.ASISTENTE);

        evento = new Evento();
        evento.setNombre("Concierto Integración");
        evento.setDescripcion("Evento para test de reseñas");
        evento.setFecha(LocalDate.now().plusMonths(1));
        evento.setHora(LocalTime.of(20, 0));
        evento.setUbicacion("Bilbao");
        evento.setAforoMaximo(50);
        evento.setEntradasVendidas(0);
        evento.setPrecioBase(new BigDecimal("60.00"));
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(organizador);
        evento = eventoRepository.save(evento);
    }

    @AfterEach
    void tearDown() {
        resenaRepository.deleteAll();
        ticketRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    // ─────────────────────────────────────────────────────────────
    // Flujo completo end-to-end
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Flujo completo: comprar entrada → crear reseña → listar → media correcta")
    void flujoCompleto_resena_end2end() {
        // Paso 1: comprar entrada (requisito previo para poder reseñar)
        ResponseEntity<Map> compraResponse = rest.exchange(
                "/api/tickets/eventos/" + evento.getId(),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(asistente)),
                Map.class);
        assertThat(compraResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Paso 2: crear reseña
        CrearResenaDTO crearDTO = new CrearResenaDTO();
        crearDTO.setPuntuacion(5);
        crearDTO.setComentario("Increíble experiencia, muy recomendado");

        ResponseEntity<ResenaResponseDTO> crearResponse = rest.exchange(
                "/api/events/" + evento.getId() + "/reviews",
                HttpMethod.POST,
                new HttpEntity<>(crearDTO, authHeaders(asistente)),
                ResenaResponseDTO.class);

        assertThat(crearResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResenaResponseDTO resenaCreada = crearResponse.getBody();
        assertThat(resenaCreada).isNotNull();
        assertThat(resenaCreada.getId()).isNotNull();
        assertThat(resenaCreada.getEventoId()).isEqualTo(evento.getId());
        assertThat(resenaCreada.getAsistenteId()).isEqualTo(asistente.getId());
        assertThat(resenaCreada.getPuntuacion()).isEqualTo(5);
        assertThat(resenaCreada.getComentario()).isEqualTo("Increíble experiencia, muy recomendado");
        assertThat(resenaCreada.getFechaCreacion()).isNotNull();

        // Paso 3: listar reseñas del evento (GET público, sin token)
        ResponseEntity<PageResponse<ResenaResponseDTO>> listadoResponse = rest.exchange(
                "/api/events/" + evento.getId() + "/reviews",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageResponse<ResenaResponseDTO>>() {});

        assertThat(listadoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listadoResponse.getBody()).isNotNull();
        assertThat(listadoResponse.getBody().getContent()).hasSize(1);
        assertThat(listadoResponse.getBody().getContent().get(0).getPuntuacion()).isEqualTo(5);

        // Paso 4: verificar que la reseña se persiste en BD
        assertThat(resenaRepository.countByEventoId(evento.getId())).isEqualTo(1);
        assertThat(resenaRepository.findMediaPuntuacionByEventoId(evento.getId())).isEqualTo(5.0);
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/events/{id}/reviews — validaciones
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST reseña sin JWT devuelve 403")
    void crearResena_sinToken_403() {
        CrearResenaDTO dto = new CrearResenaDTO();
        dto.setPuntuacion(4);

        ResponseEntity<String> response = rest.exchange(
                "/api/events/" + evento.getId() + "/reviews",
                HttpMethod.POST,
                new HttpEntity<>(dto, new HttpHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST reseña sin ticket válido devuelve 409 o 400")
    void crearResena_sinTicket_error() {
        // El asistente no ha comprado entrada
        CrearResenaDTO dto = new CrearResenaDTO();
        dto.setPuntuacion(3);
        dto.setComentario("No tengo ticket");

        ResponseEntity<String> response = rest.exchange(
                "/api/events/" + evento.getId() + "/reviews",
                HttpMethod.POST,
                new HttpEntity<>(dto, authHeaders(asistente)),
                String.class);

        assertThat(response.getStatusCode())
                .isIn(HttpStatus.BAD_REQUEST, HttpStatus.CONFLICT, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("POST reseña duplicada devuelve 409")
    void crearResena_duplicada_409() {
        // Comprar entrada
        rest.exchange("/api/tickets/eventos/" + evento.getId(),
                HttpMethod.POST, new HttpEntity<>(authHeaders(asistente)), Map.class);

        // Primera reseña — debe ir bien
        CrearResenaDTO dto = new CrearResenaDTO();
        dto.setPuntuacion(4);
        dto.setComentario("Primera reseña");
        ResponseEntity<ResenaResponseDTO> primera = rest.exchange(
                "/api/events/" + evento.getId() + "/reviews",
                HttpMethod.POST,
                new HttpEntity<>(dto, authHeaders(asistente)),
                ResenaResponseDTO.class);
        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Segunda reseña para el mismo evento — debe fallar
        CrearResenaDTO dto2 = new CrearResenaDTO();
        dto2.setPuntuacion(2);
        dto2.setComentario("Intento duplicado");
        ResponseEntity<String> segunda = rest.exchange(
                "/api/events/" + evento.getId() + "/reviews",
                HttpMethod.POST,
                new HttpEntity<>(dto2, authHeaders(asistente)),
                String.class);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("POST reseña con puntuación inválida (0) devuelve 400")
    void crearResena_puntuacionInvalida_400() {
        rest.exchange("/api/tickets/eventos/" + evento.getId(),
                HttpMethod.POST, new HttpEntity<>(authHeaders(asistente)), Map.class);

        CrearResenaDTO dto = new CrearResenaDTO();
        dto.setPuntuacion(0);

        ResponseEntity<String> response = rest.exchange(
                "/api/events/" + evento.getId() + "/reviews",
                HttpMethod.POST,
                new HttpEntity<>(dto, authHeaders(asistente)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/events/{id}/reviews — listado público
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET listado reseñas sin token devuelve 200 (endpoint público)")
    void listarResenas_sinToken_200() {
        ResponseEntity<String> response = rest.exchange(
                "/api/events/" + evento.getId() + "/reviews",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET listado reseñas con múltiples autores devuelve todas paginadas")
    void listarResenas_multipleAutores_devuelveTodasPaginadas() {
        // Segundo asistente
        Usuario asistente2 = nuevoUsuario("asistente2@resena.com", "Asistente 2", Rol.ASISTENTE);

        // Ambos compran entrada
        rest.exchange("/api/tickets/eventos/" + evento.getId(),
                HttpMethod.POST, new HttpEntity<>(authHeaders(asistente)), Map.class);
        rest.exchange("/api/tickets/eventos/" + evento.getId(),
                HttpMethod.POST, new HttpEntity<>(authHeaders(asistente2)), Map.class);

        // Ambos dejan reseña
        CrearResenaDTO dto1 = new CrearResenaDTO();
        dto1.setPuntuacion(5); dto1.setComentario("Genial");
        rest.exchange("/api/events/" + evento.getId() + "/reviews",
                HttpMethod.POST, new HttpEntity<>(dto1, authHeaders(asistente)),
                ResenaResponseDTO.class);

        CrearResenaDTO dto2 = new CrearResenaDTO();
        dto2.setPuntuacion(3); dto2.setComentario("Aceptable");
        rest.exchange("/api/events/" + evento.getId() + "/reviews",
                HttpMethod.POST, new HttpEntity<>(dto2, authHeaders(asistente2)),
                ResenaResponseDTO.class);

        // Listar
        ResponseEntity<PageResponse<ResenaResponseDTO>> response = rest.exchange(
                "/api/events/" + evento.getId() + "/reviews",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageResponse<ResenaResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(2);

        // Verificar media en repositorio: (5+3)/2 = 4.0
        Double media = resenaRepository.findMediaPuntuacionByEventoId(evento.getId());
        assertThat(media).isEqualTo(4.0);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private Usuario nuevoUsuario(String email, String nombre, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("Password123!"));
        u.setRol(rol);
        return usuarioRepository.save(u);
    }

    private HttpHeaders authHeaders(Usuario u) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generarToken(u.getId(), u.getEmail(), u.getRol().name()));
        return h;
    }

    /**
     * Wrapper simple para deserializar la respuesta paginada de Spring
     * (Spring's Page no tiene constructor público, se necesita un DTO propio).
     */
    static class PageResponse<T> {
        private java.util.List<T> content;
        private int totalPages;
        private long totalElements;
        private int number;

        public java.util.List<T> getContent() { return content; }
        public void setContent(java.util.List<T> content) { this.content = content; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        public int getNumber() { return number; }
        public void setNumber(int number) { this.number = number; }
    }
}