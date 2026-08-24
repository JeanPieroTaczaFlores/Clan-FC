package com.tienda.controller;

import com.tienda.dto.ProveedorRequest;
import com.tienda.model.Proveedor;
import com.tienda.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Proveedores: lectura para POS/admin; alta solo ADMIN. */
@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedoresController {

    private final ProveedorService proveedorService;

    @GetMapping
    public List<Proveedor> listar() {
        return proveedorService.listarActivos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Proveedor crear(@Valid @RequestBody ProveedorRequest request) {
        return proveedorService.crear(request);
    }
}
