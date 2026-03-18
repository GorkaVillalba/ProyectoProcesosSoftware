package com.ProyectoProcesosSoftware.controller;

import com.ProyectoProcesosSoftware.model.Usuario;
import com.ProyectoProcesosSoftware.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping // Atiende peticiones POST a /api/users
    public ResponseEntity<?> registrarUsuario(@Valid @RequestBody Usuario usuario) {
        try {
            Usuario nuevoUsuario = usuarioService.registrar(usuario);
            return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED); // 201 Created
        } catch (RuntimeException e) {
            // Si el email está duplicado, lanzamos el error 409
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT); // 409 Conflict
        }
    }
}