package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.EditarUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.RegistroUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.UsuarioResponseDTO;
import com.ProyectoProcesosSoftware.exception.DuplicateResourceException;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.exception.UnauthorizedActionException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    // ── T-08: Tests de registro ──

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

    // ── T-13 (Persona 1): Tests de ver y editar perfil ──

    @Test
    @DisplayName("T-13: Ver perfil exitoso devuelve UsuarioResponseDTO")
    void obtenerPerfil_exitoso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioGuardado));

        UsuarioResponseDTO result = usuarioService.obtenerPerfil(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("juan@example.com");
        verify(usuarioRepository).findById(1L);
    }

    @Test
    @DisplayName("T-13: Ver perfil de otro usuario lanza UnauthorizedActionException")
    void obtenerPerfil_otroUsuario_lanzaExcepcion() {
        assertThatThrownBy(() -> usuarioService.obtenerPerfil(1L, 99L))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    @DisplayName("T-13: Editar perfil exitoso devuelve usuario actualizado")
    void editarPerfil_exitoso() {
        EditarUsuarioDTO dto = new EditarUsuarioDTO();
        dto.setNombre("Juan Actualizado");
        dto.setEmail("juan@example.com");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioGuardado));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        UsuarioResponseDTO result = usuarioService.editarPerfil(1L, dto, 1L);

        assertThat(result).isNotNull();
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("T-13: Editar perfil de otro usuario lanza UnauthorizedActionException")
    void editarPerfil_otroUsuario_lanzaExcepcion() {
        EditarUsuarioDTO dto = new EditarUsuarioDTO();
        dto.setNombre("Juan");
        dto.setEmail("juan@example.com");

        assertThatThrownBy(() -> usuarioService.editarPerfil(1L, dto, 99L))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(usuarioRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("T-13: Obtener perfil de usuario no encontrado lanza ResourceNotFoundException")
    void obtenerPerfil_usuarioNoEncontrado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPerfil(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("T-13: Editar perfil con email ya usado por otro usuario lanza DuplicateResourceException")
    void editarPerfil_emailDuplicado() {
        EditarUsuarioDTO dto = new EditarUsuarioDTO();
        dto.setNombre("Juan");
        dto.setEmail("otro@example.com");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioGuardado));
        when(usuarioRepository.existsByEmail("otro@example.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.editarPerfil(1L, dto, 1L))
                .isInstanceOf(DuplicateResourceException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("T-13: Editar perfil de usuario no encontrado lanza ResourceNotFoundException")
    void editarPerfil_usuarioNoEncontrado() {
        EditarUsuarioDTO dto = new EditarUsuarioDTO();
        dto.setNombre("Juan");
        dto.setEmail("juan@example.com");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.editarPerfil(1L, dto, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("Eliminar cuenta propia exitosamente")
    void eliminarCuenta_exitoso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioGuardado));

        usuarioService.eliminarCuenta(1L, 1L);

        verify(usuarioRepository).delete(usuarioGuardado);
    }

    @Test
    @DisplayName("Eliminar cuenta de otro usuario lanza UnauthorizedActionException")
    void eliminarCuenta_otroUsuario_lanzaExcepcion() {
        assertThatThrownBy(() -> usuarioService.eliminarCuenta(1L, 99L))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Eliminar cuenta de usuario no encontrado lanza ResourceNotFoundException")
    void eliminarCuenta_usuarioNoEncontrado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.eliminarCuenta(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado");
    }
}
