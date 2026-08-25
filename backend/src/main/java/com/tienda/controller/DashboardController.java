package com.tienda.controller;

import com.tienda.dto.*;
import com.tienda.service.DashboardService;
import com.tienda.service.ProductoSedeStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST del dashboard ejecutivo.
 * Endpoints para comparación de sedes, alertas y resumen financiero.
 *
 * Permisos (SecurityConfig):
 *  - /api/dashboard/** -> ADMIN
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final ProductoSedeStockService productoSedeStockService;

    /** Comparación de métricas entre sedes. */
    @GetMapping("/comparacion")
    public ComparacionSedesResponse compararSedes() {
        return dashboardService.compararSedes();
    }

    /** Alertas del sistema (stock bajo, incidencias pendientes). */
    @GetMapping("/alertas")
    public AlertasStockResponse alertas() {
        return dashboardService.generarAlertas();
    }

    /** Resumen financiero consolidado. */
    @GetMapping("/finanzas")
    public ResumenFinancieroResponse finanzas() {
        return dashboardService.resumenFinanciero();
    }

    /** Resumen de inventario por sede. */
    @GetMapping("/inventario")
    public InventarioResumenResponse inventario() {
        return dashboardService.resumenInventario();
    }

    /** Stock de productos en una sede específica. */
    @GetMapping("/sede/{idSede}/stock")
    public List<ProductoSedeStock> stockPorSede(@PathVariable Long idSede) {
        return productoSedeStockService.obtenerStockPorSede(idSede);
    }
}
