package com.ProyectoProcesosSoftware.pricing;

import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal calcularPrecio(BigDecimal precioBase, int entradasVendidas, int aforoMaximo);
}