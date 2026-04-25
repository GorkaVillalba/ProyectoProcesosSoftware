package com.ProyectoProcesosSoftware.performance;

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
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * T-23: Tests de rendimiento.
 *
 * Lanzan cargas concurrentes contra los endpoints críticos y miden
 * tiempo medio, p95, p99 y throughput. Validan RNF1 (tiempo de
 * respuesta medio < 2s).
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("dev")
class TicketPerfTest {

    private static final long RNF1_MAX_MEAN_MS = 2000L;

    @Autowired private TestRestTemplate rest;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario organizador;
    private Evento evento;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();

        organizador = nuevoUsuario("org@perf.com", "Organizer", Rol.ORGANIZADOR);

        evento = new Evento();
        evento.setNombre("Perf Event");
        evento.setDescripcion("Test de rendimiento");
        evento.setFecha(LocalDate.now().plusMonths(2));
        evento.setHora(LocalTime.of(20, 0));
        evento.setUbicacion("Bilbao");
        evento.setAforoMaximo(1000);
        evento.setEntradasVendidas(0);
        evento.setPrecioBase(new BigDecimal("50.00"));
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
    // POST compra: 50 concurrentes
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/tickets/eventos/{id}: 50 peticiones concurrentes < 2s medio")
    void compraConcurrente_50() throws Exception {
        int N = 50;
        List<Usuario> compradores = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            compradores.add(nuevoUsuario("p" + i + "@perf.com", "P" + i, Rol.ASISTENTE));
        }

        List<Long> latencias = ejecutarConcurrente(N, i -> {
            HttpHeaders h = authHeaders(compradores.get(i));
            ResponseEntity<TicketResponseDTO> r = rest.exchange(
                    "/api/tickets/eventos/" + evento.getId(),
                    HttpMethod.POST,
                    new HttpEntity<>(h),
                    TicketResponseDTO.class);
            return r.getStatusCode().is2xxSuccessful();
        });

        imprimirInforme("POST /api/tickets/eventos/{id} (N=" + N + ")", latencias);
        assertThat(media(latencias)).isLessThan(RNF1_MAX_MEAN_MS);
    }

    // ─────────────────────────────────────────────────────────────
    // GET listado de eventos: 100 concurrentes
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/events: 100 peticiones concurrentes < 2s medio")
    void listadoConcurrente_100() throws Exception {
        int N = 100;
        Usuario lector = nuevoUsuario("lector@perf.com", "Lector", Rol.ASISTENTE);
        HttpHeaders headers = authHeaders(lector);

        List<Long> latencias = ejecutarConcurrente(N, i -> {
            ResponseEntity<String> r = rest.exchange(
                    "/api/events",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            return r.getStatusCode().is2xxSuccessful();
        });

        imprimirInforme("GET /api/events (N=" + N + ")", latencias);
        assertThat(media(latencias)).isLessThan(RNF1_MAX_MEAN_MS);
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/tickets/my: 50 concurrentes
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/tickets/my: 50 peticiones concurrentes < 2s medio")
    void misEntradasConcurrente_50() throws Exception {
        int N = 50;
        Usuario asistente = nuevoUsuario("asis@perf.com", "Asis", Rol.ASISTENTE);

        for (int i = 0; i < 3; i++) {
            Evento e = new Evento();
            e.setNombre("E" + i);
            e.setFecha(LocalDate.now().plusMonths(2));
            e.setHora(LocalTime.of(20, 0));
            e.setUbicacion("Bilbao");
            e.setAforoMaximo(10);
            e.setEntradasVendidas(0);
            e.setPrecioBase(new BigDecimal("50.00"));
            e.setEstado(EstadoEvento.PUBLICADO);
            e.setOrganizador(organizador);
            e = eventoRepository.save(e);
            rest.exchange("/api/tickets/eventos/" + e.getId(), HttpMethod.POST,
                    new HttpEntity<>(authHeaders(asistente)), TicketResponseDTO.class);
        }

        HttpHeaders headers = authHeaders(asistente);
        List<Long> latencias = ejecutarConcurrente(N, i -> {
            ResponseEntity<String> r = rest.exchange(
                    "/api/tickets/my",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            return r.getStatusCode().is2xxSuccessful();
        });

        imprimirInforme("GET /api/tickets/my (N=" + N + ")", latencias);
        assertThat(media(latencias)).isLessThan(RNF1_MAX_MEAN_MS);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private interface IndexedCall {
        boolean run(int index) throws Exception;
    }

    private List<Long> ejecutarConcurrente(int n, IndexedCall call) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(n, 32));
        List<Future<Long>> futuros = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int idx = i;
            futuros.add(pool.submit(() -> {
                long t0 = System.nanoTime();
                boolean ok = call.run(idx);
                long elapsed = (System.nanoTime() - t0) / 1_000_000L;
                if (!ok) throw new IllegalStateException("Petición fallida en índice " + idx);
                return elapsed;
            }));
        }
        List<Long> latencias = new ArrayList<>(n);
        for (Future<Long> f : futuros) latencias.add(f.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        return latencias;
    }

    private long media(List<Long> xs) {
        return (long) xs.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private long percentil(List<Long> xs, double p) {
        List<Long> sorted = new ArrayList<>(xs);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }

    private void imprimirInforme(String nombre, List<Long> latencias) {
        long total = latencias.stream().mapToLong(Long::longValue).sum();
        double throughput = total > 0 ? (latencias.size() * 1000.0) / total : 0;
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(" Informe de rendimiento: " + nombre);
        System.out.println("───────────────────────────────────────────────────────");
        System.out.println(" N peticiones : " + latencias.size());
        System.out.println(" Media (ms)   : " + media(latencias));
        System.out.println(" p95 (ms)     : " + percentil(latencias, 0.95));
        System.out.println(" p99 (ms)     : " + percentil(latencias, 0.99));
        System.out.println(" Máx (ms)     : " + Collections.max(latencias));
        System.out.println(" Mín (ms)     : " + Collections.min(latencias));
        System.out.println(" Throughput (req/s): " + String.format("%.2f", throughput));
        System.out.println("═══════════════════════════════════════════════════════");
    }

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
}
