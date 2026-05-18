package com.ProyectoProcesosSoftware.pricing;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estrategia de pricing <em>Regular</em>: recargo del 25 % sobre el precio
 * base.
 *
 * <p>Esta implementación de {@link PricingStrategy} se aplica cuando el
 * evento tiene una <strong>ocupación media</strong>, definida por
 * {@link PricingContext} como un porcentaje de entradas vendidas dentro
 * del rango <code>(50 %, 80 %]</code> sobre el aforo máximo.</p>
 *
 * <p>Su objetivo de negocio es <strong>monetizar la demanda creciente</strong>:
 * cuando ya se ha vendido más de la mitad del aforo se considera que la
 * disponibilidad empieza a ser un valor en sí mismo y se aplica un
 * recargo moderado sobre el precio base.</p>
 *
 * <h2>Multiplicador</h2>
 * <pre>precioFinal = precioBase × 1.25  (redondeo HALF_UP a 2 decimales)</pre>
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
 * @see LastMinutePricing
 */
@Component
@Qualifier("regular")
public class RegularPricing implements PricingStrategy {

    /**
     * Multiplicador aplicado sobre el precio base en esta estrategia ({@code 1.25}),
     * equivalente a un recargo del 25 %.
     */
    private static final BigDecimal MULTIPLICADOR = new BigDecimal("1.25");

    /**
     * Calcula el precio final aplicando un recargo del 25 % sobre el precio
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
     * @return el precio base multiplicado por {@code 1.25} y redondeado a 2
     *         decimales con {@link RoundingMode#HALF_UP}.
     */
    @Override
    public BigDecimal calcularPrecio(BigDecimal precioBase, int entradasVendidas, int aforoMaximo) {
        return precioBase.multiply(MULTIPLICADOR).setScale(2, RoundingMode.HALF_UP);
    }
}