package com.tienda.dto;

import java.math.BigDecimal;

/** Línea de la orden dentro de OrdenResponse (ticket). */
public record OrdenItemResponse(
        Long idDetalle,
        String sku,
        String nombreProducto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal ivaLinea,
        BigDecimal subtotalLinea
) {
}
