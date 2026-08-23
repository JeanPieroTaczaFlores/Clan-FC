package com.tienda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Petición de alta/edición de categoría (ADMIN). */
public record CategoriaRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 80)
        String nombre,

        @Size(max = 255)
        String descripcion,

        Boolean activa
) {
}
