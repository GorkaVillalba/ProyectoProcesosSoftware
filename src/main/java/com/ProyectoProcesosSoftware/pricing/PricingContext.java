package com.ProyectoProcesosSoftware.pricing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class PricingContext {

    private final PricingStrategy earlyBird;
    private final PricingStrategy regular;
    private final PricingStrategy lastMinute;

    @Autowired
    public PricingContext(
            @Qualifier("earlyBird")  PricingStrategy earlyBird,
            @Qualifier("regular")    PricingStrategy regular,
            @Qualifier("lastMinute") PricingStrategy lastMinute) {
        this.earlyBird  = earlyBird;
        this.regular    = regular;
        this.lastMinute = lastMinute;
    }

    public BigDecimal calcularPrecio(BigDecimal precioBase, int entradasVendidas, int aforoMaximo) {
        return seleccionarEstrategia(entradasVendidas, aforoMaximo)
                .calcularPrecio(precioBase, entradasVendidas, aforoMaximo);
    }

    public String nombreEstrategia(int entradasVendidas, int aforoMaximo) {
        if (aforoMaximo <= 0) return "EarlyBird";
        double pct = (double) entradasVendidas / aforoMaximo * 100;
        if (pct <= 50) return "EarlyBird";
        if (pct <= 80) return "Regular";
        return "LastMinute";
    }

    private PricingStrategy seleccionarEstrategia(int entradasVendidas, int aforoMaximo) {
        if (aforoMaximo <= 0) return earlyBird;
        double pct = (double) entradasVendidas / aforoMaximo * 100;
        if (pct <= 50) return earlyBird;
        if (pct <= 80) return regular;
        return lastMinute;
    }
}