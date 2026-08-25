package com.tienda.service;

import com.tienda.dto.*;
import com.tienda.model.*;
import com.tienda.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService — Unit Tests")
class DashboardServiceTest {

    @Mock private SedeRepository sedeRepository;
    @Mock private CajaRepository cajaRepository;
    @Mock private CajaMovimientoRepository cajaMovimientoRepository;
    @Mock private ProductoSedeStockRepository productoSedeStockRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private IncidenciaRepository incidenciaRepository;
    @Mock private CategoriaRepository categoriaRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Sede sede;

    @BeforeEach
    void setUp() {
        sede = Sede.builder().idSede(1L).nombre("Villa El Salvador").activa(true).build();
    }

    @Test
    @DisplayName("Comparación de sedes retorna lista de resúmenes")
    void compararSedes() {
        when(sedeRepository.findAllByActivaTrueOrderByNombreAsc()).thenReturn(List.of(sede));
        when(cajaRepository.sumarEfectivoPorSede(1L)).thenReturn(BigDecimal.valueOf(5000));
        when(cajaMovimientoRepository.findAllBySedeIdSedeOrderByFechaDesc(1L)).thenReturn(List.of());
        when(productoSedeStockRepository.contarStockBajoPorSede(1L)).thenReturn(2L);
        when(productoSedeStockRepository.findBySedeIdSede(1L)).thenReturn(List.of());

        ComparacionSedesResponse resultado = dashboardService.compararSedes();

        assertThat(resultado.sedes()).hasSize(1);
        assertThat(resultado.sedes().get(0).nombre()).isEqualTo("Villa El Salvador");
        assertThat(resultado.sedes().get(0).ventasTotales()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("Comparación con múltiples sedes")
    void compararMultiplesSedes() {
        Sede sede2 = Sede.builder().idSede(2L).nombre("Chorrillos").activa(true).build();
        when(sedeRepository.findAllByActivaTrueOrderByNombreAsc()).thenReturn(List.of(sede, sede2));
        when(cajaRepository.sumarEfectivoPorSede(1L)).thenReturn(BigDecimal.valueOf(5000));
        when(cajaRepository.sumarEfectivoPorSede(2L)).thenReturn(BigDecimal.valueOf(3000));
        when(cajaMovimientoRepository.findAllBySedeIdSedeOrderByFechaDesc(1L)).thenReturn(List.of());
        when(cajaMovimientoRepository.findAllBySedeIdSedeOrderByFechaDesc(2L)).thenReturn(List.of());
        when(productoSedeStockRepository.contarStockBajoPorSede(1L)).thenReturn(1L);
        when(productoSedeStockRepository.contarStockBajoPorSede(2L)).thenReturn(3L);
        when(productoSedeStockRepository.findBySedeIdSede(1L)).thenReturn(List.of());
        when(productoSedeStockRepository.findBySedeIdSede(2L)).thenReturn(List.of());

        ComparacionSedesResponse resultado = dashboardService.compararSedes();

        assertThat(resultado.sedes()).hasSize(2);
        assertThat(resultado.sedes().get(0).nombre()).isEqualTo("Villa El Salvador");
        assertThat(resultado.sedes().get(1).nombre()).isEqualTo("Chorrillos");
    }

    @Test
    @DisplayName("Generar alertas de stock bajo")
    void generarAlertasStockBajo() {
        when(sedeRepository.findAllByActivaTrueOrderByNombreAsc()).thenReturn(List.of(sede));
        Producto producto = Producto.builder().idProducto(1L).nombre("Audífonos").build();
        ProductoSedeStock pss = ProductoSedeStock.builder()
                .producto(producto).sede(sede).stock(2).stockMinimo(5).build();
        when(productoSedeStockRepository.findBySedeIdSedeAndStockLessThanEqualStockMinimo(1L))
                .thenReturn(List.of(pss));
        when(incidenciaRepository.findAllByOrderByFechaReporteDesc()).thenReturn(List.of());

        AlertasStockResponse resultado = dashboardService.generarAlertas();

        assertThat(resultado.alertas()).isNotEmpty();
        assertThat(resultado.totalAdvertencias()).isEqualTo(1);
    }

    @Test
    @DisplayName("Alerta crítica cuando stock es 0")
    void alertaCriticaStockCero() {
        when(sedeRepository.findAllByActivaTrueOrderByNombreAsc()).thenReturn(List.of(sede));
        Producto producto = Producto.builder().idProducto(1L).nombre("Laptop").build();
        ProductoSedeStock pss = ProductoSedeStock.builder()
                .producto(producto).sede(sede).stock(0).stockMinimo(5).build();
        when(productoSedeStockRepository.findBySedeIdSedeAndStockLessThanEqualStockMinimo(1L))
                .thenReturn(List.of(pss));
        when(incidenciaRepository.findAllByOrderByFechaReporteDesc()).thenReturn(List.of());

        AlertasStockResponse resultado = dashboardService.generarAlertas();

        assertThat(resultado.totalCriticas()).isEqualTo(1);
        assertThat(resultado.alertas().get(0).severidad()).isEqualTo("CRITICA");
    }

    @Test
    @DisplayName("Alertas de incidencias pendientes")
    void alertasIncidenciasPendientes() {
        when(sedeRepository.findAllByActivaTrueOrderByNombreAsc()).thenReturn(List.of(sede));
        when(productoSedeStockRepository.findBySedeIdSedeAndStockLessThanEqualStockMinimo(1L))
                .thenReturn(List.of());

        Incidencia inc = Incidencia.builder()
                .idIncidencia(1L)
                .tipo(TipoIncidencia.DEFECTO)
                .estado(EstadoIncidencia.REPORTADA)
                .producto(Producto.builder().nombre("Mouse").build())
                .descripcion("Mouse defectuoso")
                .build();
        when(incidenciaRepository.findAllByOrderByFechaReporteDesc()).thenReturn(List.of(inc));

        AlertasStockResponse resultado = dashboardService.generarAlertas();

        assertThat(resultado.alertas()).hasSize(1);
        assertThat(resultado.alertas().get(0).tipo()).isEqualTo("INCIDENCIA");
    }

    @Test
    @DisplayName("Resumen financiero consolidado")
    void resumenFinanciero() {
        when(sedeRepository.findAllByActivaTrueOrderByNombreAsc()).thenReturn(List.of(sede));
        when(cajaRepository.sumarEfectivoPorSede(1L)).thenReturn(BigDecimal.valueOf(5000));
        when(productoRepository.count()).thenReturn(16L);
        when(productoRepository.buscarConStockBajo()).thenReturn(List.of());
        when(incidenciaRepository.findAllByOrderByFechaReporteDesc()).thenReturn(List.of());
        when(categoriaRepository.count()).thenReturn(5L);

        ResumenFinancieroResponse resultado = dashboardService.resumenFinanciero();

        assertThat(resultado.totalProductos()).isEqualTo(16);
        assertThat(resultado.totalIncidencias()).isEqualTo(0);
        assertThat(resultado.porSede()).hasSize(1);
    }

    @Test
    @DisplayName("Resumen de inventario por sede")
    void resumenInventario() {
        when(sedeRepository.findAllByActivaTrueOrderByNombreAsc()).thenReturn(List.of(sede));
        when(productoRepository.count()).thenReturn(16L);
        when(productoRepository.valorInventario()).thenReturn(BigDecimal.valueOf(223000));
        when(productoRepository.buscarConStockBajo()).thenReturn(List.of());
        when(categoriaRepository.count()).thenReturn(5L);
        when(productoSedeStockRepository.valorInventarioPorSede(1L)).thenReturn(BigDecimal.valueOf(50000));
        when(productoSedeStockRepository.contarStockBajoPorSede(1L)).thenReturn(2L);
        when(productoSedeStockRepository.findBySedeIdSede(1L)).thenReturn(List.of());

        InventarioResumenResponse resultado = dashboardService.resumenInventario();

        assertThat(resultado.totalProductos()).isEqualTo(16);
        assertThat(resultado.valorInventario()).isEqualByComparingTo(BigDecimal.valueOf(223000));
        assertThat(resultado.totalCategorias()).isEqualTo(5);
        assertThat(resultado.porSede()).hasSize(1);
    }
}
