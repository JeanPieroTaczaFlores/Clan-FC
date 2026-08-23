package com.tienda.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Petición de alta/edición de producto (ADMIN).
 * La validación se aplica automáticamente con @Valid en el controller.
 */
public record ProductoRequest(
        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 40)
        String sku,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        String nombre,

        @Size(max = 500)
        String descripcion,

        @NotNull @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        BigDecimal precioBase,

        @NotNull @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock,

        @NotNull @PositiveOrZero
        Integer stockMinimo,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @Size(max = 300)
        String imagenUrl,

        Boolean activo
) {
}
