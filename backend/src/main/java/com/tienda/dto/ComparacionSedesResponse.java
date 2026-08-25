package com.tienda.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta de comparación de sedes para el dashboard ejecutivo.
 */
public record ComparacionSedesResponse(
        List<SedeResumen> sedes
) {
    public record SedeResumen(
            Long idSede,
            String nombre,
            BigDecimal ventasTotales,
            Integer totalOrdenes,
            BigDecimal ticketPromedio,
            BigDecimal efectivo,
            Integer productosStock,
            Integer productosStockBajo,
            Integer incidencias
    ) {
    }
}
