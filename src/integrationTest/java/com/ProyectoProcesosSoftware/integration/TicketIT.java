package com.ProyectoProcesosSoftware.integration;

import com.ProyectoProcesosSoftware.dto.TicketResponseDTO;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
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
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * T-22: Tests de integración de los endpoints REST de entradas.
 *
 * Arrancan el contexto completo de Spring Boot sobre H2 en memoria
 * (perfil dev) y hacen llamadas HTTP reales con TestRestTemplate.
 * La BD se limpia entre tests para aislar el estado.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("dev")
class TicketIT {

    @Autowired private TestRestTemplate rest;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario organizador;
    private Usuario asistente;
    private Evento evento;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();

        organizador = nuevoUsuario("org@test.com", "Organizador", Rol.ORGANIZADOR);
        asistente   = nuevoUsuario("asistente@test.com", "Asistente", Rol.ASISTENTE);

        evento = new Evento();
        evento.setNombre("Festival IT");
        evento.setDescripcion("Test de integración");
        evento.setFecha(LocalDate.now().plusMonths(2));
        evento.setHora(LocalTime.of(20, 0));
        evento.setUbicacion("Bilbao");
        evento.setAforoMaximo(5);
        evento.setEntradasVendidas(0);
        evento.setPrecioBase(new BigDecimal("100.00"));
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(organizador);
        evento = eventoRepository.save(evento);
    }

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/tickets/eventos/{eventoId}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST compra: devuelve 201 y persiste la entrada")
    void compra_exitosa_end2end() {
        HttpHeaders headers = authHeaders(asistente);
        ResponseEntity<TicketResponseDTO> response = rest.exchange(
                "/api/tickets/eventos/" + evento.getId(),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                TicketResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUuid()).isNotBlank();
        assertThat(response.getBody().getEstado()).isEqualTo(TicketStatus.VALIDO);
        assertThat(response.getBody().getPrecioFinal()).isEqualByComparingTo("100.00");
        assertThat(ticketRepository.findByAsistenteId(asistente.getId())).hasSize(1);
        assertThat(eventoRepository.findById(evento.getId()).get().getEntradasVendidas()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST compra sin JWT: devuelve 403")
    void compra_sinToken_403() {
        ResponseEntity<String> response = rest.exchange(
                "/api/tickets/eventos/" + evento.getId(),
                HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/tickets/my
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /my: devuelve las entradas del usuario autenticado")
    void getMisEntradas_end2end() {
        // Compra en el evento del setUp
        comprarDirecto(asistente);

        // Crea un segundo evento y compra otra entrada
        Evento segundo = new Evento();
        segundo.setNombre("Otro festival");
        segundo.setDescripcion("Segundo evento");
        segundo.setFecha(LocalDate.now().plusMonths(2));
        segundo.setHora(LocalTime.of(21, 0));
        segundo.setUbicacion("San Sebastián");
        segundo.setAforoMaximo(5);
        segundo.setEntradasVendidas(0);
        segundo.setPrecioBase(new BigDecimal("80.00"));
        segundo.setEstado(EstadoEvento.PUBLICADO);
        segundo.setOrganizador(organizador);
        segundo = eventoRepository.save(segundo);

        rest.exchange(
                "/api/tickets/eventos/" + segundo.getId(),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(asistente)),
                TicketResponseDTO.class);

        ResponseEntity<List<TicketResponseDTO>> response = rest.exchange(
                "/api/tickets/my",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(asistente)),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody())
                .allMatch(t -> t.getAsistenteId().equals(asistente.getId()));
    }

    @Test
    @DisplayName("GET /my: usuario sin entradas devuelve lista vacía")
    void getMisEntradas_listaVacia() {
        ResponseEntity<List<TicketResponseDTO>> response = rest.exchange(
                "/api/tickets/my",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(asistente)),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────
    // Control de aforo end-to-end
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Control de aforo: al vender todas las plazas el evento pasa a AGOTADO y la siguiente compra 409")
    void controlAforo_hastaAgotar() {
        int aforo = evento.getAforoMaximo();
        for (int i = 0; i < aforo; i++) {
            Usuario comprador = nuevoUsuario("user" + i + "@test.com", "User " + i, Rol.ASISTENTE);
            ResponseEntity<TicketResponseDTO> r = rest.exchange(
                    "/api/tickets/eventos/" + evento.getId(),
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(comprador)),
                    TicketResponseDTO.class);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        Evento eventoFinal = eventoRepository.findById(evento.getId()).get();
        assertThat(eventoFinal.getEntradasVendidas()).isEqualTo(aforo);
        assertThat(eventoFinal.getEstado()).isEqualTo(EstadoEvento.AGOTADO);

        Usuario tardeon = nuevoUsuario("tardeon@test.com", "Tardeon", Rol.ASISTENTE);
        ResponseEntity<String> response = rest.exchange(
                "/api/tickets/eventos/" + evento.getId(),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(tardeon)),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ─────────────────────────────────────────────────────────────
    // Precio dinámico end-to-end
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Precio dinámico: el precio sube conforme avanza la ocupación")
    void precioDinamico_end2end() {
        evento.setAforoMaximo(10);
        evento = eventoRepository.save(evento);

        TicketResponseDTO primera = comprarDirecto(nuevoUsuario("u1@test.com", "U1", Rol.ASISTENTE));
        assertThat(primera.getPrecioFinal()).isEqualByComparingTo("100.00");
        assertThat(primera.getEstrategiaPrecio()).isEqualTo("EarlyBird");

        for (int i = 0; i < 7; i++) {                    // antes: < 6
            comprarDirecto(nuevoUsuario("m" + i + "@test.com", "M" + i, Rol.ASISTENTE));
        }

        TicketResponseDTO intermedia = comprarDirecto(nuevoUsuario("u2@test.com", "U2", Rol.ASISTENTE));
        assertThat(intermedia.getEstrategiaPrecio()).isIn("Regular", "LastMinute");
        assertThat(intermedia.getPrecioFinal()).isGreaterThan(new BigDecimal("100.00"));

        TicketResponseDTO ultima = comprarDirecto(nuevoUsuario("u3@test.com", "U3", Rol.ASISTENTE));
        assertThat(ultima.getEstrategiaPrecio()).isEqualTo("LastMinute");
        assertThat(ultima.getPrecioFinal()).isEqualByComparingTo("150.00");
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

    private TicketResponseDTO comprarDirecto(Usuario u) {
        ResponseEntity<TicketResponseDTO> r = rest.exchange(
                "/api/tickets/eventos/" + evento.getId(),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(u)),
                TicketResponseDTO.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody();
    }
}
