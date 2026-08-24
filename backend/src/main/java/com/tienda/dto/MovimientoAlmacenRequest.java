package com.tienda.dto;

import jakarta.validation.constraints.*;

/**
 * Registro de movimiento de almacén desde el POS (cajero/admin).
 * El tipo determina si suma o resta stock:
 *   ENTRADA/DEVOLUCION suman | MERMA/AJUSTE restan (AJUSTE con nota).
 */
public record MovimientoAlmacenRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull @Min(1) @Max(10_000)
        Integer cantidad,

        /** Se normaliza a ENTRADA por defecto; también MERMA/AJUSTE/DEVOLUCION. */
        String tipo,

        Long proveedorId,

        @Size(max = 60)
        String referencia,

        @Size(max = 255)
        String nota
) {
}
