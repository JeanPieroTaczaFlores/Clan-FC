package com.tienda.model;

/**
 * Tipo de movimiento de almacén (kardex):
 *  ENTRADA      compra/recepción de mercancía del proveedor (+stock)
 *  SALIDA_VENTA venta registrada automáticamente por checkout (-stock)
 *  DEVOLUCION   el cliente devuelve y el producto vuelve al stock (+)
 *  MERMA        producto dañado/inservible que sale del stock (-)
 *  AJUSTE       corrección manual de inventario (±, según nota)
 */
public enum TipoMovimiento {
    ENTRADA, SALIDA_VENTA, DEVOLUCION, MERMA, AJUSTE
}
