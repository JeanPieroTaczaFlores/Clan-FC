package com.tienda.service;

import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.*;
import com.tienda.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlmacenService — Unit Tests")
class AlmacenServiceTest {

    @Mock private MovimientoAlmacenRepository movimientoAlmacenRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AlmacenService almacenService;

    private Producto producto;
    private Proveedor proveedor;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        proveedor = Proveedor.builder().idProveedor(1L).nombre("AudioMax").build();
        usuario = Usuario.builder().idUsuario(1L).username("admin").build();
        producto = Producto.builder()
                .idProducto(1L).sku("AUD-001").nombre("Audífonos")
                .stock(28).proveedor(proveedor).activo(true).build();
    }

    @Test
    @DisplayName("Registrar entrada de mercancía")
    void registrarEntrada() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(productoRepository.save(any())).thenReturn(producto);
        when(movimientoAlmacenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.tienda.dto.MovimientoAlmacenRequest(
                1L, 10, "ENTRADA", 1L, "FAC-001", "Compra a proveedor");

        var resultado = almacenService.registrarMovimiento(request, 1L);

        assertThat(resultado).isNotNull();
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("Registrar merma reduce stock")
    void registrarMerma() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.save(any())).thenReturn(producto);
        when(movimientoAlmacenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.tienda.dto.MovimientoAlmacenRequest(
                1L, 5, "MERMA", null, null, "Producto dañado");

        var resultado = almacenService.registrarMovimiento(request, 1L);

        assertThat(producto.getStock()).isEqualTo(23); // 28 - 5
    }

    @Test
    @DisplayName("Stock insuficiente para salida lanza excepción")
    void stockInsuficienteSalida() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        var request = new com.tienda.dto.MovimientoAlmacenRequest(
                1L, 100, "MERMA", null, null, "Demasiados dañados");

        assertThatThrownBy(() -> almacenService.registrarMovimiento(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    @DisplayName("Listar movimientos recientes")
    void listarMovimientos() {
        when(movimientoAlmacenRepository.findTop100ByOrderByFechaDesc()).thenReturn(List.of());

        var resultado = almacenService.listarRecientes();

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Producto inexistente lanza excepción")
    void productoInexistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new com.tienda.dto.MovimientoAlmacenRequest(
                99L, 10, "ENTRADA", null, null, null);

        assertThatThrownBy(() -> almacenService.registrarMovimiento(request, 1L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
