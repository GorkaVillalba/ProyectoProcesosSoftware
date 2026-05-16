package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.EditarUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.MessageResponseDTO;
import com.ProyectoProcesosSoftware.dto.RegistroUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.UsuarioResponseDTO;
import com.ProyectoProcesosSoftware.service.UsuarioService;
import com.ProyectoProcesosSoftware.dto.ValidationErrorResponseDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuarios", description = "Gestión de usuarios: registro, consulta, edición y eliminación de perfil")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
        @Operation(
        summary = "Registrar nuevo usuario",
        description = "Crea una cuenta de usuario con los datos proporcionados. El email debe ser único."
    )
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
        content = @Content(schema = @Schema(implementation = ValidationErrorResponseDTO.class)))
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroUsuarioDTO dto) {
        UsuarioResponseDTO response = usuarioService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
        @Operation(
        summary = "Obtener perfil de usuario",
        description = "Devuelve los datos del perfil del usuario identificado. Un usuario solo puede ver su propio perfil (o un administrador)."
    )
    public ResponseEntity<UsuarioResponseDTO> obtenerPerfil(
            @PathVariable Long id, Authentication auth) {
        Long authId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(usuarioService.obtenerPerfil(id, authId));
    }

    @PutMapping("/{id}")
        @Operation(
        summary = "Editar perfil de usuario",
        description = "Actualiza los datos del perfil del usuario autenticado (o administrador). Se requiere que el usuario autenticado coincida con el ID del perfil."
    )
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
        content = @Content(schema = @Schema(implementation = ValidationErrorResponseDTO.class)))
    public ResponseEntity<UsuarioResponseDTO> editarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody EditarUsuarioDTO dto,
            Authentication auth) {
        Long authId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(usuarioService.editarPerfil(id, dto, authId));
    }

    @DeleteMapping("/{id}")
        @Operation(
        summary = "Eliminar cuenta de usuario",
        description = "Elimina la cuenta del usuario autenticado (o administrador). Se requiere que el ID coincida con el usuario autenticado."
    )
    public ResponseEntity<MessageResponseDTO> eliminarCuenta(
            @PathVariable Long id, Authentication auth) {
        Long authId = Long.parseLong(auth.getName());
        usuarioService.eliminarCuenta(id, authId);
        return ResponseEntity.ok(new MessageResponseDTO("Cuenta eliminada correctamente"));
    }
}