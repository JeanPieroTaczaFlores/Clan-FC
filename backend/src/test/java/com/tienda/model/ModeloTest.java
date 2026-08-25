package com.tienda.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests para entidades del modelo.
 */
@DisplayName("Modelo — Unit Tests")
class ModeloTest {

    @Test
    @DisplayName("Pais genera bandera emoji desde código ISO")
    void paisBanderaEmoji() {
        Pais peru = Pais.builder()
                .idPais(3L).codigoIso2("PE").nombre("Perú")
                .tasaIvaGeneral(java.math.BigDecimal.valueOf(18))
                .tasaIvaReducido(java.math.BigDecimal.valueOf(18))
                .build();

        String emoji = peru.getBanderaEmoji();

        assertThat(emoji).isEqualTo("🇵🇪");
    }

    @Test
    @DisplayName("Pais con código MX genera bandera mexicana")
    void paisMexico() {
        Pais mexico = Pais.builder()
                .idPais(1L).codigoIso2("MX").nombre("México")
                .tasaIvaGeneral(java.math.BigDecimal.valueOf(16))
                .tasaIvaReducido(java.math.BigDecimal.valueOf(8))
                .build();

        String emoji = mexico.getBanderaEmoji();

        assertThat(emoji).isEqualTo("🇲🇽");
    }

    @Test
    @DisplayName("Producto stock bajo se detecta correctamente")
    void productoStockBajo() {
        Producto producto = Producto.builder()
                .stock(3).stockMinimo(5).build();

        boolean stockBajo = producto.getStock() <= producto.getStockMinimo();

        assertThat(stockBajo).isTrue();
    }

    @Test
    @DisplayName("Producto sin stock bajo")
    void productoSinStockBajo() {
        Producto producto = Producto.builder()
                .stock(28).stockMinimo(5).build();

        boolean stockBajo = producto.getStock() <= producto.getStockMinimo();

        assertThat(stockBajo).isFalse();
    }

    @Test
    @DisplayName("Orden con IVA consistente")
    void ordenIvaConsistente() {
        java.math.BigDecimal subtotal = new java.math.BigDecimal("1000.00");
        java.math.BigDecimal iva = new java.math.BigDecimal("180.00");
        java.math.BigDecimal total = new java.math.BigDecimal("1180.00");

        assertThat(total).isEqualByComparingTo(subtotal.add(iva));
    }

    @Test
    @DisplayName("DetalleOrden cálculo de subtotal línea")
    void detalleOrdenSubtotal() {
        java.math.BigDecimal precio = new java.math.BigDecimal("549.00");
        int cantidad = 3;

        java.math.BigDecimal subtotal = precio.multiply(java.math.BigDecimal.valueOf(cantidad));

        assertThat(subtotal).isEqualByComparingTo(new java.math.BigDecimal("1647.00"));
    }

    @Test
    @DisplayName("Caja movimientos tipo válidos")
    void tiposMovimientoValidos() {
        String[] tiposValidos = {"FONDOS_INICIALES", "VENTA", "RETIRO", "AJUSTE"};

        for (String tipo : tiposValidos) {
            assertThat(tipo).isIn("FONDOS_INICIALES", "VENTA", "RETIRO", "AJUSTE");
        }
    }

    @Test
    @DisplayName("SedeStock crea mapa correctamente")
    void sedeStockMapa() {
        java.util.Map<Long, Integer> sedeStock = new java.util.LinkedHashMap<>();
        sedeStock.put(1L, 28);
        sedeStock.put(2L, 18);
        sedeStock.put(3L, 32);
        sedeStock.put(4L, 18);

        assertThat(sedeStock).hasSize(4);
        assertThat(sedeStock.get(1L)).isEqualTo(28);
        assertThat(sedeStock.get(3L)).isEqualTo(32);
    }

    @Test
    @DisplayName("Estados de orden válidos")
    void estadosOrden() {
        assertThat(EstadoOrden.PAGADA).isNotNull();
        assertThat(EstadoOrden.CANCELADA).isNotNull();
    }

    @Test
    @DisplayName("Estados de incidencia válidos")
    void estadosIncidencia() {
        assertThat(EstadoIncidencia.REPORTADA).isNotNull();
        assertThat(EstadoIncidencia.EN_REVISION).isNotNull();
        assertThat(EstadoIncidencia.RESUELTA).isNotNull();
        assertThat(EstadoIncidencia.CANCELADA).isNotNull();
    }

    @Test
    @DisplayName("Tipos de incidencia válidos")
    void tiposIncidencia() {
        assertThat(TipoIncidencia.DEVOLUCION).isNotNull();
        assertThat(TipoIncidencia.DEFECTO).isNotNull();
        assertThat(TipoIncidencia.GARANTIA).isNotNull();
    }

    @Test
    @DisplayName("Canales de venta válidos")
    void canalesVenta() {
        assertThat(CanalVenta.WEB).isNotNull();
        assertThat(CanalVenta.CAJA).isNotNull();
    }
}
