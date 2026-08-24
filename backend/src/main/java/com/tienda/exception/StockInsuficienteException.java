package com.tienda.exception;

/** Se lanza cuando no hay inventario suficiente para cubrir una venta. */
public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
