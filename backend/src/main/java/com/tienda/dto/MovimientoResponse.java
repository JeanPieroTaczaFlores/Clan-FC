package com.tienda.dto;

import com.tienda.model.MovimientoAlmacen;

import java.time.OffsetDateTime;

/** Línea del kardex para la vista ALMACÉN del POS. */
public record MovimientoResponse(
        Long idMovimiento,
        String tipo,
        String productoNombre,
        String productoSku,
        Integer cantidad,
        Integer stockResultante,
        String referencia,
        String nota,
        String proveedorNombre,
        String usuarioUsername,
        OffsetDateTime fecha
) {
    public static MovimientoResponse from(MovimientoAlmacen m) {
        return new MovimientoResponse(
                m.getIdMovimiento(),
                m.getTipo().name(),
                m.getProducto().getNombre(),
                m.getProducto().getSku(),
                m.getCantidad(),
                m.getStockResultante(),
                m.getReferencia(),
                m.getNota(),
                m.getProveedor() != null ? m.getProveedor().getNombre() : null,
                m.getUsuario().getUsername(),
                m.getFecha()
        );
    }
}
