package com.tienda.dto;

import com.tienda.model.Categoria;

/** Respuesta de categoría hacia el frontend. */
public record CategoriaResponse(
        Long idCategoria,
        String nombre,
        String descripcion,
        Boolean activa
) {
    public static CategoriaResponse from(Categoria c) {
        return new CategoriaResponse(
                c.getIdCategoria(),
                c.getNombre(),
                c.getDescripcion(),
                c.getActiva()
        );
    }
}
