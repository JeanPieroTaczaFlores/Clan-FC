package com.tienda.dto;

import com.tienda.model.EmpresaCliente;
import com.tienda.model.Pais;

import java.math.BigDecimal;

/** Respuesta de empresa cliente para selectores con bandera por país. */
public record EmpresaClienteResponse(
        Long idEmpresa,
        String razonSocial,
        String rfc,
        String regimenFiscal,
        BigDecimal tasaIva,
        Long idPais,
        String paisCodigo,
        String paisNombre,
        String banderaEmoji
) {
    public static EmpresaClienteResponse from(EmpresaCliente e) {
        Pais p = e.getPais();
        return new EmpresaClienteResponse(
                e.getIdEmpresa(),
                e.getRazonSocial(),
                e.getRfc(),
                e.getRegimenFiscal().name(),
                e.getTasaIva(),
                (p != null) ? p.getIdPais() : null,
                (p != null) ? p.getCodigoIso2() : null,
                (p != null) ? p.getNombre() : null,
                Pais.banderaDesde((p != null) ? p.getCodigoIso2() : null)
        );
    }
}
