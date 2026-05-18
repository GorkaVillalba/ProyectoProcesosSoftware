package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.CrearResenaDTO;
import com.ProyectoProcesosSoftware.dto.ResenaResponseDTO;
import com.ProyectoProcesosSoftware.exception.BusinessRuleException;
import com.ProyectoProcesosSoftware.exception.DuplicateResourceException;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.exception.UnauthorizedActionException;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.ResenaRepository;
import com.ProyectoProcesosSoftware.repository.TicketRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResenaService {

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TicketRepository ticketRepository;

    /**
     * Crea una reseña para un evento.
     * Reglas de negocio:
     * 1. El usuario debe existir y tener rol ASISTENTE.
     * 2. El usuario debe tener al menos un ticket VALIDO para el evento.
     * 3. No puede existir ya una reseña suya para ese evento.
     * 4. La puntuación debe estar entre 1 y 5 (validada también en el DTO).
     */
    @Transactional
    public ResenaResponseDTO crearResena(Long eventoId, Long asistenteId, CrearResenaDTO dto) {
        // 1. Validar usuario y rol
        Usuario asistente = usuarioRepository.findById(asistenteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + asistenteId));
        if (asistente.getRol() != Rol.ASISTENTE) {
            throw new UnauthorizedActionException(
                    "Solo los asistentes pueden dejar reseñas");
        }

        // Validar que el evento existe
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evento no encontrado con id: " + eventoId));

        // 2. Validar que tiene al menos un ticket VALIDO para el evento
        boolean tieneTicket = ticketRepository.existsByEventoIdAndAsistenteIdAndEstado(
                eventoId, asistenteId, TicketStatus.VALIDO);
        if (!tieneTicket) {
            throw new BusinessRuleException(
                    "Solo puedes reseñar eventos para los que tienes una entrada válida");
        }

        // 3. Validar que no existe ya una reseña suya para este evento
        if (resenaRepository.existsByEventoIdAndAsistenteId(eventoId, asistenteId)) {
            throw new DuplicateResourceException(
                    "Ya has dejado una reseña para este evento");
        }

        // 4. Puntuación entre 1 y 5 (doble check, ya validado por @Min/@Max en el DTO)
        if (dto.getPuntuacion() < 1 || dto.getPuntuacion() > 5) {
            throw new BusinessRuleException("La puntuación debe estar entre 1 y 5");
        }

        // Crear y persistir la reseña
        Resena resena = new Resena();
        resena.setEvento(evento);
        resena.setAsistente(asistente);
        resena.setPuntuacion(dto.getPuntuacion());
        resena.setComentario(dto.getComentario());

        return toResponseDTO(resenaRepository.save(resena));
    }

    /**
     * Lista las reseñas de un evento de forma paginada.
     */
    public Page<ResenaResponseDTO> listarResenasEvento(Long eventoId, Pageable pageable) {
        eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evento no encontrado con id: " + eventoId));
        return resenaRepository.findByEventoId(eventoId, pageable)
                .map(this::toResponseDTO);
    }

    /**
     * Devuelve la media de puntuación de un evento, o 0.0 si no hay reseñas.
     */
    public Double obtenerMediaEvento(Long eventoId) {
        eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evento no encontrado con id: " + eventoId));
        Double media = resenaRepository.findMediaPuntuacionByEventoId(eventoId);
        return media != null ? media : 0.0;
    }

    // ─── Mapper interno ───────────────────────────────────────────────────────

    private ResenaResponseDTO toResponseDTO(Resena resena) {
        ResenaResponseDTO dto = new ResenaResponseDTO();
        dto.setId(resena.getId());
        dto.setEventoId(resena.getEvento().getId());
        dto.setEventoNombre(resena.getEvento().getNombre());
        dto.setAsistenteId(resena.getAsistente().getId());
        dto.setAsistenteNombre(resena.getAsistente().getNombre());
        dto.setPuntuacion(resena.getPuntuacion());
        dto.setComentario(resena.getComentario());
        dto.setFechaCreacion(resena.getFechaCreacion());
        return dto;
    }
}