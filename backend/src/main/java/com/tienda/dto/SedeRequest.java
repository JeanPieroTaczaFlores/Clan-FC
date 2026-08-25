package com.tienda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición de alta/edición de sede (ADMIN).
 */
public record SedeRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80)
        String nombre,

        @Size(max = 120)
        String direccion,

        @Size(max = 20)
        String telefono,

        Boolean activa
) {
}
