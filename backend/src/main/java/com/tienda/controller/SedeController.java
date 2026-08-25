package com.tienda.controller;

import com.tienda.dto.SedeRequest;
import com.tienda.dto.SedeResponse;
import com.tienda.service.SedeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST de sedes (sucursales).
 *
 * Permisos (SecurityConfig):
 *  - GET  /api/sedes            -> autenticados
 *  - POST/PUT/DELETE /api/sedes -> ADMIN
 */
@RestController
@RequestMapping("/api/sedes")
@RequiredArgsConstructor
public class SedeController {

    private final SedeService sedeService;

    @GetMapping
    public List<SedeResponse> listar() {
        return sedeService.listar();
    }

    @GetMapping("/todas")
    public List<SedeResponse> listarTodas() {
        return sedeService.listarTodas();
    }

    @GetMapping("/{id}")
    public SedeResponse obtener(@PathVariable Long id) {
        return sedeService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<SedeResponse> crear(@Valid @RequestBody SedeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sedeService.crear(request));
    }

    @PutMapping("/{id}")
    public SedeResponse actualizar(@PathVariable Long id, @Valid @RequestBody SedeRequest request) {
        return sedeService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        sedeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
