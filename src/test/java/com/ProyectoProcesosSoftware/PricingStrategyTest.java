package com.ProyectoProcesosSoftware;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ProyectoProcesosSoftware.pricing.EarlyBirdPricing;
import com.ProyectoProcesosSoftware.pricing.LastMinutePricing;
import com.ProyectoProcesosSoftware.pricing.PricingContext;
import com.ProyectoProcesosSoftware.pricing.RegularPricing;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class PricingStrategyTest {

    private final BigDecimal BASE = new BigDecimal("100.00");

    @Test @DisplayName("EarlyBird: 0% → precio base")
    void earlyBird_cero() {
        assertThat(new EarlyBirdPricing().calcularPrecio(BASE, 0, 100))
            .isEqualByComparingTo("100.00");
    }

    @Test @DisplayName("EarlyBird: 50% → precio base")
    void earlyBird_cincuenta() {
        assertThat(new EarlyBirdPricing().calcularPrecio(BASE, 50, 100))
            .isEqualByComparingTo("100.00");
    }

    @Test @DisplayName("Regular: 51% → precio × 1.25")
    void regular_cincuentaYUno() {
        assertThat(new RegularPricing().calcularPrecio(BASE, 51, 100))
            .isEqualByComparingTo("125.00");
    }

    @Test @DisplayName("Regular: 80% → precio × 1.25")
    void regular_ochenta() {
        assertThat(new RegularPricing().calcularPrecio(BASE, 80, 100))
            .isEqualByComparingTo("125.00");
    }

    @Test @DisplayName("LastMinute: 81% → precio × 1.50")
    void lastMinute_ochentaYUno() {
        assertThat(new LastMinutePricing().calcularPrecio(BASE, 81, 100))
            .isEqualByComparingTo("150.00");
    }

    @Test @DisplayName("LastMinute: 100% → precio × 1.50")
    void lastMinute_cien() {
        assertThat(new LastMinutePricing().calcularPrecio(BASE, 100, 100))
            .isEqualByComparingTo("150.00");
    }

    @Test @DisplayName("Context selecciona EarlyBird para 0-50%")
    void context_earlyBird() {
        PricingContext ctx = new PricingContext(new EarlyBirdPricing(), new RegularPricing(), new LastMinutePricing());
        assertThat(ctx.calcularPrecio(BASE, 30, 100)).isEqualByComparingTo("100.00");
        assertThat(ctx.nombreEstrategia(30, 100)).isEqualTo("EarlyBird");
    }

    @Test @DisplayName("Context selecciona Regular para 51-80%")
    void context_regular() {
        PricingContext ctx = new PricingContext(new EarlyBirdPricing(), new RegularPricing(), new LastMinutePricing());
        assertThat(ctx.calcularPrecio(BASE, 65, 100)).isEqualByComparingTo("125.00");
        assertThat(ctx.nombreEstrategia(65, 100)).isEqualTo("Regular");
    }

    @Test @DisplayName("Context selecciona LastMinute para 81-100%")
    void context_lastMinute() {
        PricingContext ctx = new PricingContext(new EarlyBirdPricing(), new RegularPricing(), new LastMinutePricing());
        assertThat(ctx.calcularPrecio(BASE, 90, 100)).isEqualByComparingTo("150.00");
        assertThat(ctx.nombreEstrategia(90, 100)).isEqualTo("LastMinute");
    }

    @Test @DisplayName("Context: aforoMaximo 0 no lanza excepción")
    void context_aforoCero() {
        PricingContext ctx = new PricingContext(new EarlyBirdPricing(), new RegularPricing(), new LastMinutePricing());
        assertThat(ctx.calcularPrecio(BASE, 0, 0)).isEqualByComparingTo("100.00");
    }
}