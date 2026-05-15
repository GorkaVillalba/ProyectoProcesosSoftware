package com.ProyectoProcesosSoftware.integration;

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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * US-18 / T-18.4: bajo carga concurrente, el bloqueo optimista con
 * {@code @Version} en {@link Evento} impide la sobreventa. Verifica el
 * invariante: <b>entradasVendidas nunca supera el aforo</b> aunque varios
 * hilos compitan por las últimas plazas.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("dev")
class TicketConcurrencyIT {

    private static final int HILOS = 20;
    private static final int AFORO = 19;

    @Autowired private TestRestTemplate rest;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario organizador;
    private Evento evento;
    private List<Usuario> asistentes;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();

        organizador = nuevoUsuario("org-concurrency@test.com", "Org", Rol.ORGANIZADOR);

        evento = new Evento();
        evento.setNombre("Festival concurrencia");
        evento.setDescripcion("Test de carga concurrente");
        evento.setFecha(LocalDate.now().plusMonths(2));
        evento.setHora(LocalTime.of(20, 0));
        evento.setUbicacion("Bilbao");
        evento.setAforoMaximo(AFORO);
        evento.setEntradasVendidas(0);
        evento.setPrecioBase(new BigDecimal("50.00"));
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(organizador);
        evento = eventoRepository.save(evento);

        asistentes = new ArrayList<>();
        for (int i = 0; i < HILOS; i++) {
            asistentes.add(nuevoUsuario("user" + i + "@concurrency.test", "User " + i, Rol.ASISTENTE));
        }
    }

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    /**
     * Lanza 20 hilos contra un evento con 19 plazas. Con la decisión de "409 directo
     * (sin retry)", el número exacto de 201 vs 409 depende del scheduler: varios
     * hilos pueden colisionar entre sí. Lo que el bloqueo optimista garantiza es
     * el invariante crítico: <b>entradasVendidas nunca supera el aforo</b>, y por
     * tanto no se entregan más tickets de los que caben.
     */
    @Test
    @DisplayName("20 hilos contra 19 plazas → entradasVendidas <= aforo (sin sobreventa) y al menos un 409")
    void compras_concurrentes_no_sobrevendidas() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(HILOS);

        AtomicInteger creados = new AtomicInteger(0);
        AtomicInteger conflictos = new AtomicInteger(0);
        AtomicInteger otros = new AtomicInteger(0);

        for (Usuario u : asistentes) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    ResponseEntity<String> r = rest.exchange(
                            "/api/tickets/eventos/" + evento.getId(),
                            HttpMethod.POST,
                            new HttpEntity<>(authHeaders(u)),
                            String.class);
                    if (r.getStatusCode() == HttpStatus.CREATED) {
                        creados.incrementAndGet();
                    } else if (r.getStatusCode() == HttpStatus.CONFLICT) {
                        conflictos.incrementAndGet();
                    } else {
                        otros.incrementAndGet();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishGate.countDown();
                }
            });
        }

        // Arranque sincronizado de los 20 hilos.
        startGate.countDown();
        boolean acabaron = finishGate.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(acabaron).as("todos los hilos terminan en 30s").isTrue();
        assertThat(otros.get()).as("ninguna respuesta inesperada (5xx, 4xx distinto de 409)").isZero();
        assertThat(creados.get() + conflictos.get())
                .as("todas las peticiones terminan en 201 o 409").isEqualTo(HILOS);
        assertThat(conflictos.get())
                .as("al menos una colisión optimista bajo carga concurrente").isGreaterThanOrEqualTo(1);
        assertThat(creados.get()).as("al menos una compra tiene éxito").isGreaterThanOrEqualTo(1);
        assertThat(creados.get()).as("nunca se entregan más tickets que el aforo").isLessThanOrEqualTo(AFORO);

        Evento eventoFinal = eventoRepository.findById(evento.getId()).orElseThrow();
        assertThat(eventoFinal.getEntradasVendidas())
                .as("INVARIANTE: entradasVendidas nunca supera el aforo")
                .isLessThanOrEqualTo(AFORO);
        assertThat(eventoFinal.getEntradasVendidas())
                .as("entradasVendidas coincide con tickets creados").isEqualTo(creados.get());
        assertThat(ticketRepository.findAll()).hasSize(creados.get());
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