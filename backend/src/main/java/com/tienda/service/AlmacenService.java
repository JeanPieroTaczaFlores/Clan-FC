package com.tienda.service;

import com.tienda.dto.MovimientoAlmacenRequest;
import com.tienda.dto.MovimientoResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.*;
import com.tienda.repository.MovimientoAlmacenRepository;
import com.tienda.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * ALMACÉN — kardex del inventario.
 *
 * registrar: entradas de mercancía, mermas y devoluciones que reporta el
 * cajero; el stock se ajusta y queda rastro (quién, cuándo, cuánto, factura).
 * registrarSalidaVenta: lo invoca CheckoutService en cada venta.
 */
@Service
@RequiredArgsConstructor
public class AlmacenService {

    private final MovimientoAlmacenRepository movimientosRepository;
    private final ProveedorRepository proveedorRepository;

    /** Tipos permitidos desde la interfaz del POS. */
    private static final List<String> TIPOS_UI = List.of("ENTRADA", "DEVOLUCION", "MERMA", "AJUSTE");

    /** Entrada/salida manual con producto ya cargado. */
    @Transactional
    public MovimientoResponse registrar(Producto producto, MovimientoAlmacenRequest request, Usuario usuario) {
        String tipoTexto = (request.tipo() == null || request.tipo().isBlank())
                ? "ENTRADA" : request.tipo().trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_UI.contains(tipoTexto)) {
            throw new IllegalArgumentException("Tipo de movimiento inválido: " + tipoTexto);
        }
        TipoMovimiento tipo = TipoMovimiento.valueOf(tipoTexto);

        boolean suma = (tipo == TipoMovimiento.ENTRADA || tipo == TipoMovimiento.DEVOLUCION);
        int nuevoStock = suma
                ? producto.getStock() + request.cantidad()
                : producto.getStock() - request.cantidad();

        if (nuevoStock < 0) {
            throw new IllegalArgumentException("Stock insuficiente para descontar "
                    + request.cantidad() + " unidades (disponible: " + producto.getStock() + ")");
        }
        producto.setStock(nuevoStock);

        Proveedor proveedor = request.proveedorId() != null
                ? proveedorRepository.findById(request.proveedorId()).orElse(null)
                : null;

        MovimientoAlmacen guardado = movimientosRepository.save(MovimientoAlmacen.builder()
                .tipo(tipo)
                .producto(producto)
                .cantidad(request.cantidad())
                .stockResultante(nuevoStock)
                .referencia(request.referencia())
                .nota(request.nota())
                .proveedor(proveedor)
                .usuario(usuario)
                .build());

        return MovimientoResponse.from(guardado);
    }

    /** Kardex visible en POS: últimos 100 movimientos. */
    @Transactional(readOnly = true)
    public List<MovimientoResponse> listarRecientes() {
        return movimientosRepository.findTop100ByOrderByFechaDesc().stream()
                .map(MovimientoResponse::from).toList();
    }

    /**
     * SALIDA_VENTA por cada línea vendida — misma transacción del checkout:
     * si la venta hace rollback, el kardex también.
     */
    @Transactional
    public void registrarSalidaVenta(Producto producto, int cantidad, String folioOrden, Usuario usuario) {
        movimientosRepository.save(MovimientoAlmacen.builder()
                .tipo(TipoMovimiento.SALIDA_VENTA)
                .producto(producto)
                .cantidad(cantidad)
                .stockResultante(producto.getStock()) // ya viene descontado por el checkout
                .referencia(folioOrden)
                .usuario(usuario)
                .build());
    }
}
