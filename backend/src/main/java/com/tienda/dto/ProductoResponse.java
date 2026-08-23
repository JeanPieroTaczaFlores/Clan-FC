package com.tienda.dto;

import com.tienda.model.Producto;

import java.math.BigDecimal;

/**
 * Respuesta de producto hacia el frontend.
 * Incluye categoriaNombre y el flag stockBajo para los badges de la vista
 * cliente y las alertas del panel admin, sin exponer entidades JPA.
 */
public record ProductoResponse(
        Long idProducto,
        String sku,
        String nombre,
        String descripcion,
        BigDecimal precioBase,
        Integer stock,
        Integer stockMinimo,
        Boolean stockBajo,
        Long categoriaId,
        String categoriaNombre,
        String imagenUrl,
        Boolean activo
) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(
                p.getIdProducto(),
                p.getSku(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecioBase(),
                p.getStock(),
                p.getStockMinimo(),
                p.getStock() <= p.getStockMinimo(),
                p.getCategoria().getIdCategoria(),
                p.getCategoria().getNombre(),
                p.getImagenUrl(),
                p.getActivo()
        );
    }
}
