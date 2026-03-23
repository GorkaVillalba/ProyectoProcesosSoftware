package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.EditarUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.RegistroUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.UsuarioMapper;
import com.ProyectoProcesosSoftware.dto.UsuarioResponseDTO;
import com.ProyectoProcesosSoftware.exception.DuplicateResourceException;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.exception.UnauthorizedActionException;
import com.ProyectoProcesosSoftware.model.Rol;
import com.ProyectoProcesosSoftware.model.Usuario;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponseDTO registrar(RegistroUsuarioDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + dto.getEmail());
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setRol(Rol.valueOf(dto.getRol().toUpperCase()));

        Usuario guardado = usuarioRepository.save(usuario);
        return UsuarioMapper.toResponseDTO(guardado);
    }

    public UsuarioResponseDTO obtenerPerfil(Long id, Long usuarioAutenticadoId) {
        if (!id.equals(usuarioAutenticadoId)) {
            throw new UnauthorizedActionException("No puedes acceder al perfil de otro usuario");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        return UsuarioMapper.toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO editarPerfil(Long id, EditarUsuarioDTO dto, Long usuarioAutenticadoId) {
        if (!id.equals(usuarioAutenticadoId)) {
            throw new UnauthorizedActionException("No puedes editar el perfil de otro usuario");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (!usuario.getEmail().equals(dto.getEmail()) && usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + dto.getEmail());
        }

        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        Usuario actualizado = usuarioRepository.save(usuario);
        return UsuarioMapper.toResponseDTO(actualizado);
    }

    @Transactional
    public void eliminarCuenta(Long id, Long usuarioAutenticadoId) {
        if (!id.equals(usuarioAutenticadoId)) {
            throw new UnauthorizedActionException("No puedes eliminar la cuenta de otro usuario");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        usuarioRepository.delete(usuario);
    }
}