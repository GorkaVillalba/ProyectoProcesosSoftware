package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.EditarUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.MessageResponseDTO;
import com.ProyectoProcesosSoftware.dto.RegistroUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.UsuarioResponseDTO;
import com.ProyectoProcesosSoftware.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock private UsuarioService usuarioService;
    @Mock private Authentication authentication;

    @InjectMocks private UsuarioController usuarioController;

    private UsuarioResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new UsuarioResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setEmail("user@test.com");
        responseDTO.setNombre("User");
        responseDTO.setRol("ASISTENTE");
    }

    @Test
    @DisplayName("registrar: devuelve 201 CREATED con el usuario creado")
    void registrar() {
        RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
        dto.setEmail("user@test.com");
        dto.setNombre("User");
        dto.setPassword("password");
        dto.setRol("ASISTENTE");

        when(usuarioService.registrar(dto)).thenReturn(responseDTO);

        ResponseEntity<UsuarioResponseDTO> response = usuarioController.registrar(dto);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(responseDTO);
    }

    @Test
    @DisplayName("obtenerPerfil: extrae authId del Authentication y delega al servicio")
    void obtenerPerfil() {
        when(authentication.getName()).thenReturn("1");
        when(usuarioService.obtenerPerfil(1L, 1L)).thenReturn(responseDTO);

        ResponseEntity<UsuarioResponseDTO> response =
                usuarioController.obtenerPerfil(1L, authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDTO);
    }

    @Test
    @DisplayName("editarPerfil: pasa authId al servicio y devuelve 200 con el DTO actualizado")
    void editarPerfil() {
        EditarUsuarioDTO dto = new EditarUsuarioDTO();
        dto.setNombre("Nuevo");
        dto.setEmail("nuevo@test.com");

        when(authentication.getName()).thenReturn("2");
        when(usuarioService.editarPerfil(5L, dto, 2L)).thenReturn(responseDTO);

        ResponseEntity<UsuarioResponseDTO> response =
                usuarioController.editarPerfil(5L, dto, authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDTO);
    }

    @Test
    @DisplayName("eliminarCuenta: invoca al servicio y devuelve mensaje de confirmación")
    void eliminarCuenta() {
        when(authentication.getName()).thenReturn("7");

        ResponseEntity<MessageResponseDTO> response =
                usuarioController.eliminarCuenta(7L, authentication);

        verify(usuarioService).eliminarCuenta(7L, 7L);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Cuenta eliminada correctamente");
    }
}