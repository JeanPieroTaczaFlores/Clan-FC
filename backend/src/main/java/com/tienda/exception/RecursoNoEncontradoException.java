package com.tienda.exception;

/** Se lanza cuando un recurso (producto, categoría, orden...) no existe. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
