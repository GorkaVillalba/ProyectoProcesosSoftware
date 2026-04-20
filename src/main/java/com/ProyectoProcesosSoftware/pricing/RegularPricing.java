package com.ProyectoProcesosSoftware.pricing;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Qualifier("regular")
public class RegularPricing implements PricingStrategy {

    private static final BigDecimal MULTIPLICADOR = new BigDecimal("1.25");

    @Override
    public BigDecimal calcularPrecio(BigDecimal precioBase, int entradasVendidas, int aforoMaximo) {
        return precioBase.multiply(MULTIPLICADOR).setScale(2, RoundingMode.HALF_UP);
    }
}