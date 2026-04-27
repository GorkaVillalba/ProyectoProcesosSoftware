package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.ForgotPasswordDTO;
import com.ProyectoProcesosSoftware.dto.JwtResponseDTO;
import com.ProyectoProcesosSoftware.dto.LoginDTO;
import com.ProyectoProcesosSoftware.dto.MessageResponseDTO;
import com.ProyectoProcesosSoftware.dto.ResetPasswordDTO;
import com.ProyectoProcesosSoftware.model.Rol;
import com.ProyectoProcesosSoftware.model.Usuario;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import com.ProyectoProcesosSoftware.security.JwtService;
import com.ProyectoProcesosSoftware.service.PasswordRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private PasswordRecoveryService passwordRecoveryService;

    @InjectMocks private AuthController authController;

    private Usuario usuario;
    private LoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("user@test.com");
        usuario.setPassword("hashedPwd");
        usuario.setRol(Rol.ASISTENTE);

        loginDTO = new LoginDTO();
        loginDTO.setEmail("user@test.com");
        loginDTO.setPassword("plainPwd");
    }

    @Test
    @DisplayName("login: credenciales válidas devuelve token y datos del usuario")
    void loginExitoso() {
        when(usuarioRepository.findByEmail("user@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("plainPwd", "hashedPwd")).thenReturn(true);
        when(jwtService.generarToken(eq(1L), eq("user@test.com"), eq("ASISTENTE")))
                .thenReturn("token-jwt");

        ResponseEntity<?> response = authController.login(loginDTO);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(JwtResponseDTO.class);
        JwtResponseDTO body = (JwtResponseDTO) response.getBody();
        assertThat(body.getToken()).isEqualTo("token-jwt");
        assertThat(body.getId()).isEqualTo(1L);
        assertThat(body.getEmail()).isEqualTo("user@test.com");
        assertThat(body.getRol()).isEqualTo("ASISTENTE");
    }

    @Test
    @DisplayName("login: usuario inexistente devuelve 401")
    void loginUsuarioNoEncontrado() {
        when(usuarioRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.login(loginDTO);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isInstanceOf(MessageResponseDTO.class);
        assertThat(((MessageResponseDTO) response.getBody()).getMessage())
                .isEqualTo("Credenciales incorrectas");
    }

    @Test
    @DisplayName("login: contraseña incorrecta devuelve 401")
    void loginPasswordIncorrecta() {
        when(usuarioRepository.findByEmail("user@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("plainPwd", "hashedPwd")).thenReturn(false);

        ResponseEntity<?> response = authController.login(loginDTO);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(((MessageResponseDTO) response.getBody()).getMessage())
                .isEqualTo("Credenciales incorrectas");
    }

    @Test
    @DisplayName("forgotPassword: invoca el servicio y responde 200 con mensaje genérico")
    void forgotPasswordOk() {
        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setEmail("user@test.com");

        ResponseEntity<MessageResponseDTO> response = authController.forgotPassword(dto);

        verify(passwordRecoveryService).generarTokenRecuperacion("user@test.com");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage())
                .contains("Si el email está registrado");
    }

    @Test
    @DisplayName("resetPassword: invoca al servicio con token y nueva password y responde 200")
    void resetPasswordOk() {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken("tok-123");
        dto.setNuevaPassword("nuevaPwd");

        ResponseEntity<MessageResponseDTO> response = authController.resetPassword(dto);

        verify(passwordRecoveryService).cambiarPassword("tok-123", "nuevaPwd");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Contraseña restablecida correctamente");
    }
}