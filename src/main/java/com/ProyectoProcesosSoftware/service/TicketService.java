package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.TicketMapper;
import com.ProyectoProcesosSoftware.dto.TicketResponseDTO;
import com.ProyectoProcesosSoftware.exception.BusinessRuleException;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.exception.UnauthorizedActionException;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.pricing.PricingContext;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.TicketRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PricingContext pricingContext;

    // US-17: comprar entrada con precio calculado en el momento de la compra
    @Transactional
    public TicketResponseDTO comprarEntrada(Long eventoId, Long asistenteId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + eventoId));
        if (evento.getEstado() == EstadoEvento.AGOTADO) {
            throw new BusinessRuleException("El evento está agotado");
        }
        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new BusinessRuleException("Solo se pueden comprar entradas de eventos publicados");
        }

        if (evento.getEntradasVendidas() >= evento.getAforoMaximo()) {
            throw new BusinessRuleException("No quedan plazas disponibles para este evento");
        }

        Usuario asistente = usuarioRepository.findById(asistenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + asistenteId));

        if (asistente.getRol() != Rol.ASISTENTE) {
            throw new UnauthorizedActionException("Solo los asistentes pueden comprar entradas");
        }

        if (ticketRepository.existsByEventoIdAndAsistenteIdAndEstado(
                eventoId, asistenteId, TicketStatus.VALIDO)) {
            throw new BusinessRuleException("Ya tienes una entrada para este evento");
        }

        BigDecimal precioFinal = pricingContext.calcularPrecio(
                evento.getPrecioBase(),
                evento.getEntradasVendidas(),
                evento.getAforoMaximo()
        );
        String estrategia = pricingContext.nombreEstrategia(
                evento.getEntradasVendidas(),
                evento.getAforoMaximo()
        );

        Ticket ticket = new Ticket();
        ticket.setEvento(evento);
        ticket.setAsistente(asistente);
        ticket.setPrecioFinal(precioFinal);

        evento.setEntradasVendidas(evento.getEntradasVendidas() + 1);
        if (evento.getEntradasVendidas() >= evento.getAforoMaximo()) {
            evento.setEstado(EstadoEvento.AGOTADO);
        }
        eventoRepository.save(evento);

        Ticket guardado = ticketRepository.save(ticket);
        return TicketMapper.TicketResponseDTO(guardado, estrategia);
    }

    // T-13 + T-15: cancelar entrada con regla de 48h y liberar plaza
    @Transactional
    public TicketResponseDTO cancelarEntrada(Long ticketId, Long usuarioId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada no encontrada con id: " + ticketId));

        if (!ticket.getAsistente().getId().equals(usuarioId)) {
            throw new UnauthorizedActionException("No puedes cancelar una entrada que no es tuya");
        }

        if (ticket.getEstado() != TicketStatus.VALIDO) {
            throw new BusinessRuleException("La entrada ya está cancelada");
        }

        Evento evento = ticket.getEvento();
        java.time.LocalDateTime fechaEvento =
                java.time.LocalDateTime.of(evento.getFecha(), evento.getHora());
        long minutosHastaEvento = java.time.Duration.between(
                java.time.LocalDateTime.now(), fechaEvento).toMinutes();
        if (minutosHastaEvento <= 48 * 60) {
            throw new BusinessRuleException(
                    "No se puede cancelar: faltan menos de 48h para el evento");
        }

        ticket.setEstado(TicketStatus.CANCELADO);

        evento.setEntradasVendidas(Math.max(0, evento.getEntradasVendidas() - 1));
        if (evento.getEstado() == EstadoEvento.AGOTADO
                && evento.getEntradasVendidas() < evento.getAforoMaximo()) {
            evento.setEstado(EstadoEvento.PUBLICADO);
        }
        eventoRepository.save(evento);

        Ticket guardado = ticketRepository.save(ticket);
        String estrategia = pricingContext.nombreEstrategia(
                evento.getEntradasVendidas(), evento.getAforoMaximo());
        return TicketMapper.TicketResponseDTO(guardado, estrategia);
    }

    public List<TicketResponseDTO> misEntradas(Long asistenteId) {
        return ticketRepository.findByAsistenteId(asistenteId)
                .stream()
                .map(t -> TicketMapper.TicketResponseDTO(t, pricingContext.nombreEstrategia(
                        t.getEvento().getEntradasVendidas(),
                        t.getEvento().getAforoMaximo())))
                .toList();
    }

    // Consultar mis entradas ordenadas por fecha de compra descendente
    public List<TicketResponseDTO> getMisEntradas(Long usuarioId) {
        return ticketRepository.findByAsistenteIdOrderByFechaCompraDesc(usuarioId)
                .stream()
                .map(t -> TicketMapper.TicketResponseDTO(t, pricingContext.nombreEstrategia(
                        t.getEvento().getEntradasVendidas(),
                        t.getEvento().getAforoMaximo())))
                .toList();
    }
}