package com.tienda.controller;

import com.tienda.dto.CheckoutRequest;
import com.tienda.dto.OrdenResponse;
import com.tienda.model.CanalVenta;
import com.tienda.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * PUNTO DE VENTA / CAJA (rol CAJERO, también ADMIN).
 * POST /api/pos/cobros — cobra una venta en mostrador con canal CAJA:
 * selecciona cliente/empresa, calcula impuesto dinámico y descuenta stock.
 */
@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosController {

    private final CheckoutService checkoutService;

    @PostMapping("/cobros")
    public ResponseEntity<OrdenResponse> cobrar(@Valid @RequestBody CheckoutRequest request,
                                                Authentication authentication) {
        OrdenResponse orden = checkoutService.procesar(
                request, CanalVenta.CAJA, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(orden);
    }
}
