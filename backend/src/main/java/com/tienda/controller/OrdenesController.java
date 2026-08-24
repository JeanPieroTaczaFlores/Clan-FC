package com.tienda.controller;

import com.tienda.dto.OrdenResponse;
import com.tienda.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consulta de órdenes según el rol autenticado:
 *  - ADMIN:   todas las ventas
 *  - CAJERO:  cobros realizados en caja (canal CAJA)
 *  - CLIENTE: únicamente sus propias compras web
 */
@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
public class OrdenesController {

    private final CheckoutService checkoutService;

    @GetMapping
    public List<OrdenResponse> listar(Authentication authentication) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean esCajero = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAJERO"));

        return checkoutService.listarSegunRol(authentication.getName(), esAdmin, esCajero);
    }

    /** Detalle de una orden (ticket / futuro PDF). Acceso: autenticado. */
    @GetMapping("/{id}")
    public OrdenResponse obtener(@PathVariable Long id) {
        return checkoutService.obtenerPorId(id);
    }
}
