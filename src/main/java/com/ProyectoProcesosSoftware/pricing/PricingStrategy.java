package com.ProyectoProcesosSoftware.pricing;

import java.math.BigDecimal;

/**
 * Contrato común para todas las estrategias de cálculo de precio dinámico
 * de las entradas de un evento.
 *
 * <p>Forma parte de la aplicación del patrón <em>Strategy</em> dentro del
 * subsistema de pricing: cada implementación encapsula una política
 * diferente para transformar el precio base de un evento en el precio
 * final que paga el asistente, en función del porcentaje de ocupación del
 * evento en el instante de la consulta.</p>
 *
 * <p>Las implementaciones actuales son:</p>
 * <ul>
 *   <li>{@link EarlyBirdPricing}  — ocupación 0 % – 50 %, sin recargo (×1.00).</li>
 *   <li>{@link RegularPricing}    — ocupación 51 % – 80 %, recargo +25 % (×1.25).</li>
 *   <li>{@link LastMinutePricing} — ocupación 81 % – 100 %, recargo +50 % (×1.50).</li>
 * </ul>
 *
 * <p>El componente {@link PricingContext} se encarga de seleccionar la
 * estrategia adecuada para cada llamada en función del aforo y de las
 * entradas vendidas, de modo que los clientes (servicios y controladores)
 * no dependen de las implementaciones concretas.</p>
 *
 * @author Equipo Proyecto Procesos Software
 * @see PricingContext
 * @see EarlyBirdPricing
 * @see RegularPricing
 * @see LastMinutePricing
 */
public interface PricingStrategy {

    /**
     * Calcula el precio final de una entrada aplicando la política definida
     * por la estrategia concreta.
     *
     * <p>Las implementaciones reciben tanto el precio base como el contexto
     * de ocupación del evento; algunas estrategias podrán ignorar
     * {@code entradasVendidas} y {@code aforoMaximo} (por ejemplo,
     * {@link EarlyBirdPricing} aplica un multiplicador fijo), pero el
     * contrato los expone uniformemente para permitir futuras estrategias
     * que sí los necesiten.</p>
     *
     * @param precioBase       precio base del evento, en euros. Debe ser
     *                         no nulo y no negativo.
     * @param entradasVendidas número de entradas ya vendidas en el momento
     *                         del cálculo. Debe ser {@code >= 0}.
     * @param aforoMaximo      aforo total del evento. Debe ser {@code > 0}
     *                         cuando se quiera calcular un porcentaje de
     *                         ocupación significativo; el contexto se
     *                         encarga de manejar el caso degenerado de
     *                         aforo nulo o negativo.
     * @return el precio final calculado, redondeado a 2 decimales mediante
     *         {@link java.math.RoundingMode#HALF_UP} en las implementaciones
     *         que aplican multiplicador.
     */
    BigDecimal calcularPrecio(BigDecimal precioBase, int entradasVendidas, int aforoMaximo);
}