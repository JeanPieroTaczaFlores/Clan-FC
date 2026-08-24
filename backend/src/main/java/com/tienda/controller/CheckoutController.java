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
 * CHECKOUT WEB DEL CLIENTE (rol CLIENTE, también ADMIN).
 * POST /api/checkout — confirma el carrito: valida/reserva stock, registra
 * la orden con IVA parametrizado por régimen fiscal y descuenta inventario,
 * todo en una transacción.
 */
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<OrdenResponse> confirmar(@Valid @RequestBody CheckoutRequest request,
                                                   Authentication authentication) {
        OrdenResponse orden = checkoutService.procesar(
                request, CanalVenta.WEB, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(orden);
    }
}
