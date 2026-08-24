package com.tienda.repository;

import com.tienda.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Extensión del repositorio de productos con operaciones de inventario
 * atómicas usadas por el checkout transaccional.
 */
public interface InventarioRepository extends JpaRepository<Producto, Long> {

    /**
     * Descuento ATÓMICO de stock: la condición "stock >= :cantidad" en el
     * WHERE garantiza que nunca quede negativo incluso con ventas concurrentes.
     * @return 1 si descontó, 0 si no había stock suficiente.
     */
    @Modifying
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad WHERE p.idProducto = :id AND p.stock >= :cantidad")
    int descontarStock(@Param("id") Long idProducto, @Param("cantidad") Integer cantidad);
}
