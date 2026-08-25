package com.tienda.service;

import com.tienda.dto.ProductoResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.Producto;
import com.tienda.model.ProductoSedeStock;
import com.tienda.model.Sede;
import com.tienda.repository.ProductoRepository;
import com.tienda.repository.ProductoSedeStockRepository;
import com.tienda.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de inventario por sede.
 * Permite consultar y gestionar el stock de productos por sucursal.
 */
@Service
@RequiredArgsConstructor
public class ProductoSedeStockService {

    private final ProductoSedeStockRepository productoSedeStockRepository;
    private final ProductoRepository productoRepository;
    private final SedeRepository sedeRepository;

    @Transactional(readOnly = true)
    public List<ProductoSedeStock> obtenerStockPorSede(Long idSede) {
        return productoSedeStockRepository.findBySedeIdSede(idSede);
    }

    @Transactional(readOnly = true)
    public Integer obtenerStockProductoEnSede(Long idProducto, Long idSede) {
        return productoSedeStockRepository.findByProductoIdProductoAndSedeIdSede(idProducto, idSede)
                .map(ProductoSedeStock::getStock)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> obtenerMapaStockSede(Long idSede) {
        return productoSedeStockRepository.findBySedeIdSede(idSede).stream()
                .collect(Collectors.toMap(
                        pss -> pss.getProducto().getIdProducto(),
                        ProductoSedeStock::getStock
                ));
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> productosConStockBajoPorSede(Long idSede) {
        return productoSedeStockRepository.findBySedeIdSedeAndStockLessThanEqualStockMinimo(idSede).stream()
                .map(pss -> {
                    ProductoResponse resp = ProductoResponse.from(pss.getProducto());
                    return new ProductoResponse(
                            resp.idProducto(), resp.sku(), resp.nombre(), resp.descripcion(),
                            resp.precioBase(), pss.getStock(), pss.getStockMinimo(),
                            pss.getStock() <= pss.getStockMinimo(),
                            resp.categoriaId(), resp.categoriaNombre(), resp.garantiaMeses(),
                            resp.proveedorId(), resp.proveedorNombre(), resp.imagenUrl(), resp.activo()
                    );
                })
                .toList();
    }

    @Transactional
    public ProductoSedeStock actualizarStock(Long idProducto, Long idSede, Integer nuevoStock, Integer stockMinimo) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + idProducto + " no encontrado"));
        Sede sede = sedeRepository.findById(idSede)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sede " + idSede + " no encontrada"));

        ProductoSedeStock pss = productoSedeStockRepository.findByProductoIdProductoAndSedeIdSede(idProducto, idSede)
                .orElse(ProductoSedeStock.builder()
                        .producto(producto)
                        .sede(sede)
                        .stock(0)
                        .stockMinimo(5)
                        .build());

        pss.setStock(nuevoStock);
        if (stockMinimo != null) pss.setStockMinimo(stockMinimo);

        return productoSedeStockRepository.save(pss);
    }

    @Transactional
    public void descontarStock(Long idProducto, Long idSede, int cantidad) {
        ProductoSedeStock pss = productoSedeStockRepository.findByProductoIdProductoAndSedeIdSede(idProducto, idSede)
                .orElseThrow(() -> new RecursoNoEncontradoException("Stock del producto " + idProducto + " en sede " + idSede + " no encontrado"));

        if (pss.getStock() < cantidad) {
            throw new com.tienda.exception.StockInsuficienteException(
                    "Stock insuficiente en sede. Disponible: " + pss.getStock() + ", solicitado: " + cantidad);
        }

        pss.setStock(pss.getStock() - cantidad);
        productoSedeStockRepository.save(pss);
    }

    @Transactional
    public void aumentarStock(Long idProducto, Long idSede, int cantidad) {
        ProductoSedeStock pss = productoSedeStockRepository.findByProductoIdProductoAndSedeIdSede(idProducto, idSede)
                .orElseGet(() -> {
                    Producto producto = productoRepository.findById(idProducto)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
                    Sede sede = sedeRepository.findById(idSede)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada"));
                    return productoSedeStockRepository.save(ProductoSedeStock.builder()
                            .producto(producto).sede(sede).stock(0).stockMinimo(5).build());
                });

        pss.setStock(pss.getStock() + cantidad);
        productoSedeStockRepository.save(pss);
    }
}
