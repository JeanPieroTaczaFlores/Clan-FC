package com.tienda.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global de errores REST: convierte excepciones en respuestas
 * JSON consistentes ({ timestamp, status, error, detalle }) para que el
 * frontend pueda mostrarlas sin conocer la implementación interna.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404 — recurso inexistente. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    /** 409 — violaciones de integridad (SKU duplicado, FK inválida, etc.). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo(HttpStatus.CONFLICT, ex.getMessage()));
    }

    /** 400 — errores de validación de DTOs (@Valid): campo -> mensaje. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));

        Map<String, Object> body = cuerpo(HttpStatus.BAD_REQUEST, "Error de validación");
        body.put("campos", errores);
        return ResponseEntity.badRequest().body(body);
    }

    private Map<String, Object> cuerpo(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", mensaje);
        return body;
    }
}
