package com.tienda.service;

import com.tienda.dto.CheckoutRequest;
import com.tienda.dto.OrdenResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.exception.StockInsuficienteException;
import com.tienda.model.*;
import com.tienda.repository.EmpresaClienteRepository;
import com.tienda.repository.InventarioRepository;
import com.tienda.repository.OrdenRepository;
import com.tienda.repository.ProductoRepository;
import com.tienda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.security.SecureRandom;

/**
 * PROCESO DE CHECKOUT / VENTA EN CAJA (Entregable 2) — todo bajo una única
 * transacción de PostgreSQL (@Transactional):
 *
 *   a) Validación y RESERVA de stock (descuento atómico por línea).
 *   b) Registro de la orden con subtotal, IVA parametrizado y total.
 *   c) Inserción en ordenes + detalle_ordenes con canal WEB/CAJA y snapshot
 *      fiscal del régimen/tasa aplicados.
 *   d) Descuento definitivo del inventario (mismo UPDATE del paso "a":
 *      si algo falla después, el rollback devuelve el stock).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final OrdenRepository ordenRepository;
    private final InventarioRepository inventarioRepository;
    private final ProductoRepository productoRepository;
    private final EmpresaClienteRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TaxCalculationService taxService;
    private final AlmacenService almacenService; // kardex: SALIDA_VENTA por línea

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter FOLIO_FECHA =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * Procesa una venta completa.
     * @param request  items + empresa compradora + método de pago
     * @param canal    WEB (cliente online) o CAJA (POS del cajero)
     * @param username usuario autenticado que ejecuta la operación
     */
    @Transactional
    public OrdenResponse procesar(CheckoutRequest request, CanalVenta canal, String username) {
        // --- Datos base de la venta ---
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + username));

        EmpresaCliente empresa = null;
        if (request.empresaClienteId() != null) {
            empresa = empresaRepository.findById(request.empresaClienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Empresa cliente " + request.empresaClienteId() + " no encontrada"));
        }

        RegimenFiscal regimen = taxService.regimenDe(empresa);
        // Sin empresa: el país del USUARIO define su IVA de consumidor final.
        BigDecimal tasa = taxService.resolverTasa(empresa, usuario.getPais());

        // --- Orden cabecera (aún sin montos; se llenan al recorrer líneas) ---
        Orden orden = Orden.builder()
                .folio(generarFolio())
                .canal(canal)
                .usuario(usuario)
                .empresaCliente(empresa)
                .pais(empresa != null ? empresa.getPais() : usuario.getPais()) // snapshot país fiscal
                .regimenFiscal(regimen)
                .tasaIva(tasa)
                .subtotal(BigDecimal.ZERO)
                .iva(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .metodoPago(normalizarMetodoPago(request.metodoPago()))
                .estado(EstadoOrden.PAGADA)
                .build();

        BigDecimal acumuladoBase = BigDecimal.ZERO;
        BigDecimal acumuladoIva = BigDecimal.ZERO;

        // --- Por cada línea: reservar stock + crear detalle ---
        for (CheckoutRequest.ItemCheckoutRequest item : request.items()) {
            int cantidad = item.cantidad() == null ? 1 : item.cantidad();

            Producto producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Producto " + item.productoId() + " no encontrado"));

            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new StockInsuficienteException(
                        "El producto \"" + producto.getNombre() + "\" no está disponible");
            }

            // d) DESCUENTO DEFINITIVO ATÓMICO: falla si el stock ya no alcanza
            // (protege contra ventas simultáneas del mismo inventario).
            int filas = inventarioRepository.descontarStock(producto.getIdProducto(), cantidad);
            if (filas == 0) {
                throw new StockInsuficienteException("Stock insuficiente para \""
                        + producto.getNombre() + "\" (disponible: "
                        + producto.getStock() + ", solicitado: " + cantidad + ")");
            }

            BigDecimal precioUnitario = producto.getPrecioBase();
            BigDecimal subtotalLinea = precioUnitario.multiply(BigDecimal.valueOf(cantidad))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal ivaLinea = taxService.calcularIva(subtotalLinea, tasa);

            orden.addItem(DetalleOrden.builder()
                    .orden(orden)
                    .producto(producto)
                    .sku(producto.getSku())
                    .nombreProducto(producto.getNombre())
                    .cantidad(cantidad)
                    .precioUnitario(precioUnitario)
                    .ivaLinea(ivaLinea)
                    .subtotalLinea(subtotalLinea)
                    .build());

            acumuladoBase = acumuladoBase.add(subtotalLinea);
            acumuladoIva = acumuladoIva.add(ivaLinea);

            // Kardex: la venta queda trazada en movimientos_almacen.
            almacenService.registrarSalidaVenta(
                    producto, cantidad, orden.getFolio(), usuario);
        }

        // --- b/c) Montos finales y persistencia en cascada ---
        orden.setSubtotal(acumuladoBase.setScale(2, RoundingMode.HALF_UP));
        orden.setIva(acumuladoIva.setScale(2, RoundingMode.HALF_UP));
        orden.setTotal(orden.getSubtotal().add(orden.getIva()));

        Orden guardada = ordenRepository.save(orden);
        log.info("Orden {} registrada: canal={}, items={}, total={}",
                guardada.getFolio(), canal, guardada.getDetalles().size(), guardada.getTotal());

        return OrdenResponse.from(guardada);
    }

    /** Lista órdenes según el rol del solicitante (autorización de lectura). */
    @Transactional(readOnly = true)
    public List<OrdenResponse> listarSegunRol(String username, boolean esAdmin, boolean esCajero) {
        List<Orden> ordenes;
        if (esAdmin) {
            ordenes = ordenRepository.findAllByOrderByFechaCreacionDesc();           // ADMIN: todas
        } else if (esCajero) {
            ordenes = ordenRepository.findAllByCanalOrderByFechaCreacionDesc(CanalVenta.CAJA); // CAJERO: sus cobros de caja
        } else {
            Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
            ordenes = ordenRepository.findAllByUsuarioIdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario()); // CLIENTE: las suyas
        }
        return ordenes.stream().map(OrdenResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrdenResponse obtenerPorId(Long id) {
        return ordenRepository.findById(id)
                .map(OrdenResponse::from)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden " + id + " no encontrada"));
    }

    /* ------------------------------ Privados ------------------------------ */

    /**
     * Folio legible y prácticamente único: ORD-{fecha}-{sufijo aleatorio}.
     * El sufijo evita colisiones si dos ventas se confirman en el mismo segundo.
     */
    private String generarFolio() {
        String sufijo = Integer.toString(100 + RANDOM.nextInt(900), 36).toUpperCase(Locale.ROOT);
        String folio = "ORD-" + LocalDateTime.now().format(FOLIO_FECHA) + "-" + sufijo;
        return ordenRepository.existsByFolio(folio) ? folio + "-" + RANDOM.nextInt(10) : folio;
    }

    private String normalizarMetodoPago(String metodoPago) {
        if (metodoPago == null || metodoPago.isBlank()) return "EFECTIVO";
        return switch (metodoPago.trim().toUpperCase(Locale.ROOT)) {
            case "TARJETA" -> "TARJETA";
            case "TRANSFERENCIA" -> "TRANSFERENCIA";
            default -> "EFECTIVO";
        };
    }
}
