package com.tienda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Petición de alta/edición de caja (ADMIN).
 */
public record CajaRequest(
        @NotNull(message = "La sede es obligatoria")
        Long idSede,

        Integer numeroCaja,

        BigDecimal efectivo,

        String estado,

        Long idUsuario
) {
}
