package com.tienda.controller;

import com.tienda.dto.CategoriaRequest;
import com.tienda.dto.CategoriaResponse;
import com.tienda.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST de categorías.
 * GET público; POST/PUT/DELETE solo ADMIN (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    /** ?todas=true devuelve también inactivas (uso del panel admin). */
    @GetMapping
    public List<CategoriaResponse> listar(@RequestParam(defaultValue = "false") boolean todas) {
        return todas ? categoriaService.listarTodas() : categoriaService.listarActivas();
    }

    @GetMapping("/{id}")
    public CategoriaResponse obtener(@PathVariable Long id) {
        return categoriaService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
    }

    @PutMapping("/{id}")
    public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
