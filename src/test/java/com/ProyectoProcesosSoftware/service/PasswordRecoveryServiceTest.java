package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.exception.BusinessRuleException;
import com.ProyectoProcesosSoftware.model.TokenRecuperacion;
import com.ProyectoProcesosSoftware.model.Usuario;
import com.ProyectoProcesosSoftware.repository.TokenRecuperacionRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// T-28 (Persona 7): Tests de PasswordRecoveryService
@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TokenRecuperacionRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private PasswordRecoveryService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@example.com");
        usuario.setPassword("old");
    }

    @Test void generar_token_exitoso() {
        when(usuarioRepository.findByEmail("test@example.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        String token = service.generarTokenRecuperacion("test@example.com");
        assertThat(token).isNotNull();
        verify(tokenRepository).save(any());
    }

    @Test void cambiar_con_token_valido() {
        TokenRecuperacion tk = new TokenRecuperacion();
        tk.setUsuario(usuario);
        tk.setToken("abc");
        tk.setFechaExpiracion(LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(tk));
        when(passwordEncoder.encode("new123")).thenReturn("$hashed");

        service.cambiarPassword("abc", "new123");
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).delete(tk);
    }

    @Test void token_expirado_400() {
        TokenRecuperacion tk = new TokenRecuperacion();
        tk.setUsuario(usuario);
        tk.setToken("abc");
        tk.setFechaExpiracion(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(tk));

        assertThatThrownBy(() -> service.cambiarPassword("abc", "new"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("expirado");
    }

    @Test void token_invalido_400() {
        when(tokenRepository.findByToken("xxx")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cambiarPassword("xxx", "new"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("inválido");
    }
}
