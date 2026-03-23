package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.dto.EditarUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.MessageResponseDTO;
import com.ProyectoProcesosSoftware.dto.RegistroUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.UsuarioResponseDTO;
import com.ProyectoProcesosSoftware.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroUsuarioDTO dto) {
        UsuarioResponseDTO response = usuarioService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPerfil(
            @PathVariable Long id, Authentication auth) {
        Long authId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(usuarioService.obtenerPerfil(id, authId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> editarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody EditarUsuarioDTO dto,
            Authentication auth) {
        Long authId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(usuarioService.editarPerfil(id, dto, authId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponseDTO> eliminarCuenta(
            @PathVariable Long id, Authentication auth) {
        Long authId = Long.parseLong(auth.getName());
        usuarioService.eliminarCuenta(id, authId);
        return ResponseEntity.ok(new MessageResponseDTO("Cuenta eliminada correctamente"));
    }
}