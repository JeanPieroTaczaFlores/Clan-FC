package com.tienda.dto;

import com.tienda.dto.ProductoResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * REPORTE SEMANAL para el cajero (últimos 7 días).
 *  - dias: ventas por día para la gráfica de barras
 *  - topProductos: unidades vendidas por producto
 *  - bajoStock: productos que requieren reposición
 */
public record ReporteSemanalResponse(
        BigDecimal totalVentas,
        Integer numeroOrdenes,
        BigDecimal ticketPromedio,
        List<DiaVenta> dias,
        List<ProductoVendido> topProductos,
        List<ProductoResponse> bajoStock
) {
    /** Venta agregada de un día. */
    public record DiaVenta(String fecha, String diaSemana, BigDecimal total, Integer ordenes) {
    }

    /** Unidades e importe vendidos de un producto en la semana. */
    public record ProductoVendido(String sku, String nombre, Integer unidades, BigDecimal importe) {
    }
}
