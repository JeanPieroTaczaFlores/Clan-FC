package com.tienda.dto;

import com.tienda.model.Orden;
import com.tienda.model.DetalleOrden;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Respuesta completa de una orden (ticket de POS / confirmación de pedido /
 * futuro comprobante PDF del Entregable 3).
 */
public record OrdenResponse(
        Long idOrden,
        String folio,
        String canal,
        String estado,
        String usuarioNombre,
        Long empresaId,
        String empresaNombre,
        String paisNombre,
        String banderaEmoji,
        String regimenFiscal,
        BigDecimal tasaIva,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total,
        String metodoPago,
        OffsetDateTime fechaCreacion,
        List<OrdenItemResponse> items
) {
    public static OrdenResponse from(Orden o) {
        List<OrdenItemResponse> items = o.getDetalles().stream()
                .map(OrdenResponse::mapItem)
                .toList();

        com.tienda.model.Pais pais = o.getPais();

        return new OrdenResponse(
                o.getIdOrden(),
                o.getFolio(),
                o.getCanal().name(),
                o.getEstado().name(),
                o.getUsuario().getNombreCompleto(),
                o.getEmpresaCliente() != null ? o.getEmpresaCliente().getIdEmpresa() : null,
                o.getEmpresaCliente() != null ? o.getEmpresaCliente().getRazonSocial() : "Consumidor final",
                pais != null ? pais.getNombre() : null,
                com.tienda.model.Pais.banderaDesde(pais != null ? pais.getCodigoIso2() : null),
                o.getRegimenFiscal().name(),
                o.getTasaIva(),
                o.getSubtotal(),
                o.getIva(),
                o.getTotal(),
                o.getMetodoPago(),
                o.getFechaCreacion(),
                items
        );
    }

    private static OrdenItemResponse mapItem(DetalleOrden d) {
        return new OrdenItemResponse(
                d.getIdDetalle(),
                d.getSku(),
                d.getNombreProducto(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getIvaLinea(),
                d.getSubtotalLinea()
        );
    }
}
