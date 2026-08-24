package com.tienda.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de registro PÚBLICO (autoregistro de clientes).
 * El rol asignado siempre es CLIENTE — los roles internos (CAJERO/ADMIN)
 * solo puede crearlos un ADMIN vía /api/usuarios.
 */
public record RegistroUsuarioRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 6, max = 100) String password, // se hashea con BCrypt
        @NotBlank @Size(min = 3, max = 120) String nombreCompleto,
        @NotNull Long idPais                                 // país fiscal: define su IVA
) {
}
