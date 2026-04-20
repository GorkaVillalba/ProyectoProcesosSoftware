package com.ProyectoProcesosSoftware.pricing;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@Qualifier("earlyBird")
public class EarlyBirdPricing implements PricingStrategy {

    @Override
    public BigDecimal calcularPrecio(BigDecimal precioBase, int entradasVendidas, int aforoMaximo) {
        return precioBase;
    }
}