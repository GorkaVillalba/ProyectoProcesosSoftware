package com.ProyectoProcesosSoftware.dto;

import com.ProyectoProcesosSoftware.model.Evento;
import com.ProyectoProcesosSoftware.pricing.PricingContext;

public class EventoMapper {

    public static EventoResponseDTO toResponseDTO(Evento e, PricingContext pricingContext) {
        EventoResponseDTO dto = new EventoResponseDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setDescripcion(e.getDescripcion());
        dto.setFecha(e.getFecha());
        dto.setHora(e.getHora());
        dto.setUbicacion(e.getUbicacion());
        dto.setAforoMaximo(e.getAforoMaximo());
        dto.setEntradasVendidas(e.getEntradasVendidas());
        dto.setPlazasDisponibles(e.getAforoMaximo() - e.getEntradasVendidas());
        dto.setPrecioBase(e.getPrecioBase());
        dto.setEstado(e.getEstado().name());
        dto.setOrganizadorNombre(e.getOrganizador().getNombre());
        dto.setOrganizadorId(e.getOrganizador().getId());

        // US-13: precio dinámico por Strategy
        dto.setPrecioActual(
            pricingContext.calcularPrecio(e.getPrecioBase(), e.getEntradasVendidas(), e.getAforoMaximo())
        );
        dto.setEstrategiaPrecio(
            pricingContext.nombreEstrategia(e.getEntradasVendidas(), e.getAforoMaximo())
        );

        return dto;
    }
}