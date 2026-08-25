package com.tienda.dto;

import com.tienda.model.Sede;

import java.math.BigDecimal;

/**
 * Respuesta de sede hacia el frontend.
 */
public record SedeResponse(
        Long idSede,
        String nombre,
        String direccion,
        String telefono,
        Boolean activa,
        BigDecimal efectivoTotal,
        Integer totalProductos,
        Integer totalOrdenes,
        Integer incidencias
) {
    public static SedeResponse from(Sede s) {
        return new SedeResponse(
                s.getIdSede(),
                s.getNombre(),
                s.getDireccion(),
                s.getTelefono(),
                s.getActiva(),
                BigDecimal.ZERO,
                0,
                0,
                0
        );
    }

    public static SedeResponse from(Sede s, BigDecimal efectivo, int productos, int ordenes, int incidencias) {
        return new SedeResponse(
                s.getIdSede(),
                s.getNombre(),
                s.getDireccion(),
                s.getTelefono(),
                s.getActiva(),
                efectivo,
                productos,
                ordenes,
                incidencias
        );
    }
}
