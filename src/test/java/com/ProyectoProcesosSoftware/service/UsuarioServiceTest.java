package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.RegistroUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.UsuarioResponseDTO;
import com.ProyectoProcesosSoftware.exception.DuplicateResourceException;
import com.ProyectoProcesosSoftware.model.Rol;
import com.ProyectoProcesosSoftware.model.Usuario;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// T-08 (Persona 6): Tests unitarios de UsuarioService.registrar()
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UsuarioService usuarioService;

    private RegistroUsuarioDTO registroDTO;
    private Usuario usuarioGuardado;

    @BeforeEach
    void setUp() {
        registroDTO = new RegistroUsuarioDTO();
        registroDTO.setNombre("Juan García");
        registroDTO.setEmail("juan@example.com");
        registroDTO.setPassword("password123");
        registroDTO.setRol("ASISTENTE");

        usuarioGuardado = new Usuario();
        usuarioGuardado.setId(1L);
        usuarioGuardado.setNombre("Juan García");
        usuarioGuardado.setEmail("juan@example.com");
        usuarioGuardado.setPassword("$2a$10$encoded");
        usuarioGuardado.setRol(Rol.ASISTENTE);
        usuarioGuardado.setFechaRegistro(LocalDateTime.now());
    }

    @Test
    @DisplayName("Registro exitoso devuelve UsuarioResponseDTO")
    void registrar_exitoso() {
        when(usuarioRepository.existsByEmail("juan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        UsuarioResponseDTO result = usuarioService.registrar(registroDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("juan@example.com");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Email duplicado lanza DuplicateResourceException")
    void registrar_emailDuplicado() {
        when(usuarioRepository.existsByEmail("juan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(registroDTO))
                .isInstanceOf(DuplicateResourceException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Contraseña se almacena hasheada")
    void registrar_passwordHasheada() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            assertThat(u.getPassword()).isEqualTo("$2a$10$hash");
            u.setId(1L);
            u.setFechaRegistro(LocalDateTime.now());
            return u;
        });

        usuarioService.registrar(registroDTO);
        verify(passwordEncoder).encode("password123");
    }
}
