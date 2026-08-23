package com.tienda.repository;

import com.tienda.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla productos.
 * Incluye consultas derivadas y JPQL usadas por catálogo e inventario.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    /** Catálogo público: solo activos. */
    List<Producto> findAllByActivoTrueOrderByNombreAsc();

    /** Catálogo filtrado por categoría. */
    List<Producto> findAllByCategoriaIdCategoriaAndActivoTrueOrderByNombreAsc(Long idCategoria);

    /**
     * Búsqueda flexible del catálogo: filtra por texto en nombre o SKU
     * (case-insensitive) combinado opcionalmente con categoría.
     */
    @Query("""
           SELECT p FROM Producto p
           WHERE p.activo = TRUE
             AND (:busqueda IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                                  OR LOWER(p.sku)  LIKE LOWER(CONCAT('%', :busqueda, '%')))
             AND (:categoriaId IS NULL OR p.categoria.idCategoria = :categoriaId)
           ORDER BY p.nombre ASC
           """)
    List<Producto> buscarCatalogo(@Param("busqueda") String busqueda,
                                  @Param("categoriaId") Long categoriaId);

    /** Inventario admin: incluye inactivos, con filtros opcionales. */
    @Query("""
           SELECT p FROM Producto p
           WHERE (:busqueda IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                                  OR LOWER(p.sku)  LIKE LOWER(CONCAT('%', :busqueda, '%')))
             AND (:categoriaId IS NULL OR p.categoria.idCategoria = :categoriaId)
           ORDER BY p.idProducto ASC
           """)
    List<Producto> buscarInventario(@Param("busqueda") String busqueda,
                                    @Param("categoriaId") Long categoriaId);

    /** Productos en alerta: stock igual o por debajo del mínimo (más críticos primero). */
    @Query("""
           SELECT p FROM Producto p
           WHERE p.stock <= p.stockMinimo
           ORDER BY p.stock ASC
           """)
    List<Producto> buscarConStockBajo();

    /** Valorización del inventario al costo base (sin IVA). */
    @Query("SELECT COALESCE(SUM(p.precioBase * p.stock), 0) FROM Producto p")
    BigDecimal valorInventario();
}
