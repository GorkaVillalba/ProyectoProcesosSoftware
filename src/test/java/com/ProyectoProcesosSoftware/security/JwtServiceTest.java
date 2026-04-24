package com.ProyectoProcesosSoftware.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "clave-secreta-suficientemente-larga-para-hmac-sha256");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);
    }

    @Test
    @DisplayName("generarToken devuelve un token no nulo")
    void generarToken_devuelveTokenNoNulo() {
        String token = jwtService.generarToken(1L, "test@example.com", "ASISTENTE");
        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("extraerUserId devuelve el id correcto")
    void extraerUserId_correcto() {
        String token = jwtService.generarToken(1L, "test@example.com", "ASISTENTE");
        assertThat(jwtService.extraerUserId(token)).isEqualTo("1");
    }

    @Test
    @DisplayName("extraerEmail devuelve el email correcto")
    void extraerEmail_correcto() {
        String token = jwtService.generarToken(1L, "test@example.com", "ASISTENTE");
        assertThat(jwtService.extraerEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("extraerRol devuelve el rol correcto")
    void extraerRol_correcto() {
        String token = jwtService.generarToken(1L, "test@example.com", "ASISTENTE");
        assertThat(jwtService.extraerRol(token)).isEqualTo("ASISTENTE");
    }

    @Test
    @DisplayName("validarToken con token válido devuelve true")
    void validarToken_valido() {
        String token = jwtService.generarToken(1L, "test@example.com", "ASISTENTE");
        assertThat(jwtService.validarToken(token)).isTrue();
    }

    @Test
    @DisplayName("validarToken con token expirado devuelve false")
    void validarToken_expirado() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.generarToken(1L, "test@example.com", "ASISTENTE");
        assertThat(jwtService.validarToken(token)).isFalse();
    }

    @Test
    @DisplayName("validarToken con token malformado devuelve false")
    void validarToken_malformado() {
        assertThat(jwtService.validarToken("esto.no.es.un.token")).isFalse();
    }
}