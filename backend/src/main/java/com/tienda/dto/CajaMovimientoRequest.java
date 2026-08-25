package com.tienda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Petición para registrar un movimiento de caja.
 */
public record CajaMovimientoRequest(
        @NotNull(message = "La caja es obligatoria")
        Long idCaja,

        @NotNull(message = "El tipo es obligatorio")
        String tipo, // FONDOS_INICIALES, VENTA, RETIRO, AJUSTE

        @NotNull @Positive(message = "El monto debe ser positivo")
        BigDecimal monto,

        String referencia
) {
}
