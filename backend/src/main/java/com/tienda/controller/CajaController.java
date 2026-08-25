package com.tienda.controller;

import com.tienda.dto.CajaMovimientoRequest;
import com.tienda.dto.CajaMovimientoResponse;
import com.tienda.dto.CajaRequest;
import com.tienda.dto.CajaResponse;
import com.tienda.service.CajaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * API REST de cajas y movimientos de caja.
 *
 * Permisos (SecurityConfig):
 *  - GET  /api/cajas/**             -> autenticados
 *  - POST /api/cajas/**             -> CAJERO/ADMIN
 *  - POST /api/cajas/movimientos    -> CAJERO/ADMIN
 */
@RestController
@RequestMapping("/api/cajas")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService cajaService;

    @GetMapping("/sede/{idSede}")
    public List<CajaResponse> listarPorSede(@PathVariable Long idSede) {
        return cajaService.listarPorSede(idSede);
    }

    @GetMapping("/{id}")
    public CajaResponse obtener(@PathVariable Long id) {
        return cajaService.obtenerPorId(id);
    }

    @GetMapping("/abierta/sede/{idSede}")
    public CajaResponse obtenerCajaAbierta(@PathVariable Long idSede) {
        return cajaService.obtenerCajaAbierta(idSede);
    }

    @PostMapping
    public ResponseEntity<CajaResponse> crear(@Valid @RequestBody CajaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.crear(request));
    }

    @PostMapping("/{id}/habilitar")
    public CajaResponse habilitar(
            @PathVariable Long id,
            @RequestParam Long idUsuario,
            @RequestParam BigDecimal fondosIniciales) {
        return cajaService.habilitar(id, idUsuario, fondosIniciales);
    }

    @PostMapping("/{id}/cerrar")
    public CajaResponse cerrar(@PathVariable Long id) {
        return cajaService.cerrar(id);
    }

    @GetMapping("/{idCaja}/movimientos")
    public List<CajaMovimientoResponse> listarMovimientos(@PathVariable Long idCaja) {
        return cajaService.listarMovimientosPorCaja(idCaja);
    }

    @GetMapping("/movimientos/sede/{idSede}")
    public List<CajaMovimientoResponse> listarMovimientosPorSede(@PathVariable Long idSede) {
        return cajaService.listarMovimientosPorSede(idSede);
    }

    @GetMapping("/movimientos")
    public List<CajaMovimientoResponse> listarTodosMovimientos() {
        return cajaService.listarTodosMovimientos();
    }

    @PostMapping("/movimientos")
    public ResponseEntity<CajaMovimientoResponse> registrarMovimiento(
            @Valid @RequestBody CajaMovimientoRequest request,
            Authentication auth) {
        // En un sistema real, el ID del usuario vendría del token JWT.
        // Por ahora usamos el username para buscar el usuario.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cajaService.registrarMovimiento(request, 1L)); // TODO: resolver ID del auth
    }
}
