package com.ProyectoProcesosSoftware.dto;

import com.ProyectoProcesosSoftware.model.Evento;
import com.ProyectoProcesosSoftware.pricing.PricingContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EventoMapper {

    /**
     * Convierte un Evento a EventoResponseDTO incluyendo datos de reseñas.
     *
     * @param e              entidad Evento
     * @param pricingContext contexto de estrategia de precios
     * @param media          media de puntuaciones (null si no hay reseñas → se devuelve 0.0)
     * @param numResenas     número total de reseñas (0 si no hay ninguna)
     */
    public static EventoResponseDTO toResponseDTO(Evento e, PricingContext pricingContext,
                                                   Double media, long numResenas) {
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

        // US-26: valoraciones — 0.0 y 0 cuando no hay reseñas
        BigDecimal puntuacionMedia = (media != null)
                ? BigDecimal.valueOf(media).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        dto.setPuntuacionMedia(puntuacionMedia);
        dto.setNumeroResenas(numResenas);

        return dto;
    }

    /**
     * Sobrecarga de compatibilidad para llamadas que aún no disponen de datos de reseñas.
     * Establece puntuacionMedia=0.0 y numeroResenas=0.
     *
     * @deprecated Usar {@link #toResponseDTO(Evento, PricingContext, Double, long)} inyectando
     *             los valores desde el servicio para evitar consultas N+1.
     */
    @Deprecated
    public static EventoResponseDTO toResponseDTO(Evento e, PricingContext pricingContext) {
        return toResponseDTO(e, pricingContext, null, 0L);
    }
}