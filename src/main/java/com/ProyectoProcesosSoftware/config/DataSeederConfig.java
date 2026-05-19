package com.ProyectoProcesosSoftware.config;

import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Seeder Java alternativo al SQL puro, usado únicamente con perfil "demo".
 * Solo siembra si la BD está vacía (idempotente).
 */
@Configuration
@Profile("demo")
public class DataSeederConfig {

    @Bean
    CommandLineRunner seed(UsuarioRepository usuarios,
                           EventoRepository eventos,
                           TicketRepository tickets,
                           ResenaRepository resenas,
                           FavoritoRepository favoritos,
                           PasswordEncoder encoder) {
        return args -> {
            if (usuarios.count() > 0) return; // ya sembrado

            // ── Usuarios ──
            Usuario org   = nuevoUsuario("Olivia Organizadora", "org@demo.com",   "Demo1234!", Rol.ORGANIZADOR, encoder);
            Usuario alice = nuevoUsuario("Alice Asistente",     "alice@demo.com", "Demo1234!", Rol.ASISTENTE,   encoder);
            Usuario bob   = nuevoUsuario("Bob Asistente",       "bob@demo.com",   "Demo1234!", Rol.ASISTENTE,   encoder);
            Usuario carol = nuevoUsuario("Carol Asistente",     "carol@demo.com", "Demo1234!", Rol.ASISTENTE,   encoder);
            usuarios.saveAll(List.of(org, alice, bob, carol));

            // ── Eventos ──
            Evento e1 = nuevoEvento("Concierto Jazz Bilbao",  "EarlyBird 0% de ocupación.",   LocalDate.now().plusMonths(2), LocalTime.of(20, 0), "Bilbao",        100, 0,  50.00, EstadoEvento.PUBLICADO, org);
            Evento e2 = nuevoEvento("Festival Indie Donosti", "Regular 60% de ocupación.",    LocalDate.now().plusMonths(2), LocalTime.of(21, 0), "San Sebastián", 100, 60, 80.00, EstadoEvento.PUBLICADO, org);
            Evento e3 = nuevoEvento("Show Stand-Up Vitoria",  "LastMinute 90% de ocupación.", LocalDate.now().plusMonths(1), LocalTime.of(22, 0), "Vitoria",       100, 90, 30.00, EstadoEvento.PUBLICADO, org);
            Evento e4 = nuevoEvento("Final Liga Esports",     "AGOTADO 100% de ocupación.",   LocalDate.now().plusMonths(3), LocalTime.of(19, 0), "Bilbao Arena",   50, 50, 25.00, EstadoEvento.AGOTADO,   org);
            Evento e5 = nuevoEvento("Charla Tech (Borrador)", "Aún sin publicar (BORRADOR).", LocalDate.now().plusMonths(6), LocalTime.of(18, 0), "Bilbao",        200, 0,   0.00, EstadoEvento.BORRADOR,  org);
            eventos.saveAll(List.of(e1, e2, e3, e4, e5));

            // ── Tickets ──
            tickets.saveAll(List.of(
                nuevoTicket(e1, alice, TicketStatus.VALIDO,    50.00),
                nuevoTicket(e2, alice, TicketStatus.VALIDO,    80.00),
                nuevoTicket(e3, bob,   TicketStatus.VALIDO,    45.00),
                nuevoTicket(e2, carol, TicketStatus.CANCELADO, 80.00)
            ));

            // ── Reseñas (US-26) ──
            resenas.saveAll(List.of(
                nuevaResena(e1, alice, 5, "Concierto espectacular, ambiente increíble."),
                nuevaResena(e2, alice, 4, "Muy buen festival aunque algo concurrido."),
                nuevaResena(e3, bob,   3, "Stand-up entretenido, esperaba un poco más.")
            ));

            // ── Favoritos (US-27): 2 por asistente ──
            favoritos.saveAll(List.of(
                nuevoFavorito(alice, e1), nuevoFavorito(alice, e3),
                nuevoFavorito(bob,   e2), nuevoFavorito(bob,   e4),
                nuevoFavorito(carol, e1), nuevoFavorito(carol, e2)
            ));
        };
    }

    private static Usuario nuevoUsuario(String nombre, String email, String pass, Rol rol, PasswordEncoder enc) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPassword(enc.encode(pass));
        u.setRol(rol);
        return u;
    }

    private static Evento nuevoEvento(String nombre, String desc, LocalDate fecha, LocalTime hora,
                                      String ubic, int aforo, int vendidas, double precio,
                                      EstadoEvento estado, Usuario organizador) {
        Evento e = new Evento();
        e.setNombre(nombre);
        e.setDescripcion(desc);
        e.setFecha(fecha);
        e.setHora(hora);
        e.setUbicacion(ubic);
        e.setAforoMaximo(aforo);
        e.setEntradasVendidas(vendidas);
        e.setPrecioBase(BigDecimal.valueOf(precio));
        e.setEstado(estado);
        e.setOrganizador(organizador);
        return e;
    }

    private static Ticket nuevoTicket(Evento e, Usuario u, TicketStatus estado, double precio) {
        Ticket t = new Ticket();
        t.setEvento(e);
        t.setAsistente(u);
        t.setEstado(estado);
        t.setPrecioFinal(BigDecimal.valueOf(precio));
        return t;
    }

    private static Resena nuevaResena(Evento e, Usuario u, int puntos, String comentario) {
        Resena r = new Resena();
        r.setEvento(e);
        r.setAsistente(u);
        r.setPuntuacion(puntos);
        r.setComentario(comentario);
        return r;
    }

    private static Favorito nuevoFavorito(Usuario u, Evento e) {
        Favorito f = new Favorito();
        f.setUsuario(u);
        f.setEvento(e);
        return f;
    }
}