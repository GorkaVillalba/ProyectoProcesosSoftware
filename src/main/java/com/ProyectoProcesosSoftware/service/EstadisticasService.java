package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.EstadisticasOrganizadorDTO;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.exception.UnauthorizedActionException;
import com.ProyectoProcesosSoftware.model.Evento;
import com.ProyectoProcesosSoftware.model.Rol;
import com.ProyectoProcesosSoftware.model.Usuario;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.TicketRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * US-28 / T-28.3: calcula los agregados del panel del organizador a partir
 * de sus eventos y de los tickets VALIDO vendidos en ellos.
 */
@Service
public class EstadisticasService {

    @Autowired private EventoRepository eventoRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    public EstadisticasOrganizadorDTO obtenerEstadisticasOrganizador(Long organizadorId) {
        Usuario user = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + organizadorId));
        if (user.getRol() != Rol.ORGANIZADOR) {
            throw new UnauthorizedActionException("Solo los organizadores tienen panel de estadísticas");
        }

        List<Evento> eventos = eventoRepository.findByOrganizadorId(organizadorId);
        EstadisticasOrganizadorDTO dto = new EstadisticasOrganizadorDTO();

        if (eventos.isEmpty()) {
            dto.setNumeroEventos(0);
            dto.setEntradasVendidasTotales(0);
            dto.setIngresosTotales(BigDecimal.ZERO);
            dto.setPorcentajeOcupacionMedia(BigDecimal.ZERO);
            return dto;
        }

        long entradas = eventos.stream()
                .mapToLong(e -> e.getEntradasVendidas() == null ? 0 : e.getEntradasVendidas())
                .sum();

        BigDecimal ingresos = ticketRepository.sumarIngresosByOrganizadorId(organizadorId);
        if (ingresos == null) ingresos = BigDecimal.ZERO;

        BigDecimal sumaPorcentajes = eventos.stream()
                .filter(e -> e.getAforoMaximo() != null && e.getAforoMaximo() > 0)
                .map(e -> BigDecimal.valueOf(e.getEntradasVendidas())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(e.getAforoMaximo()), 4, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ocupacionMedia = eventos.isEmpty()
                ? BigDecimal.ZERO
                : sumaPorcentajes.divide(BigDecimal.valueOf(eventos.size()), 2, RoundingMode.HALF_UP);

        dto.setNumeroEventos(eventos.size());
        dto.setEntradasVendidasTotales(entradas);
        dto.setIngresosTotales(ingresos.setScale(2, RoundingMode.HALF_UP));
        dto.setPorcentajeOcupacionMedia(ocupacionMedia);
        return dto;
    }
}