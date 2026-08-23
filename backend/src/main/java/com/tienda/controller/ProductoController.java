package com.tienda.controller;

import com.tienda.dto.ProductoRequest;
import com.tienda.dto.ProductoResponse;
import com.tienda.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST de productos.
 *
 * Permisos (definidos en SecurityConfig):
 *  - GET  /api/productos            -> público (catálogo CLIENTE/CAJERO/ADMIN)
 *  - POST /api/productos            -> ADMIN
 *  - PUT  /api/productos/{id}       -> ADMIN
 *  - DEL  /api/productos/{id}       -> ADMIN
 */
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    /**
     * Lista productos del catálogo.
     * ?busqueda=texto&categoriaId=N&incluirInactivos=true (solo admin ve inactivos).
     */
    @GetMapping
    public List<ProductoResponse> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {

        return incluirInactivos
                ? productoService.buscarInventario(busqueda, categoriaId)
                : productoService.buscarCatalogo(busqueda, categoriaId);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
