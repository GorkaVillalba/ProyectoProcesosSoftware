package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.EventoMapper;
import com.ProyectoProcesosSoftware.dto.EventoResponseDTO;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.model.Favorito;
import com.ProyectoProcesosSoftware.pricing.PricingContext;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.FavoritoRepository;
import com.ProyectoProcesosSoftware.repository.ResenaRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import com.ProyectoProcesosSoftware.model.Evento;
import com.ProyectoProcesosSoftware.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoritoService {

    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ResenaRepository resenaRepository;
    @Autowired private PricingContext pricingContext;

    /**
     * Marca un evento como favorito para un usuario.
     * Idempotente: si el favorito ya existe no lanza error ni lo duplica.
     */
    @Transactional
    public void agregar(Long usuarioId, Long eventoId) {
        if (favoritoRepository.existsByUsuarioIdAndEventoId(usuarioId, eventoId)) {
            return;
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + eventoId));

        Favorito favorito = new Favorito();
        favorito.setUsuario(usuario);
        favorito.setEvento(evento);
        favoritoRepository.save(favorito);
    }

    /**
     * Elimina un evento de la lista de favoritos de un usuario.
     * Idempotente: si el favorito no existe no lanza error.
     */
    @Transactional
    public void quitar(Long usuarioId, Long eventoId) {
        if (!favoritoRepository.existsByUsuarioIdAndEventoId(usuarioId, eventoId)) {
            return;
        }
        favoritoRepository.deleteByUsuarioIdAndEventoId(usuarioId, eventoId);
    }

    /**
     * Devuelve la lista de eventos favoritos de un usuario como EventoResponseDTO,
     * incluyendo puntuacionMedia y numeroResenas de cada evento.
     */
    public List<EventoResponseDTO> listarFavoritos(Long usuarioId) {
        return favoritoRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(fav -> {
                    Evento e = fav.getEvento();
                    Double media = resenaRepository.findMediaPuntuacionByEventoId(e.getId());
                    long numResenas = resenaRepository.countByEventoId(e.getId());
                    return EventoMapper.toResponseDTO(e, pricingContext, media, numResenas);
                })
                .collect(Collectors.toList());
    }
}