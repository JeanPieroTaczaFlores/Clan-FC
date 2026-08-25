package com.tienda.service;

import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.Categoria;
import com.tienda.model.Producto;
import com.tienda.model.Proveedor;
import com.tienda.dto.ProductoRequest;
import com.tienda.dto.ProductoResponse;
import com.tienda.repository.CategoriaRepository;
import com.tienda.repository.ProductoRepository;
import com.tienda.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService — Unit Tests")
class ProductoServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ProveedorRepository proveedorRepository;

    @InjectMocks
    private ProductoService productoService;

    private Producto producto;
    private Categoria categoria;
    private Proveedor proveedor;

    @BeforeEach
    void setUp() {
        categoria = Categoria.builder().idCategoria(1L).nombre("Audio").build();
        proveedor = Proveedor.builder().idProveedor(1L).nombre("AudioMax").build();
        producto = Producto.builder()
                .idProducto(1L).sku("AUD-001").nombre("Audífonos OneOdio")
                .precioBase(BigDecimal.valueOf(549)).stock(28).stockMinimo(5)
                .categoria(categoria).proveedor(proveedor).activo(true).build();
    }

    @Test
    @DisplayName("Buscar catálogo con texto")
    void buscarCatalogoConTexto() {
        when(productoRepository.buscarCatalogo("audifonos", null))
                .thenReturn(List.of(producto));

        List<ProductoResponse> resultado = productoService.buscarCatalogo("audifonos", null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Audífonos OneOdio");
    }

    @Test
    @DisplayName("Buscar catálogo con texto null retorna todo")
    void buscarCatalogoSinFiltro() {
        when(productoRepository.buscarCatalogo(null, null))
                .thenReturn(List.of(producto));

        List<ProductoResponse> resultado = productoService.buscarCatalogo(null, null);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("Obtener producto por ID")
    void obtenerPorId() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        ProductoResponse respuesta = productoService.obtenerPorId(1L);

        assertThat(respuesta.sku()).isEqualTo("AUD-001");
        assertThat(respuesta.categoriaNombre()).isEqualTo("Audio");
    }

    @Test
    @DisplayName("Obtener producto inexistente lanza excepción")
    void obtenerInexistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Crear producto válido")
    void crearProducto() {
        when(productoRepository.findBySkuIgnoreCase("AUD-003")).thenReturn(Optional.empty());
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        ProductoRequest request = new ProductoRequest(
                "AUD-003", "Nuevo Audífonos", "Descripción", BigDecimal.valueOf(549),
                28, 5, 1L, 12, 1L, null, true);
        ProductoResponse respuesta = productoService.crear(request);

        assertThat(respuesta).isNotNull();
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("Crear producto con SKU duplicado lanza excepción")
    void crearSkuDuplicado() {
        when(productoRepository.findBySkuIgnoreCase("AUD-001")).thenReturn(Optional.of(producto));

        ProductoRequest request = new ProductoRequest(
                "AUD-001", "Duplicado", null, BigDecimal.valueOf(100),
                10, 5, 1L, 12, null, null, true);

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya existe");
    }

    @Test
    @DisplayName("Actualizar producto existente")
    void actualizarProducto() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.findBySkuIgnoreCase("AUD-001")).thenReturn(Optional.of(producto));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        ProductoRequest request = new ProductoRequest(
                "AUD-001", "Audífonos Actualizado", "Nueva desc", BigDecimal.valueOf(599),
                30, 5, 1L, 12, 1L, null, true);
        ProductoResponse respuesta = productoService.actualizar(1L, request);

        assertThat(respuesta.nombre()).isEqualTo("Audífonos Actualizado");
    }

    @Test
    @DisplayName("Eliminar producto existente")
    void eliminarProducto() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        productoService.eliminar(1L);

        verify(productoRepository).delete(producto);
    }

    @Test
    @DisplayName("Productos con stock bajo")
    void productosConStockBajo() {
        when(productoRepository.buscarConStockBajo()).thenReturn(List.of(producto));

        List<ProductoResponse> resultado = productoService.productosConStockBajo();

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("Valorización del inventario")
    void valorInventario() {
        when(productoRepository.valorInventario()).thenReturn(BigDecimal.valueOf(223433));

        BigDecimal valor = productoService.valorInventario();

        assertThat(valor).isEqualByComparingTo(BigDecimal.valueOf(223433));
    }
}
