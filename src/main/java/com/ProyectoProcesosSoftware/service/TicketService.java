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
import java.util.stream.Collectors;

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

        if (ticketRepository.existsByEventoIdAndAsistenteId(eventoId, asistenteId)) {
            throw new BusinessRuleException("Ya tienes una entrada para este evento");
        }

        // US-17: el precio se calcula en el momento de la compra con la estrategia activa
        BigDecimal precioFinal = pricingContext.calcularPrecio(
                evento.getPrecioBase(),
                evento.getEntradasVendidas(),
                evento.getAforoMaximo()
        );
        String estrategia = pricingContext.nombreEstrategia(
                evento.getEntradasVendidas(),
                evento.getAforoMaximo()
        );

        // Crear el ticket guardando el precioFinal calculado
        Ticket ticket = new Ticket();
        ticket.setEvento(evento);
        ticket.setAsistente(asistente);
        ticket.setPrecioFinal(precioFinal);

        // Incrementar entradas vendidas
        evento.setEntradasVendidas(evento.getEntradasVendidas() + 1);
        if (evento.getEntradasVendidas() >= evento.getAforoMaximo()) {
            evento.setEstado(EstadoEvento.AGOTADO);
        }
        eventoRepository.save(evento);

        Ticket guardado = ticketRepository.save(ticket);
        return TicketMapper.TicketResponseDTO(guardado, estrategia);
    }

    public List<TicketResponseDTO> misEntradas(Long asistenteId) {
        return ticketRepository.findByAsistenteId(asistenteId)
                .stream()
                .map(t -> TicketMapper.TicketResponseDTO(t, pricingContext.nombreEstrategia(
                        t.getEvento().getEntradasVendidas(),
                        t.getEvento().getAforoMaximo())))
                .collect(Collectors.toList());
    }

    //Consultar mis entradas ordenadas por fecha de compra descendente
    public List<TicketResponseDTO> getMisEntradas(Long usuarioId) {
        return ticketRepository.findByAsistenteIdOrderByFechaCompraDesc(usuarioId)
                .stream()
                .map(t -> TicketMapper.TicketResponseDTO(t, pricingContext.nombreEstrategia(
                        t.getEvento().getEntradasVendidas(),
                        t.getEvento().getAforoMaximo())))
                .collect(Collectors.toList());
    }

    
}