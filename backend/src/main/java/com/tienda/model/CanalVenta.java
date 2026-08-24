package com.tienda.model;

/**
 * Canal por el que se realizó la venta:
 * WEB   -> checkout del cliente en la tienda online.
 * CAJA  -> cobro directo en punto de venta (POS) por el cajero.
 */
public enum CanalVenta {
    WEB,
    CAJA
}
