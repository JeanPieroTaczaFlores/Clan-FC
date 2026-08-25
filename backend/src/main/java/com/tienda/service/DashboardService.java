package com.tienda.service;

import com.tienda.dto.*;
import com.tienda.repository.*;import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard ejecutivo: comparación de sedes, alertas y resumen financiero.
 * Agrega datos de múltiples fuentes para las vistas del admin.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SedeRepository sedeRepository;
    private final CajaRepository cajaRepository;
    private final CajaMovimientoRepository cajaMovimientoRepository;
    private final OrdenRepository ordenRepository;
    private final ProductoSedeStockRepository productoSedeStockRepository;
    private final ProductoRepository productoRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final CategoriaRepository categoriaRepository;

    /**
     * Comparación de sedes: ventas, caja, ticket promedio, incidencias por sede.
     */
    @Transactional(readOnly = true)
    public ComparacionSedesResponse compararSedes() {
        List<ComparacionSedesResponse.SedeResumen> resumenes = new ArrayList<>();

        sedeRepository.findAllByActivaTrueOrderByNombreAsc().forEach(sede -> {
            Long idSede = sede.getIdSede();

            // Ventas de esta sede (órdenes CAJA de usuarios asignados a la sede)
            BigDecimal ventas = cajaRepository.sumarEfectivoPorSede(idSede);

            // Contar movimientos de tipo VENTA en la sede
            long ordenes = cajaMovimientoRepository.findAllBySedeIdSedeOrderByFechaDesc(idSede)
                    .stream()
                    .filter(m -> "VENTA".equals(m.getTipo()))
                    .count();

            BigDecimal ticketPromedio = ordenes > 0
                    ? ventas.divide(BigDecimal.valueOf(ordenes), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            int stockBajo = (int) productoSedeStockRepository.contarStockBajoPorSede(idSede);
            int productos = productoSedeStockRepository.findBySedeIdSede(idSede).size();

            resumenes.add(new ComparacionSedesResponse.SedeResumen(
                    idSede, sede.getNombre(), ventas, (int) ordenes,
                    ticketPromedio, BigDecimal.ZERO, productos, stockBajo, 0
            ));
        });

        return new ComparacionSedesResponse(resumenes);
    }

    /**
     * Alertas del sistema: stock bajo, incidencias pendientes.
     */
    @Transactional(readOnly = true)
    public AlertasStockResponse generarAlertas() {
        List<AlertasStockResponse.Alerta> alertas = new ArrayList<>();
        int criticas = 0, advertencias = 0, info = 0;

        // Alertas de stock bajo por sede
        sedeRepository.findAllByActivaTrueOrderByNombreAsc().forEach(sede -> {
            productoSedeStockRepository.findBySedeIdSedeAndStockLessThanEqualStockMinimo(sede.getIdSede())
                    .forEach(pss -> {
                        String severidad = pss.getStock() == 0 ? "CRITICA" : "ALERTA";
                        alertas.add(new AlertasStockResponse.Alerta(
                                severidad, "STOCK_Bajo",
                                "Stock bajo: " + pss.getProducto().getNombre(),
                                "Sede " + sede.getNombre() + " — Stock: " + pss.getStock() + " (mínimo: " + pss.getStockMinimo() + ")",
                                pss.getProducto().getIdProducto()
                        ));
                    });
        });

        // Incidencias pendientes
        incidenciaRepository.findAllByOrderByFechaReporteDesc().forEach(inc -> {
            if ("REPORTADA".equals(inc.getEstado()) || "EN_REVISION".equals(inc.getEstado())) {
                alertas.add(new AlertasStockResponse.Alerta(
                        "INFO", "INCIDENCIA",
                        "Incidencia " + inc.getTipo() + ": " + inc.getProducto().getNombre(),
                        "Estado: " + inc.getEstado() + " — " + inc.getDescripcion(),
                        inc.getIdIncidencia()
                ));
            }
        });

        // Contar severidades
        for (AlertasStockResponse.Alerta a : alertas) {
            switch (a.severidad()) {
                case "CRITICA" -> criticas++;
                case "ALERTA" -> advertencias++;
                default -> info++;
            }
        }

        return new AlertasStockResponse(alertas, criticas, advertencias, info);
    }

    /**
     * Resumen financiero consolidado de todas las sedes.
     */
    @Transactional(readOnly = true)
    public ResumenFinancieroResponse resumenFinanciero() {
        List<ResumenFinancieroResponse.SedeFinanciera> porSede = new ArrayList<>();
        BigDecimal ingresosTotales = BigDecimal.ZERO;
        BigDecimal ivaTotal = BigDecimal.ZERO;
        BigDecimal efectivoTotal = BigDecimal.ZERO;
        int totalOrdenes = 0;

        sedeRepository.findAllByActivaTrueOrderByNombreAsc().forEach(sede -> {
            Long idSede = sede.getIdSede();

            // Ventas de la sede
            BigDecimal ventasSede = cajaRepository.sumarEfectivoPorSede(idSede);
            BigDecimal efectivoSede = cajaRepository.sumarEfectivoPorSede(idSede);

            // IVA estimado (18% Peru)
            BigDecimal ivaSede = ventasSede.multiply(new BigDecimal("0.18"))
                    .setScale(2, RoundingMode.HALF_UP);

            porSede.add(new ResumenFinancieroResponse.SedeFinanciera(
                    idSede, sede.getNombre(), ventasSede, ivaSede, efectivoSede, 0, 0
            ));
        });

        int totalProductos = (int) productoRepository.count();
        int totalIncidencias = incidenciaRepository.findAllByOrderByFechaReporteDesc().size();

        return new ResumenFinancieroResponse(
                ingresosTotales, ivaTotal, efectivoTotal,
                porSede, totalOrdenes, totalProductos, totalIncidencias
        );
    }

    /**
     * Resumen de inventario por sede.
     */
    @Transactional(readOnly = true)
    public InventarioResumenResponse resumenInventario() {
        List<InventarioResumenResponse.StockSede> porSede = new ArrayList<>();

        sedeRepository.findAllByActivaTrueOrderByNombreAsc().forEach(sede -> {
            Long idSede = sede.getIdSede();
            BigDecimal valor = productoSedeStockRepository.valorInventarioPorSede(idSede);
            long stockBajo = productoSedeStockRepository.contarStockBajoPorSede(idSede);
            int productos = productoSedeStockRepository.findBySedeIdSede(idSede).size();

            porSede.add(new InventarioResumenResponse.StockSede(
                    idSede, sede.getNombre(), valor, (int) stockBajo, productos
            ));
        });

        return new InventarioResumenResponse(
                (int) productoRepository.count(),
                productoRepository.valorInventario(),
                (int) productoRepository.buscarConStockBajo().size(),
                (int) categoriaRepository.count(),
                porSede
        );
    }
}
