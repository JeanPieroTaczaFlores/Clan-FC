package com.tienda.dto;

import com.tienda.model.Incidencia;

import java.time.OffsetDateTime;

/** Incidencia visible en la pestaña INCIDENCIAS del POS y panel admin. */
public record IncidenciaResponse(
        Long idIncidencia,
        String tipo,
        String estado,
        Long productoId,
        String productoNombre,
        String productoSku,
        Integer garantiaMeses,
        Long ordenId,
        String ordenFolio,
        Integer cantidad,
        String descripcion,
        String reportadoPor,
        OffsetDateTime fechaReporte,
        String resolucion
) {
    public static IncidenciaResponse from(Incidencia i) {
        return new IncidenciaResponse(
                i.getIdIncidencia(),
                i.getTipo().name(),
                i.getEstado().name(),
                i.getProducto().getIdProducto(),
                i.getProducto().getNombre(),
                i.getProducto().getSku(),
                i.getProducto().getGarantiaMeses(),
                i.getOrden() != null ? i.getOrden().getIdOrden() : null,
                i.getOrden() != null ? i.getOrden().getFolio() : null,
                i.getCantidad(),
                i.getDescripcion(),
                i.getReportadoPor().getUsername(),
                i.getFechaReporte(),
                i.getResolucion()
        );
    }
}
