package com.tienda.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen financiero consolidado para el dashboard ejecutivo.
 */
public record ResumenFinancieroResponse(
        BigDecimal ingresosTotales,
        BigDecimal ivaTotal,
        BigDecimal efectivoTotal,
        List<SedeFinanciera> porSede,
        Integer totalOrdenes,
        Integer totalProductos,
        Integer totalIncidencias
) {
    public record SedeFinanciera(
            Long idSede,
            String nombre,
            BigDecimal ingresos,
            BigDecimal iva,
            BigDecimal efectivo,
            Integer ordenes,
            Integer retiros
    ) {
    }
}
