package com.tienda.dto;

import com.tienda.model.CajaMovimiento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Respuesta de movimiento de caja.
 */
public record CajaMovimientoResponse(
        Long idMovimiento,
        Long idCaja,
        Integer numeroCaja,
        String sedeNombre,
        String tipo,
        BigDecimal monto,
        BigDecimal saldoDespues,
        String referencia,
        String usuarioUsername,
        OffsetDateTime fecha
) {
    public static CajaMovimientoResponse from(CajaMovimiento m) {
        return new CajaMovimientoResponse(
                m.getIdMovimiento(),
                m.getCaja().getIdCaja(),
                m.getCaja().getNumeroCaja(),
                m.getCaja().getSede().getNombre(),
                m.getTipo(),
                m.getMonto(),
                m.getSaldoDespues(),
                m.getReferencia(),
                m.getUsuario().getUsername(),
                m.getFecha()
        );
    }
}
