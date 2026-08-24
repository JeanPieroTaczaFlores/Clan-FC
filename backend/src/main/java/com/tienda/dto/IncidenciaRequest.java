package com.tienda.dto;

import jakarta.validation.constraints.*;

/**
 * Reporte de producto dañado/devuelto/en garantía desde el POS.
 * Si trae ordenId, se vincula a la venta original para trazabilidad.
 */
public record IncidenciaRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        Long ordenId,

        /** DEVOLUCION | DEFECTO | GARANTIA (normalizado en el servicio). */
        String tipo,

        @NotNull @Min(1) @Max(1000)
        Integer cantidad,

        @NotBlank @Size(min = 5, max = 500)
        String descripcion
) {
}
