package com.tienda.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests para utilidades del dashboard ejecutivo.
 * Simula cálculos de KPIs, alertas y comparación de sedes.
 */
@DisplayName("DashboardUtils — Unit Tests")
class DashboardUtilsTest {

    @Test
    @DisplayName("Calcular KPI: ventas totales")
    void ventasTotales() {
        List<BigDecimal> ventas = List.of(
                new BigDecimal("5000.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("7500.00"),
                new BigDecimal("2500.00")
        );

        BigDecimal total = ventas.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(total).isEqualByComparingTo(new BigDecimal("18000.00"));
    }

    @Test
    @DisplayName("Calcular KPI: ticket promedio")
    void ticketPromedio() {
        BigDecimal totalVentas = new BigDecimal("18000.00");
        int totalOrdenes = 30;

        BigDecimal ticket = totalVentas.divide(BigDecimal.valueOf(totalOrdenes), 2, RoundingMode.HALF_UP);

        assertThat(ticket).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    @DisplayName("Calcular KPI: inventario total por sede")
    void inventarioPorSede() {
        Map<String, BigDecimal> inventario = new LinkedHashMap<>();
        inventario.put("Villa El Salvador", new BigDecimal("199965.00"));
        inventario.put("Chorrillos", new BigDecimal("196163.00"));
        inventario.put("San Juan de Lurigancho", new BigDecimal("295505.00"));
        inventario.put("Surco", new BigDecimal("208593.00"));

        BigDecimal totalInventario = inventario.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalInventario).isEqualByComparingTo(new BigDecimal("900226.00"));
    }

    @Test
    @DisplayName("Clasificar alertas por severidad")
    void clasificarAlertas() {
        List<String> alertas = List.of("CRITICA", "ALERTA", "INFO", "CRITICA", "ALERTA", "INFO", "INFO");

        long criticas = alertas.stream().filter(a -> a.equals("CRITICA")).count();
        long advertencias = alertas.stream().filter(a -> a.equals("ALERTA")).count();
        long info = alertas.stream().filter(a -> a.equals("INFO")).count();

        assertThat(criticas).isEqualTo(2);
        assertThat(advertencias).isEqualTo(2);
        assertThat(info).isEqualTo(3);
    }

    @Test
    @DisplayName("Top productos por unidades vendidas")
    void topProductos() {
        Map<String, Integer> ventas = new LinkedHashMap<>();
        ventas.put("AUD-001", 45);
        ventas.put("TEC-001", 32);
        ventas.put("CEL-001", 28);
        ventas.put("RAT-001", 55);
        ventas.put("MON-001", 12);

        List<Map.Entry<String, Integer>> top3 = ventas.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .toList();

        assertThat(top3.get(0).getKey()).isEqualTo("RAT-001");
        assertThat(top3.get(0).getValue()).isEqualTo(55);
        assertThat(top3.get(1).getKey()).isEqualTo("AUD-001");
        assertThat(top3.get(2).getKey()).isEqualTo("TEC-001");
    }

    @Test
    @DisplayName("Comparación de sedes: calcular diferencia porcentual")
    void diferenciaPorcentual() {
        BigDecimal ventasA = new BigDecimal("5000.00");
        BigDecimal ventasB = new BigDecimal("3000.00");

        BigDecimal diff = ventasA.subtract(ventasB)
                .divide(ventasA, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        assertThat(diff).isEqualByComparingTo(new BigDecimal("40.0000"));
    }

    @Test
    @DisplayName("Ventas por día de la semana (7 días)")
    void ventasPorDia() {
        List<String> dias = List.of("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom");
        List<BigDecimal> montos = List.of(
                new BigDecimal("2500"), new BigDecimal("3200"),
                new BigDecimal("1800"), new BigDecimal("4100"),
                new BigDecimal("3500"), new BigDecimal("5200"),
                new BigDecimal("2900")
        );

        assertThat(dias).hasSize(7);
        assertThat(montos).hasSize(7);

        BigDecimal totalSemana = montos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalSemana).isEqualByComparingTo(new BigDecimal("23200"));
    }

    @Test
    @DisplayName("IVA consolidado de todas las sedes")
    void ivaConsolidado() {
        List<BigDecimal> ivaPorSede = List.of(
                new BigDecimal("900.00"),
                new BigDecimal("540.00"),
                new BigDecimal("1350.00"),
                new BigDecimal("450.00")
        );

        BigDecimal ivaTotal = ivaPorSede.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(ivaTotal).isEqualByComparingTo(new BigDecimal("3240.00"));
    }

    @Test
    @DisplayName("Porcentaje de incidencias por sede")
    void porcentajeIncidencias() {
        int totalIncidencias = 11;
        int[] incidenciasPorSede = {3, 2, 3, 3};

        for (int inc : incidenciasPorSede) {
            BigDecimal porcentaje = BigDecimal.valueOf(inc)
                    .multiply(new BigDecimal("100"))
                    .divide(BigDecimal.valueOf(totalIncidencias), 1, RoundingMode.HALF_UP);
            assertThat(porcentaje).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Test
    @DisplayName("Sparkline: últimos 7 valores")
    void sparkline() {
        List<BigDecimal> historial = List.of(
                new BigDecimal("1000"), new BigDecimal("1200"),
                new BigDecimal("800"), new BigDecimal("1500"),
                new BigDecimal("1100"), new BigDecimal("1800"),
                new BigDecimal("2000")
        );

        BigDecimal min = historial.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal max = historial.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        assertThat(min).isEqualByComparingTo(new BigDecimal("800"));
        assertThat(max).isEqualByComparingTo(new BigDecimal("2000"));
    }
}
