package com.tienda.dto;

import java.util.List;

/**
 * Alertas del sistema: stock bajo, incidencias pendientes, cajas cerradas.
 */
public record AlertasStockResponse(
        List<Alerta> alertas,
        Integer totalCriticas,
        Integer totalAdvertencias,
        Integer totalInfo
) {
    public record Alerta(
            String severidad,   // CRITICA, ALERTA, INFO
            String tipo,       // STOCK_Bajo, INCIDENCIA, CAJA
            String titulo,
            String descripcion,
            Long idRelacionado
    ) {
    }
}
