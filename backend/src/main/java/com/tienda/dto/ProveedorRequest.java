package com.tienda.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta de proveedor desde el panel ADMIN. */
public record ProveedorRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 120) String contactoNombre,
        @Size(max = 30) String telefono,
        @Email @Size(max = 100) String email
) {
}