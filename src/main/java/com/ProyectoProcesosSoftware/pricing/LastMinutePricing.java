package com.ProyectoProcesosSoftware.pricing;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estrategia de pricing <em>LastMinute</em>: recargo del 50 % sobre el
 * precio base.
 *
 * <p>Esta implementación de {@link PricingStrategy} se aplica cuando el
 * evento tiene una <strong>ocupación alta</strong>, definida por
 * {@link PricingContext} como un porcentaje de entradas vendidas superior
 * al 80 % del aforo máximo (rango <code>(80 %, 100 %]</code>).</p>
 *
 * <p>Su objetivo de negocio es <strong>maximizar el ingreso de las últimas
 * plazas</strong>: cuando la demanda es muy elevada y la disponibilidad
 * residual escasa, el sistema aplica el recargo más alto previsto. Esto
 * también desincentiva la compra impulsiva justo antes del agotamiento
 * total del aforo, premiando indirectamente a quienes compraron con
 * antelación bajo {@link EarlyBirdPricing}.</p>
 *
 * <h2>Multiplicador</h2>
 * <pre>precioFinal = precioBase × 1.50  (redondeo HALF_UP a 2 decimales)</pre>
 *
 * <p>El resultado se redondea a céntimos mediante
 * {@link RoundingMode#HALF_UP} para evitar precios con más de dos
 * decimales que serían incompatibles con la representación monetaria de
 * la pasarela de pago.</p>
 *
 * @author Equipo Proyecto Procesos Software
 * @see PricingStrategy
 * @see PricingContext
 * @see EarlyBirdPricing
 * @see RegularPricing
 */
@Component
@Qualifier("lastMinute")
public class LastMinutePricing implements PricingStrategy {

    /**
     * Multiplicador aplicado sobre el precio base en esta estrategia ({@code 1.50}),
     * equivalente a un recargo del 50 %.
     */
    private static final BigDecimal MULTIPLICADOR = new BigDecimal("1.50");

    /**
     * Calcula el precio final aplicando un recargo del 50 % sobre el precio
     * base.
     *
     * <p>Los parámetros {@code entradasVendidas} y {@code aforoMaximo} no
     * intervienen en el cálculo de esta estrategia porque el porcentaje de
     * recargo es fijo dentro de su rango de aplicación; la selección del
     * rango la realiza {@link PricingContext} <em>antes</em> de delegar en
     * esta clase. Se mantienen en la firma para respetar el contrato del
     * patrón <em>Strategy</em>.</p>
     *
     * @param precioBase       precio base del evento, en euros.
     * @param entradasVendidas número de entradas vendidas (no utilizado).
     * @param aforoMaximo      aforo total del evento (no utilizado).
     * @return el precio base multiplicado por {@code 1.50} y redondeado a 2
     *         decimales con {@link RoundingMode#HALF_UP}.
     */
    @Override
    public BigDecimal calcularPrecio(BigDecimal precioBase, int entradasVendidas, int aforoMaximo) {
        return precioBase.multiply(MULTIPLICADOR).setScale(2, RoundingMode.HALF_UP);
    }
}