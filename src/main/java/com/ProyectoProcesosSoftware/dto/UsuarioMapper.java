package com.ProyectoProcesosSoftware.dto;

import com.ProyectoProcesosSoftware.model.Usuario;

public class UsuarioMapper {
    public static UsuarioResponseDTO toResponseDTO(Usuario u) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(u.getId());
        dto.setNombre(u.getNombre());
        dto.setEmail(u.getEmail());
        dto.setRol(u.getRol().name());
        dto.setFechaRegistro(u.getFechaRegistro());
        return dto;
    }
}
