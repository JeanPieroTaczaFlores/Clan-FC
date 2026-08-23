package com.tienda.service;

import com.tienda.dto.ProductoRequest;
import com.tienda.dto.ProductoResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.Categoria;
import com.tienda.model.Producto;
import com.tienda.repository.CategoriaRepository;
import com.tienda.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lógica de negocio del inventario (CRUD de productos, ADMIN).
 * Reglas: SKU único, categoría obligatoria y existente, stock/precio >= 0.
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    /* ----------------------------- Catálogo ------------------------------- */

    /** Catálogo público: solo activos, con búsqueda y filtro de categoría. */
    @Transactional(readOnly = true)
    public List<ProductoResponse> buscarCatalogo(String busqueda, Long categoriaId) {
        String texto = normalizar(busqueda);
        return productoRepository.buscarCatalogo(texto, categoriaId).stream()
                .map(ProductoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        return ProductoResponse.from(buscarProducto(id));
    }

    /* ---------------------------- Inventario ------------------------------ */

    /** Vista admin: incluye inactivos. */
    @Transactional(readOnly = true)
    public List<ProductoResponse> buscarInventario(String busqueda, Long categoriaId) {
        String texto = normalizar(busqueda);
        return productoRepository.buscarInventario(texto, categoriaId).stream()
                .map(ProductoResponse::from).toList();
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        validarSkuUnico(request.sku(), null);
        Categoria categoria = buscarCategoria(request.categoriaId());

        Producto producto = Producto.builder()
                .sku(request.sku().trim().toUpperCase())
                .nombre(request.nombre().trim())
                .descripcion(request.descripcion())
                .precioBase(request.precioBase())
                .stock(request.stock())
                .stockMinimo(request.stockMinimo())
                .imagenUrl(request.imagenUrl())
                .categoria(categoria)
                .activo(request.activo() == null || request.activo())
                .build();

        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarProducto(id);
        validarSkuUnico(request.sku(), id);
        Categoria categoria = buscarCategoria(request.categoriaId());

        producto.setSku(request.sku().trim().toUpperCase());
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setPrecioBase(request.precioBase());
        producto.setStock(request.stock());
        producto.setStockMinimo(request.stockMinimo());
        producto.setImagenUrl(request.imagenUrl());
        producto.setCategoria(categoria);
        if (request.activo() != null) producto.setActivo(request.activo());

        return ProductoResponse.from(productoRepository.save(producto));
    }

    /** Borrado físico (uso admin). Las órdenes históricas guardan snapshot. */
    @Transactional
    public void eliminar(Long id) {
        productoRepository.delete(buscarProducto(id));
    }

    /* ------------------------- Consultas auxiliares ----------------------- */

    /** Productos con stock <= mínimo (alertas dashboard Entregable 3). */
    @Transactional(readOnly = true)
    public List<ProductoResponse> productosConStockBajo() {
        return productoRepository.buscarConStockBajo().stream()
                .map(ProductoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal valorInventario() {
        return productoRepository.valorInventario();
    }

    /* ------------------------------ Privados ------------------------------ */

    private void validarSkuUnico(String sku, Long idExcluido) {
        productoRepository.findBySkuIgnoreCase(sku).ifPresent(p -> {
            if (!p.getIdProducto().equals(idExcluido)) {
                throw new IllegalArgumentException("El SKU " + sku + " ya existe");
            }
        });
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría " + id + " no encontrada"));
    }

    private Producto buscarProducto(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no encontrado"));
    }

    private String normalizar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
