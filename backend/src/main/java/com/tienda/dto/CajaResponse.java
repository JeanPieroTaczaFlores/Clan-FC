package com.tienda.dto;

import com.tienda.model.Caja;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Respuesta de caja hacia el frontend.
 */
public record CajaResponse(
        Long idCaja,
        Long idSede,
        String sedeNombre,
        Integer numeroCaja,
        BigDecimal efectivo,
        String estado,
        Long idUsuario,
        String usuarioUsername,
        OffsetDateTime fechaApertura,
        OffsetDateTime fechaCierre
) {
    public static CajaResponse from(Caja c) {
        return new CajaResponse(
                c.getIdCaja(),
                c.getSede().getIdSede(),
                c.getSede().getNombre(),
                c.getNumeroCaja(),
                c.getEfectivo(),
                c.getEstado(),
                c.getUsuario() != null ? c.getUsuario().getIdUsuario() : null,
                c.getUsuario() != null ? c.getUsuario().getUsername() : null,
                c.getFechaApertura(),
                c.getFechaCierre()
        );
    }
}
