package com.tienda.service;

import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.exception.StockInsuficienteException;
import com.tienda.model.Producto;
import com.tienda.model.ProductoSedeStock;
import com.tienda.model.Sede;
import com.tienda.repository.ProductoRepository;
import com.tienda.repository.ProductoSedeStockRepository;
import com.tienda.repository.SedeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoSedeStockService — Unit Tests")
class ProductoSedeStockServiceTest {

    @Mock private ProductoSedeStockRepository productoSedeStockRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private SedeRepository sedeRepository;

    @InjectMocks
    private ProductoSedeStockService productoSedeStockService;

    private Sede sede;
    private Producto producto;
    private ProductoSedeStock pss;

    @BeforeEach
    void setUp() {
        sede = Sede.builder().idSede(1L).nombre("Villa El Salvador").build();
        producto = Producto.builder()
                .idProducto(1L).sku("AUD-001").nombre("Audífonos OneOdio")
                .precioBase(BigDecimal.valueOf(549)).stock(28).stockMinimo(5).build();
        pss = ProductoSedeStock.builder()
                .id(1L).producto(producto).sede(sede)
                .stock(25).stockMinimo(5).build();
    }

    @Test
    @DisplayName("Obtener stock por sede")
    void obtenerStockPorSede() {
        when(productoSedeStockRepository.findBySedeIdSede(1L)).thenReturn(List.of(pss));

        List<ProductoSedeStock> resultado = productoSedeStockService.obtenerStockPorSede(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getStock()).isEqualTo(25);
    }

    @Test
    @DisplayName("Obtener stock de producto en sede específica")
    void obtenerStockProductoEnSede() {
        when(productoSedeStockRepository.findByProductoIdProductoAndSedeIdSede(1L, 1L))
                .thenReturn(Optional.of(pss));

        Integer stock = productoSedeStockService.obtenerStockProductoEnSede(1L, 1L);

        assertThat(stock).isEqualTo(25);
    }

    @Test
    @DisplayName("Stock de producto no registrado en sede retorna 0")
    void stockNoRegistradoRetornaCero() {
        when(productoSedeStockRepository.findByProductoIdProductoAndSedeIdSede(99L, 1L))
                .thenReturn(Optional.empty());

        Integer stock = productoSedeStockService.obtenerStockProductoEnSede(99L, 1L);

        assertThat(stock).isEqualTo(0);
    }

    @Test
    @DisplayName("Obtener mapa de stock por sede")
    void obtenerMapaStock() {
        when(productoSedeStockRepository.findBySedeIdSede(1L)).thenReturn(List.of(pss));

        Map<Long, Integer> mapa = productoSedeStockService.obtenerMapaStockSede(1L);

        assertThat(mapa).containsEntry(1L, 25);
    }

    @Test
    @DisplayName("Productos con stock bajo por sede")
    void productosConStockBajo() {
        ProductoSedeStock pssBajo = ProductoSedeStock.builder()
                .id(2L).producto(producto).sede(sede)
                .stock(2).stockMinimo(5).build();
        when(productoSedeStockRepository.findBySedeIdSedeAndStockLessThanEqualStockMinimo(1L))
                .thenReturn(List.of(pssBajo));

        var resultado = productoSedeStockService.productosConStockBajoPorSede(1L);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("Actualizar stock existente")
    void actualizarStock() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(sede));
        when(productoSedeStockRepository.findByProductoIdProductoAndSedeIdSede(1L, 1L))
                .thenReturn(Optional.of(pss));
        when(productoSedeStockRepository.save(any())).thenReturn(pss);

        ProductoSedeStock resultado = productoSedeStockService.actualizarStock(1L, 1L, 30, 5);

        assertThat(resultado.getStock()).isEqualTo(30);
    }

    @Test
    @DisplayName("Descuento exitoso de stock")
    void descontarStock() {
        when(productoSedeStockRepository.findByProductoIdProductoAndSedeIdSede(1L, 1L))
                .thenReturn(Optional.of(pss));

        productoSedeStockService.descontarStock(1L, 1L, 10);

        verify(productoSedeStockRepository).save(any());
        assertThat(pss.getStock()).isEqualTo(15); // 25 - 10
    }

    @Test
    @DisplayName("Descuento con stock insuficiente lanza excepción")
    void descontarStockInsuficiente() {
        when(productoSedeStockRepository.findByProductoIdProductoAndSedeIdSede(1L, 1L))
                .thenReturn(Optional.of(pss));

        assertThatThrownBy(() -> productoSedeStockService.descontarStock(1L, 1L, 50))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("insuficiente");
    }

    @Test
    @DisplayName("Aumentar stock existente")
    void aumentarStock() {
        when(productoSedeStockRepository.findByProductoIdProductoAndSedeIdSede(1L, 1L))
                .thenReturn(Optional.of(pss));

        productoSedeStockService.aumentarStock(1L, 1L, 15);

        verify(productoSedeStockRepository).save(any());
        assertThat(pss.getStock()).isEqualTo(40); // 25 + 15
    }

    @Test
    @DisplayName("Aumentar stock crea registro si no existe")
    void aumentarStockCreaRegistro() {
        when(productoSedeStockRepository.findByProductoIdProductoAndSedeIdSede(1L, 1L))
                .thenReturn(Optional.empty());
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(sede));
        when(productoSedeStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        productoSedeStockService.aumentarStock(1L, 1L, 10);

        verify(productoSedeStockRepository, times(2)).save(any());
    }
}
