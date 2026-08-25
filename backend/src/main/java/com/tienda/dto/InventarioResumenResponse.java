package com.tienda.dto;

import java.math.BigDecimal;

/**
 * Resumen del inventario para el dashboard.
 */
public record InventarioResumenResponse(
        Integer totalProductos,
        BigDecimal valorInventario,
        Integer stockBajo,
        Integer totalCategorias,
        List<StockSede> porSede
) {
    public record StockSede(
            Long idSede,
            String nombre,
            BigDecimal valorInventario,
            Integer productosStockBajo,
            Integer totalProductos
    ) {
    }
}
