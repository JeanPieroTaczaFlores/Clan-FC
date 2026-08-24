package com.tienda.service;

import com.tienda.dto.ProductoResponse;
import com.tienda.dto.ReporteSemanalResponse;
import com.tienda.model.CanalVenta;
import com.tienda.model.DetalleOrden;
import com.tienda.model.Orden;
import com.tienda.repository.OrdenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;

/**
 * REPORTE SEMANAL para el cajero: ventas de los últimos 7 días por día,
 * top de productos vendidos y lista de reposición (stock bajo).
 * Los datos salen de las órdenes CAJA + WEB reales del período.
 */
@Service
@RequiredArgsConstructor
public class ReporteSemanalService {

    private final OrdenRepository ordenRepository;
    private final ProductoService productoService;

    @Transactional(readOnly = true)
    public ReporteSemanalResponse generar() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.minusDays(6); // ventana de 7 días incl. hoy

        // Órdenes PAGADAS dentro de la ventana.
        List<Orden> ordenes = ordenRepository.findAllByOrderByFechaCreacionDesc().stream()
                .filter(o -> o.getEstado() == com.tienda.model.EstadoOrden.PAGADA)
                .filter(o -> {
                    LocalDate f = o.getFechaCreacion().toLocalDate();
                    return !f.isBefore(inicio) && !f.isAfter(hoy);
                })
                .toList();

        BigDecimal totalVentas = ordenes.stream()
                .map(Orden::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // --- Ventas por día (siempre 7 días, con ceros) ---
        List<ReporteSemanalResponse.DiaVenta> dias = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            List<Orden> delDia = ordenes.stream()
                    .filter(o -> o.getFechaCreacion().toLocalDate().equals(dia))
                    .toList();

            dias.add(new ReporteSemanalResponse.DiaVenta(
                    dia.toString(),
                    dia.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es", "MX")),
                    delDia.stream().map(Orden::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                    delDia.size()));
        }

        // --- Top productos por unidades vendidas ---
        Map<String, BigDecimal> importes = new HashMap<>();
        Map<String, Object[]> acumulado = new LinkedHashMap<>(); // sku -> [nombre, unidades]
        for (Orden o : ordenes) {
            for (DetalleOrden d : o.getDetalles()) {
                Object[] acc = acumulado.computeIfAbsent(d.getSku(),
                        k -> new Object[]{d.getNombreProducto(), 0});
                acc[1] = (Integer) acc[1] + d.getCantidad();
                importes.merge(d.getSku(),
                        d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())),
                        BigDecimal::add);
            }
        }

        List<ReporteSemanalResponse.ProductoVendido> top = acumulado.entrySet().stream()
                .sorted((a, b) -> Integer.compare((Integer) b.getValue()[1], (Integer) a.getValue()[1]))
                .limit(5)
                .map(e -> new ReporteSemanalResponse.ProductoVendido(
                        e.getKey(),
                        (String) e.getValue()[0],
                        (Integer) e.getValue()[1],
                        importes.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                .toList();

        int numeroOrdenes = ordenes.size();
        BigDecimal ticketPromedio = numeroOrdenes > 0
                ? totalVentas.divide(BigDecimal.valueOf(numeroOrdenes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new ReporteSemanalResponse(
                totalVentas.setScale(2, RoundingMode.HALF_UP),
                numeroOrdenes,
                ticketPromedio,
                dias,
                top,
                productoService.productosConStockBajo() // reposición sugerida
        );
    }
}
