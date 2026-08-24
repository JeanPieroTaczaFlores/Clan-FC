package com.tienda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Cuerpo del checkout (web o POS).
 * El backend SIEMPRE recalcula precios/IVA desde la BD: el frontend nunca
 * manda montos, solo referencias y cantidades.
 */
public record CheckoutRequest(
        @Valid @NotEmpty(message = "El carrito está vacío")
        List<ItemCheckoutRequest> items,

        /**
         * Empresa cliente que compra (B2B) — define el régimen fiscal y por
         * tanto la tasa de IVA. NULL = consumidor final (tasa general).
         */
        Long empresaClienteId,

        @NotBlank
        String metodoPago
) {
        /** Línea del carrito: referencia al producto + cantidad deseada. */
        public record ItemCheckoutRequest(
                Long productoId,
                @Valid Integer cantidad
        ) {
        }
}
