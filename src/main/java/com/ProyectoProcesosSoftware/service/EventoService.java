package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.pricing.PricingContext;
import com.ProyectoProcesosSoftware.dto.*;
import com.ProyectoProcesosSoftware.exception.*;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ═══════════════════════════════════════════════════════════════
// T-16 (Persona 4): Servicio de Eventos
// Incluye métodos de T-18 (listar), T-20 (detalle), T-22 (editar), T-24 (eliminar)
// ═══════════════════════════════════════════════════════════════
@Service
public class EventoService {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public EventoResponseDTO crearEvento(CrearEventoDTO dto, Long organizadorId) {
        Usuario org = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (org.getRol() != Rol.ORGANIZADOR) {
            throw new UnauthorizedActionException("Solo los organizadores pueden crear eventos");
        }

        Evento evento = new Evento();
        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFecha(dto.getFecha());
        evento.setHora(dto.getHora());
        evento.setUbicacion(dto.getUbicacion());
        evento.setAforoMaximo(dto.getAforoMaximo());
        evento.setPrecioBase(dto.getPrecioBase());
        evento.setEstado(EstadoEvento.BORRADOR);
        evento.setOrganizador(org);

        return EventoMapper.toResponseDTO(eventoRepository.save(evento), pricingContext);
    }

    // T-18 (Persona 5)
    public Page<EventoResponseDTO> listarEventos(String nombre, String ubicacion, Pageable pageable) {
        return eventoRepository.findByEstadoAndFiltros(EstadoEvento.PUBLICADO, nombre, ubicacion, pageable)
                .map(evento -> EventoMapper.toResponseDTO(evento, pricingContext));
    }

    // T-20 (Persona 1)
    public EventoResponseDTO obtenerDetalle(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
        return EventoMapper.toResponseDTO(evento, pricingContext);
    }

    // T-22 (Persona 6)
    @Transactional
    public EventoResponseDTO editarEvento(Long eventoId, EditarEventoDTO dto, Long organizadorId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
        if (!evento.getOrganizador().getId().equals(organizadorId)) {
            throw new UnauthorizedActionException("Solo el organizador creador puede editar este evento");
        }
        if (dto.getAforoMaximo() < evento.getEntradasVendidas()) {
            throw new BusinessRuleException("No se puede reducir el aforo por debajo de las entradas vendidas");
        }

        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFecha(dto.getFecha());
        evento.setHora(dto.getHora());
        evento.setUbicacion(dto.getUbicacion());
        evento.setAforoMaximo(dto.getAforoMaximo());
        evento.setPrecioBase(dto.getPrecioBase());

        return EventoMapper.toResponseDTO(eventoRepository.save(evento), pricingContext);
    }

    // T-24 (Persona 6)
    @Transactional
    public void eliminarEvento(Long eventoId, Long organizadorId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
        if (!evento.getOrganizador().getId().equals(organizadorId)) {
            throw new UnauthorizedActionException("Solo el organizador creador puede eliminar este evento");
        }
        if (evento.getEntradasVendidas() > 0) {
            throw new BusinessRuleException("No se puede eliminar: tiene " + evento.getEntradasVendidas() + " entradas vendidas");
        }
        eventoRepository.delete(evento);
    }
    @Autowired
    private PricingContext pricingContext;
}
