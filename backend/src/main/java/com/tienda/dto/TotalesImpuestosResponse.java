package com.tienda.dto;

import java.math.BigDecimal;

/**
 * Desglose de impuestos calculado por TaxCalculationService.
 * El frontend lo usa para el cálculo EN VIVO del carrito/POS y las órdenes
 * lo persisten como snapshot fiscal.
 */
public record TotalesImpuestosResponse(
        BigDecimal subtotal,      // suma de precios base (sin IVA)
        BigDecimal tasaIva,       // % aplicado (resuelto por país + régimen)
        String regimenFiscal,     // EXENTO | GENERAL | REDUCIDO
        BigDecimal iva,           // importe del impuesto
        BigDecimal total,         // subtotal + iva
        String paisCodigo,        // ISO 3166-1 alpha-2 (null = consumidor final)
        String paisNombre,
        String banderaEmoji       // 🇲🇽, 🇨🇴... derivado del código ISO
) {
}
