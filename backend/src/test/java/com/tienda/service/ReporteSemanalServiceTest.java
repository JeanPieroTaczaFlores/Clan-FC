package com.tienda.service;

import com.tienda.dto.ReporteSemanalResponse;
import com.tienda.model.*;
import com.tienda.repository.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReporteSemanalService — Unit Tests")
class ReporteSemanalServiceTest {

    @Mock private OrdenRepository ordenRepository;
    @Mock private ProductoService productoService;

    @InjectMocks
    private ReporteSemanalService reporteSemanalService;

    @Test
    @DisplayName("Generar reporte sin órdenes retorna ceros")
    void reporteSinOrdenes() {
        when(ordenRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of());
        when(productoService.productosConStockBajo()).thenReturn(List.of());

        ReporteSemanalResponse resultado = reporteSemanalService.generar();

        assertThat(resultado.totalVentas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.numeroOrdenes()).isEqualTo(0);
        assertThat(resultado.ticketPromedio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.dias()).hasSize(7);
    }

    @Test
    @DisplayName("Reporte con órdenes recientes")
    void reporteConOrdenes() {
        Rol rol = Rol.builder().idRol(1L).nombre("CAJERO").build();
        Usuario usuario = Usuario.builder().idUsuario(1L).username("cajero1").rol(rol).build();
        Orden orden = Orden.builder()
                .idOrden(1L).folio("ORD-001").canal(CanalVenta.CAJA)
                .usuario(usuario).subtotal(BigDecimal.valueOf(549))
                .iva(BigDecimal.valueOf(98.82)).total(BigDecimal.valueOf(647.82))
                .estado(EstadoOrden.PAGADA)
                .fechaCreacion(OffsetDateTime.now())
                .detalles(List.of())
                .build();

        when(ordenRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(orden));
        when(productoService.productosConStockBajo()).thenReturn(List.of());

        ReporteSemanalResponse resultado = reporteSemanalService.generar();

        assertThat(resultado.numeroOrdenes()).isEqualTo(1);
        assertThat(resultado.totalVentas()).isEqualByComparingTo(BigDecimal.valueOf(647.82));
    }

    @Test
    @DisplayName("Días del reporte siempre son 7")
    void siempreSieteDias() {
        when(ordenRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of());
        when(productoService.productosConStockBajo()).thenReturn(List.of());

        ReporteSemanalResponse resultado = reporteSemanalService.generar();

        assertThat(resultado.dias()).hasSize(7);
        // El primer día debe tener fecha anterior al último
        assertThat(resultado.dias().get(0).fecha())
                .isLessThan(resultado.dias().get(6).fecha());
    }

    @Test
    @DisplayName("Ticket promedio se calcula correctamente")
    void ticketPromedio() {
        Rol rol = Rol.builder().idRol(1L).nombre("CAJERO").build();
        Usuario usuario = Usuario.builder().idUsuario(1L).username("cajero1").rol(rol).build();

        Orden o1 = Orden.builder().idOrden(1L).folio("ORD-001").canal(CanalVenta.CAJA)
                .usuario(usuario).total(BigDecimal.valueOf(1000)).estado(EstadoOrden.PAGADA)
                .fechaCreacion(OffsetDateTime.now()).detalles(List.of()).build();
        Orden o2 = Orden.builder().idOrden(2L).folio("ORD-002").canal(CanalVenta.CAJA)
                .usuario(usuario).total(BigDecimal.valueOf(2000)).estado(EstadoOrden.PAGADA)
                .fechaCreacion(OffsetDateTime.now()).detalles(List.of()).build();

        when(ordenRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(o1, o2));
        when(productoService.productosConStockBajo()).thenReturn(List.of());

        ReporteSemanalResponse resultado = reporteSemanalService.generar();

        assertThat(resultado.numeroOrdenes()).isEqualTo(2);
        assertThat(resultado.ticketPromedio()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }
}
