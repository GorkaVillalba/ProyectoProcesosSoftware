package com.ProyectoProcesosSoftware.integration;

import com.ProyectoProcesosSoftware.dto.ValidationErrorResponseDTO;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * US-20 / T-20.4: tests de integración de validación de entrada para los
 * endpoints de usuarios. Verifica que la API responde 400 con un mapa
 * estructurado de errores por campo.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("dev")
class UsuarioValidationIT {

    @Autowired private TestRestTemplate rest;
    @Autowired private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/users con email malformado, password corta y nombre vacío → 400 con los 3 campos en el mapa")
    void registro_camposInvalidos_devuelve400ConMapa() {
        Map<String, Object> body = Map.of(
                "nombre", "",
                "email", "no-es-un-email",
                "password", "123",
                "rol", "ASISTENTE"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<ValidationErrorResponseDTO> response = rest.exchange(
                "/api/users",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                ValidationErrorResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ValidationErrorResponseDTO err = response.getBody();
        assertThat(err).isNotNull();
        assertThat(err.getStatus()).isEqualTo(400);
        assertThat(err.getMessage()).isEqualTo("Error de validación");
        assertThat(err.getErrors())
                .containsKeys("nombre", "email", "password");
        assertThat(err.getErrors().get("nombre")).isEqualTo("El nombre es obligatorio");
        assertThat(err.getErrors().get("email")).isEqualTo("El email debe ser válido");
        assertThat(err.getErrors().get("password"))
                .contains("al menos 6 caracteres");
    }
}