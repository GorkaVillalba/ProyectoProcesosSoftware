package com.ProyectoProcesosSoftware.integration;

import com.ProyectoProcesosSoftware.dto.ValidationErrorResponseDTO;
import com.ProyectoProcesosSoftware.model.Rol;
import com.ProyectoProcesosSoftware.model.Usuario;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * US-20 / T-20.4: tests de integración de validación de entrada para los
 * endpoints de eventos. Verifica que la API responde 400 con un mapa
 * estructurado de errores por campo.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("dev")
class EventoValidationIT {

    @Autowired private TestRestTemplate rest;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario organizador;

    @BeforeEach
    void setUp() {
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();

        organizador = new Usuario();
        organizador.setNombre("Org validación");
        organizador.setEmail("org-validation@test.com");
        organizador.setPassword(passwordEncoder.encode("Password123!"));
        organizador.setRol(Rol.ORGANIZADOR);
        organizador = usuarioRepository.save(organizador);
    }

    @AfterEach
    void tearDown() {
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/events con fecha pasada, aforo 0, precio negativo y ubicación vacía → 400 con los 4 campos en el mapa")
    void crearEvento_camposInvalidos_devuelve400ConMapa() {
        Map<String, Object> body = new HashMap<>();
        body.put("nombre", "Evento inválido");
        body.put("descripcion", "Test");
        body.put("fecha", "2020-01-01"); // pasada
        body.put("hora", "20:00:00");
        body.put("ubicacion", "");       // vacía
        body.put("aforoMaximo", 0);      // menor que 1
        body.put("precioBase", -10);     // negativo

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtService.generarToken(
                organizador.getId(), organizador.getEmail(), organizador.getRol().name()));

        ResponseEntity<ValidationErrorResponseDTO> response = rest.exchange(
                "/api/events",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                ValidationErrorResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ValidationErrorResponseDTO err = response.getBody();
        assertThat(err).isNotNull();
        assertThat(err.getStatus()).isEqualTo(400);
        assertThat(err.getMessage()).isEqualTo("Error de validación");
        assertThat(err.getErrors())
                .containsKeys("fecha", "ubicacion", "aforoMaximo", "precioBase");
        assertThat(err.getErrors().get("fecha")).contains("futura");
        assertThat(err.getErrors().get("ubicacion")).contains("ubicación");
        assertThat(err.getErrors().get("aforoMaximo")).contains("al menos 1");
        assertThat(err.getErrors().get("precioBase")).contains("negativo");
    }
}