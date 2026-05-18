package com.ProyectoProcesosSoftware.pricing;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Estrategia de pricing <em>EarlyBird</em>: precio base sin recargo alguno.
 *
 * <p>Esta implementación de {@link PricingStrategy} se aplica cuando el
 * evento tiene una <strong>ocupación baja</strong>, definida por
 * {@link PricingContext} como un porcentaje de entradas vendidas
 * comprendido en el rango <code>[0 %, 50 %]</code> sobre el aforo máximo
 * (también se selecciona como caso defensivo cuando el aforo configurado
 * es {@code 0}).</p>
 *
 * <p>Su objetivo de negocio es <strong>incentivar la compra anticipada</strong>:
 * los primeros asistentes en reservar entradas obtienen el precio base
 * publicado por el organizador, sin ningún incremento por demanda.</p>
 *
 * <h2>Multiplicador</h2>
 * <pre>precioFinal = precioBase × 1.00</pre>
 *
 * <p>El método {@link #calcularPrecio(BigDecimal, int, int)} ignora los
 * parámetros de ocupación porque el resultado es siempre el precio base.
 * La firma se mantiene homogénea con el resto de estrategias para respetar
 * el contrato del patrón <em>Strategy</em>.</p>
 *
 * @author Equipo Proyecto Procesos Software
 * @see PricingStrategy
 * @see PricingContext
 * @see RegularPricing
 * @see LastMinutePricing
 */
@Component
@Qualifier("earlyBird")
public class EarlyBirdPricing implements PricingStrategy {

    /**
     * Devuelve el precio base sin aplicar recargo.
     *
     * <p>En esta estrategia el porcentaje de ocupación es irrelevante:
     * el precio final coincide siempre con el precio base configurado por
     * el organizador del evento.</p>
     *
     * @param precioBase       precio base del evento, en euros.
     * @param entradasVendidas número de entradas vendidas (no utilizado en
     *                         esta estrategia).
     * @param aforoMaximo      aforo total del evento (no utilizado en esta
     *                         estrategia).
     * @return el mismo {@code precioBase} recibido, sin transformación.
     */
    @Override
    public BigDecimal calcularPrecio(BigDecimal precioBase, int entradasVendidas, int aforoMaximo) {
        return precioBase;
    }
}