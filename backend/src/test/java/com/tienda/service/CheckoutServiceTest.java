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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutService — Unit Tests")
class CheckoutServiceTest {

    @Mock private OrdenRepository ordenRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private InventarioRepository inventarioRepository;
    @Mock private DetalleOrdenRepository detalleOrdenRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PaisRepository paisRepository;
    @Mock private EmpresaClienteRepository empresaClienteRepository;
    @Mock private AlmacenService almacenService;
    @Mock private TaxCalculationService taxCalculationService;

    @InjectMocks
    private CheckoutService checkoutService;

    private Producto producto;
    private Usuario usuario;
    private Pais pais;

    @BeforeEach
    void setUp() {
        Categoria cat = Categoria.builder().idCategoria(1L).nombre("Audio").build();
        producto = Producto.builder()
                .idProducto(1L).sku("AUD-001").nombre("Audífonos")
                .precioBase(BigDecimal.valueOf(549)).stock(28).stockMinimo(5)
                .categoria(cat).activo(true).build();

        Rol rol = Rol.builder().idRol(3L).nombre("CLIENTE").build();
        usuario = Usuario.builder().idUsuario(1L).username("cliente").rol(rol).activo(true).build();
        pais = Pais.builder().idPais(3L).codigoIso2("PE").nombre("Perú")
                .tasaIvaGeneral(BigDecimal.valueOf(18)).tasaIvaReducido(BigDecimal.valueOf(18)).build();
    }

    @Test
    @DisplayName("Calcular IVA de productos")
    void calcularIva() {
        when(taxCalculationService.resolverTasaIva(usuario, null))
                .thenReturn(new TaxCalculationService.TasaIvaResult(
                        BigDecimal.valueOf(18), RegimenFiscal.GENERAL, pais));

        BigDecimal iva = checkoutService.calcularIvaLinea(BigDecimal.valueOf(549), BigDecimal.valueOf(18));

        assertThat(iva).isEqualByComparingTo(BigDecimal.valueOf(98.82));
    }

    @Test
    @DisplayName("Generar folio único")
    void generarFolio() {
        when(ordenRepository.existsByFolio(anyString())).thenReturn(false);

        String folio = checkoutService.generarFolio();

        assertThat(folio).startsWith("ORD-");
        assertThat(folio.length()).isGreaterThan(10);
    }

    @Test
    @DisplayName("Validar stock suficiente")
    void validarStockSuficiente() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(inventarioRepository.descontarStock(1L, 5)).thenReturn(1);

        boolean resultado = checkoutService.validarYDescontarStock(1L, 5);

        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("Stock insuficiente retorna false")
    void stockInsuficiente() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(inventarioRepository.descontarStock(1L, 100)).thenReturn(0);

        boolean resultado = checkoutService.validarYDescontarStock(1L, 100);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Listar órdenes de usuario CLIENTE solo ve las suyas")
    void listarOrdenesCliente() {
        Rol rol = Rol.builder().idRol(3L).nombre("CLIENTE").build();
        Usuario cliente = Usuario.builder().idUsuario(1L).rol(rol).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));

        Orden orden = Orden.builder()
                .idOrden(1L).folio("ORD-001").canal(CanalVenta.WEB)
                .usuario(cliente).subtotal(BigDecimal.valueOf(549))
                .iva(BigDecimal.valueOf(98.82)).total(BigDecimal.valueOf(647.82))
                .estado(EstadoOrden.PAGADA).fechaCreacion(OffsetDateTime.now())
                .detalles(List.of()).build();
        when(ordenRepository.findAllByUsuarioIdUsuarioOrderByFechaCreacionDesc(1L))
                .thenReturn(List.of(orden));

        List<Orden> resultado = checkoutService.listarOrdenes(1L, "CLIENTE");

        assertThat(resultado).hasSize(1);
    }
}
