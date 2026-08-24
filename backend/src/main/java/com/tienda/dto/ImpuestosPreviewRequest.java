package com.tienda.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Petición para previsualizar impuestos en vivo (calculadora del frontend). */
public record ImpuestosPreviewRequest(
        @NotNull @DecimalMin("0.00")
        BigDecimal subtotalBase,

        /** NULL = consumidor final con tasa general. */
        Long empresaClienteId
) {
}
