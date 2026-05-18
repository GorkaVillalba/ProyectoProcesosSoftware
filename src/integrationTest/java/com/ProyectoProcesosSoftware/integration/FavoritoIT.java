package com.ProyectoProcesosSoftware.integration;

import com.ProyectoProcesosSoftware.dto.EventoResponseDTO;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.FavoritoRepository;
import com.ProyectoProcesosSoftware.repository.TicketRepository;
import com.ProyectoProcesosSoftware.repository.ResenaRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import com.ProyectoProcesosSoftware.security.JwtService;
import org.junit.jupiter.api.*;
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
 * T-27.5: Tests de integración end-to-end de favoritos.
 *
 * Flujo principal:
 *   POST /api/events/{id}/favorite  → 201
 *   GET  /api/users/me/favorites    → lista con el evento
 *   DELETE /api/events/{id}/favorite → 204
 *   GET  /api/users/me/favorites    → lista vacía
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("dev")
class FavoritoIT {

    @Autowired private TestRestTemplate rest;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private ResenaRepository resenaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario usuario;
    private Evento evento;

    @BeforeEach
    void setUp() {
        resenaRepository.deleteAll();
        ticketRepository.deleteAll();
        favoritoRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario organizador = nuevoUsuario("org@fav.com", "Organizador", Rol.ORGANIZADOR);
        usuario = nuevoUsuario("user@fav.com", "Usuario Test", Rol.ASISTENTE);

        evento = new Evento();
        evento.setNombre("Evento Favorito IT");
        evento.setDescripcion("Test");
        evento.setFecha(LocalDate.now().plusMonths(1));
        evento.setHora(LocalTime.of(19, 0));
        evento.setUbicacion("Vitoria");
        evento.setAforoMaximo(100);
        evento.setEntradasVendidas(0);
        evento.setPrecioBase(new BigDecimal("40.00"));
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(organizador);
        evento = eventoRepository.save(evento);
    }

    @AfterEach
    void tearDown() {
        resenaRepository.deleteAll();
        ticketRepository.deleteAll();
        favoritoRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    // ─── Flujo completo ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Flujo completo: POST → GET lista → DELETE → GET lista vacía")
    void flujoCompleto_favorito_end2end() {
        // POST — añadir a favoritos
        ResponseEntity<Void> postResp = rest.exchange(
                "/api/events/" + evento.getId() + "/favorite",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                Void.class);
        assertThat(postResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // GET — la lista debe contener el evento
        ResponseEntity<List<EventoResponseDTO>> getResp = rest.exchange(
                "/api/users/me/favorites",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<List<EventoResponseDTO>>() {});
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody()).hasSize(1);
        assertThat(getResp.getBody().get(0).getId()).isEqualTo(evento.getId());
        assertThat(getResp.getBody().get(0).getNombre()).isEqualTo("Evento Favorito IT");

        // DELETE — quitar de favoritos
        ResponseEntity<Void> deleteResp = rest.exchange(
                "/api/events/" + evento.getId() + "/favorite",
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // GET — la lista debe estar vacía
        ResponseEntity<List<EventoResponseDTO>> getResp2 = rest.exchange(
                "/api/users/me/favorites",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<List<EventoResponseDTO>>() {});
        assertThat(getResp2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp2.getBody()).isEmpty();

        // Verificar que no queda nada en BD
        assertThat(favoritoRepository.existsByUsuarioIdAndEventoId(usuario.getId(), evento.getId()))
                .isFalse();
    }

    // ─── Idempotencia ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST duplicado es idempotente: devuelve 201 y no crea duplicado")
    void postDuplicado_esIdempotente() {
        rest.exchange("/api/events/" + evento.getId() + "/favorite",
                HttpMethod.POST, new HttpEntity<>(authHeaders()), Void.class);
        ResponseEntity<Void> segunda = rest.exchange(
                "/api/events/" + evento.getId() + "/favorite",
                HttpMethod.POST, new HttpEntity<>(authHeaders()), Void.class);

        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(favoritoRepository.findByUsuarioId(usuario.getId())).hasSize(1);
    }

    @Test
    @DisplayName("DELETE sin favorito previo es idempotente: devuelve 204")
    void deleteSinFavorito_esIdempotente() {
        ResponseEntity<Void> resp = rest.exchange(
                "/api/events/" + evento.getId() + "/favorite",
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ─── Seguridad ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST sin token devuelve 401 o 403")
    void postSinToken_401() {
        ResponseEntity<String> resp = rest.exchange(
                "/api/events/" + evento.getId() + "/favorite",
                HttpMethod.POST, HttpEntity.EMPTY, String.class);
        assertThat(resp.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET favoritos sin token devuelve 401 o 403")
    void getSinToken_401() {
        ResponseEntity<String> resp = rest.exchange(
                "/api/users/me/favorites",
                HttpMethod.GET, HttpEntity.EMPTY, String.class);
        assertThat(resp.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET favoritos evento inexistente en POST devuelve 404")
    void postEventoInexistente_404() {
        ResponseEntity<String> resp = rest.exchange(
                "/api/events/999999/favorite",
                HttpMethod.POST, new HttpEntity<>(authHeaders()), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Usuario nuevoUsuario(String email, String nombre, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("Password123!"));
        u.setRol(rol);
        return usuarioRepository.save(u);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol().name()));
        return h;
    }
}