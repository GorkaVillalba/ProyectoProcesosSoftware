package com.ProyectoProcesosSoftware.pricing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Contexto del patrón <em>Strategy</em> para el cálculo del precio dinámico
 * de las entradas de un evento.
 *
 * <h2>Patrón Strategy</h2>
 *
 * <p>Esta clase desempeña el papel de <strong>Context</strong> dentro del
 * patrón de diseño <em>Strategy</em> (GoF). El subsistema de pricing está
 * formado por:</p>
 *
 * <ul>
 *   <li><strong>Strategy</strong> — la interfaz {@link PricingStrategy},
 *       que define el contrato común {@code calcularPrecio(...)} para
 *       cualquier política de precios futura.</li>
 *   <li><strong>ConcreteStrategy</strong> — las implementaciones
 *       {@link EarlyBirdPricing}, {@link RegularPricing} y
 *       {@link LastMinutePricing}, cada una con su multiplicador propio.</li>
 *   <li><strong>Context</strong> — esta clase, {@code PricingContext}, que
 *       mantiene referencias a las tres estrategias inyectadas por Spring
 *       y elige cuál aplicar en cada llamada en función del nivel de
 *       ocupación del evento.</li>
 * </ul>
 *
 * <p>Las ventajas de aplicar Strategy aquí son las habituales del patrón:</p>
 * <ul>
 *   <li>Los clientes ({@code EventoService}, {@code TicketService}, etc.)
 *       no conocen las implementaciones concretas: invocan a este contexto
 *       y obtienen el precio adecuado sin <em>switch</em> ni
 *       <em>if-else</em> repartidos por el código.</li>
 *   <li>Añadir una nueva política de precios (por ejemplo, una estrategia
 *       de descuento por código promocional) consiste en crear una nueva
 *       implementación de {@link PricingStrategy}, registrarla como bean
 *       con su {@link Qualifier} y enchufarla aquí, sin tocar los
 *       servicios que ya usan el contexto.</li>
 *   <li>Cada estrategia es trivialmente testable de forma aislada.</li>
 * </ul>
 *
 * <h2>Reglas de selección de estrategia</h2>
 *
 * <p>El nombre de la estrategia y el {@link PricingStrategy} concreto se
 * deciden en función del porcentaje de ocupación
 * <code>(entradasVendidas / aforoMaximo) × 100</code>:</p>
 *
 * <ul>
 *   <li><b>EarlyBird</b>  — porcentaje de ocupación en <code>[0 %, 50 %]</code>
 *       (también si {@code aforoMaximo <= 0}, como salvaguarda).</li>
 *   <li><b>Regular</b>    — porcentaje de ocupación en <code>(50 %, 80 %]</code>.</li>
 *   <li><b>LastMinute</b> — porcentaje de ocupación en <code>(80 %, 100 %]</code>.</li>
 * </ul>
 *
 * <p>Los métodos {@link #calcularPrecio(BigDecimal, int, int)} y
 * {@link #nombreEstrategia(int, int)} comparten exactamente la misma
 * lógica de selección para garantizar que el precio cobrado y la
 * etiqueta informativa devuelta al cliente siempre coinciden.</p>
 *
 * @author Equipo Proyecto Procesos Software
 * @see PricingStrategy
 * @see EarlyBirdPricing
 * @see RegularPricing
 * @see LastMinutePricing
 */
@Component
public class PricingContext {

    private final PricingStrategy earlyBird;
    private final PricingStrategy regular;
    private final PricingStrategy lastMinute;

    /**
     * Construye el contexto inyectando las tres estrategias concretas
     * mediante sus respectivos {@link Qualifier} de Spring.
     *
     * <p>El uso de inyección por constructor garantiza que el contexto no
     * pueda existir en un estado parcial (todas las estrategias son
     * obligatorias) y facilita el testing con dobles.</p>
     *
     * @param earlyBird  estrategia para ocupación baja (bean con qualifier
     *                   {@code "earlyBird"}).
     * @param regular    estrategia para ocupación media (bean con qualifier
     *                   {@code "regular"}).
     * @param lastMinute estrategia para ocupación alta (bean con qualifier
     *                   {@code "lastMinute"}).
     */
    @Autowired
    public PricingContext(
            @Qualifier("earlyBird")  PricingStrategy earlyBird,
            @Qualifier("regular")    PricingStrategy regular,
            @Qualifier("lastMinute") PricingStrategy lastMinute) {
        this.earlyBird  = earlyBird;
        this.regular    = regular;
        this.lastMinute = lastMinute;
    }

    /**
     * Calcula el precio final de una entrada delegando en la estrategia
     * apropiada para el nivel de ocupación actual del evento.
     *
     * <p>Equivale a seleccionar la estrategia con
     * {@link #seleccionarEstrategia(int, int)} y llamar a su método
     * {@link PricingStrategy#calcularPrecio(BigDecimal, int, int)}.</p>
     *
     * @param precioBase       precio base configurado por el organizador,
     *                         en euros. Debe ser no nulo y no negativo.
     * @param entradasVendidas número de entradas ya vendidas en el momento
     *                         del cálculo. {@code >= 0}.
     * @param aforoMaximo      aforo total del evento. Si es {@code <= 0} se
     *                         considera caso degenerado y se aplica
     *                         {@link EarlyBirdPricing}.
     * @return el precio final que debe pagar el asistente, redondeado a 2
     *         decimales por las estrategias que aplican multiplicador.
     */
    public BigDecimal calcularPrecio(BigDecimal precioBase, int entradasVendidas, int aforoMaximo) {
        return seleccionarEstrategia(entradasVendidas, aforoMaximo)
                .calcularPrecio(precioBase, entradasVendidas, aforoMaximo);
    }

    /**
     * Devuelve el nombre simbólico de la estrategia que se aplicaría con la
     * ocupación indicada.
     *
     * <p>Pensado para acompañar al precio en las respuestas de la API
     * (DTOs de ticket o de detalle de evento), de modo que el cliente
     * pueda explicar al usuario por qué se le cobra un determinado precio
     * sin necesidad de inspeccionar la implementación.</p>
     *
     * @param entradasVendidas número de entradas vendidas.
     * @param aforoMaximo      aforo total del evento; si es {@code <= 0} se
     *                         devuelve {@code "EarlyBird"} como caso
     *                         defensivo.
     * @return uno de {@code "EarlyBird"}, {@code "Regular"} o
     *         {@code "LastMinute"} según el rango de ocupación.
     */
    public String nombreEstrategia(int entradasVendidas, int aforoMaximo) {
        if (aforoMaximo <= 0) return "EarlyBird";
        double pct = (double) entradasVendidas / aforoMaximo * 100;
        if (pct <= 50) return "EarlyBird";
        if (pct <= 80) return "Regular";
        return "LastMinute";
    }

    /**
     * Selecciona la implementación concreta de {@link PricingStrategy}
     * aplicable al evento según su porcentaje de ocupación actual.
     *
     * <p>La tabla de selección es:</p>
     * <ul>
     *   <li>{@code aforoMaximo <= 0}        → {@link EarlyBirdPricing}.</li>
     *   <li>ocupación en {@code [0%, 50%]}  → {@link EarlyBirdPricing}.</li>
     *   <li>ocupación en {@code (50%, 80%]} → {@link RegularPricing}.</li>
     *   <li>ocupación en {@code (80%, 100%]}→ {@link LastMinutePricing}.</li>
     * </ul>
     *
     * <p>Es método privado porque la elección de estrategia es un detalle
     * interno del contexto; los clientes externos solo deberían interactuar
     * con {@link #calcularPrecio(BigDecimal, int, int)} y
     * {@link #nombreEstrategia(int, int)}.</p>
     *
     * @param entradasVendidas número de entradas vendidas.
     * @param aforoMaximo      aforo total del evento.
     * @return la estrategia concreta a aplicar para esos valores.
     */
    private PricingStrategy seleccionarEstrategia(int entradasVendidas, int aforoMaximo) {
        if (aforoMaximo <= 0) return earlyBird;
        double pct = (double) entradasVendidas / aforoMaximo * 100;
        if (pct <= 50) return earlyBird;
        if (pct <= 80) return regular;
        return lastMinute;
    }
}