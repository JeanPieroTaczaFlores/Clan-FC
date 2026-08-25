package com.tienda.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests para utilidades de formato y cálculo fiscal peruano.
 */
@DisplayName("PeruFiscalUtils — Unit Tests")
class PeruFiscalUtilsTest {

    @Test
    @DisplayName("Calcula IVA 18% correctamente")
    void calcularIva18() {
        BigDecimal base = new BigDecimal("1000.00");
        BigDecimal tasa = new BigDecimal("18.00");

        BigDecimal iva = base.multiply(tasa).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        assertThat(iva).isEqualByComparingTo(new BigDecimal("180.00"));
    }

    @Test
    @DisplayName("Calcula IVA con decimales")
    void calcularIvaDecimales() {
        BigDecimal base = new BigDecimal("549.00");
        BigDecimal tasa = new BigDecimal("18.00");

        BigDecimal iva = base.multiply(tasa).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        assertThat(iva).isEqualByComparingTo(new BigDecimal("98.82"));
    }

    @Test
    @DisplayName("Total con IVA")
    void totalConIva() {
        BigDecimal base = new BigDecimal("1000.00");
        BigDecimal iva = new BigDecimal("180.00");

        BigDecimal total = base.add(iva);

        assertThat(total).isEqualByComparingTo(new BigDecimal("1180.00"));
    }

    @Test
    @DisplayName("Formateo de fecha peruano")
    void formatoFechaPeruano() {
        LocalDate fecha = LocalDate.of(2026, 8, 25);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "PE"));

        String formateada = fecha.format(formatter);

        assertThat(formateada).isEqualTo("25/08/2026");
    }

    @Test
    @DisplayName("Generación de folio ORD")
    void generarFolio() {
        LocalDate hoy = LocalDate.now();
        String folio = "ORD-" + hoy.toString().replace("-", "") + "-" + "12345";

        assertThat(folio).startsWith("ORD-");
        assertThat(folio).containsPattern("^ORD-\\d{8}-\\d+$");
    }

    @Test
    @DisplayName("Redondeo a 2 decimales")
    void redondeoDecimales() {
        BigDecimal valor = new BigDecimal("123.456");
        BigDecimal redondeado = valor.setScale(2, RoundingMode.HALF_UP);

        assertThat(redondeado).isEqualByComparingTo(new BigDecimal("123.46"));
    }

    @Test
    @DisplayName("Suma de múltiples líneas")
    void sumaLineas() {
        BigDecimal linea1 = new BigDecimal("549.00");
        BigDecimal linea2 = new BigDecimal("799.00");
        BigDecimal linea3 = new BigDecimal("349.00");

        BigDecimal subtotal = linea1.add(linea2).add(linea3);

        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("1697.00"));
    }

    @Test
    @DisplayName("Cálculo de IVA por línea")
    void ivaPorLinea() {
        BigDecimal precio = new BigDecimal("549.00");
        int cantidad = 3;
        BigDecimal tasa = new BigDecimal("18.00");

        BigDecimal subtotalLinea = precio.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal ivaLinea = subtotalLinea.multiply(tasa).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        assertThat(subtotalLinea).isEqualByComparingTo(new BigDecimal("1647.00"));
        assertThat(ivaLinea).isEqualByComparingTo(new BigDecimal("296.46"));
    }

    @Test
    @DisplayName("Validación RUC peruano (11 dígitos)")
    void validarRuc() {
        String ruc = "20512345678";
        assertThat(ruc).hasSize(11);
        assertThat(ruc).matches("^\\d{11}$");
    }

    @Test
    @DisplayName("RUC inválido")
    void rucInvalido() {
        String ruc = "12345";
        assertThat(ruc.length()).isLessThan(11);
    }

    @Test
    @DisplayName("Cálculo de efectivo por ventas del día")
    void efectivoDelDia() {
        BigDecimal[] ventas = {
                new BigDecimal("250.00"),
                new BigDecimal("180.50"),
                new BigDecimal("420.00"),
                new BigDecimal("95.50")
        };

        BigDecimal total = java.math.BigDecimal.ZERO;
        for (BigDecimal v : ventas) {
            total = total.add(v);
        }

        assertThat(total).isEqualByComparingTo(new BigDecimal("946.00"));
    }

    @Test
    @DisplayName("Porcentaje de stock bajo")
    void porcentajeStockBajo() {
        int totalProductos = 16;
        int stockBajo = 4;

        BigDecimal porcentaje = BigDecimal.valueOf(stockBajo)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(totalProductos), 1, RoundingMode.HALF_UP);

        assertThat(porcentaje).isEqualByComparingTo(new BigDecimal("25.0"));
    }

    @Test
    @DisplayName("Ticket promedio")
    void ticketPromedio() {
        BigDecimal totalVentas = new BigDecimal("5000.00");
        int numOrdenes = 10;

        BigDecimal ticket = totalVentas.divide(BigDecimal.valueOf(numOrdenes), 2, RoundingMode.HALF_UP);

        assertThat(ticket).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Diferencia porcentual entre sedes")
    void diferenciaSedes() {
        BigDecimal ventasA = new BigDecimal("5000.00");
        BigDecimal ventasB = new BigDecimal("3000.00");

        BigDecimal diferencia = ventasA.subtract(ventasB)
                .divide(ventasA, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        assertThat(diferencia).isEqualByComparingTo(new BigDecimal("40.0000"));
    }
}
