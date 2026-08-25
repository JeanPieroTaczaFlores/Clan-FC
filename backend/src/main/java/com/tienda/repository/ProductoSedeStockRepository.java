package com.tienda.repository;

import com.tienda.model.ProductoSedeStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductoSedeStockRepository extends JpaRepository<ProductoSedeStock, Long> {

    List<ProductoSedeStock> findBySedeIdSede(Long idSede);

    Optional<ProductoSedeStock> findByProductoIdProductoAndSedeIdSede(Long idProducto, Long idSede);

    List<ProductoSedeStock> findBySedeIdSedeAndStockLessThanEqualStockMinimo(Long idSede);

    @Query("SELECT COALESCE(SUM(p.precioBase * pss.stock), 0) " +
           "FROM ProductoSedeStock pss JOIN pss.producto p " +
           "WHERE pss.sede.idSede = :sedeId")
    BigDecimal valorInventarioPorSede(@Param("sedeId") Long sedeId);

    @Query("SELECT COUNT(pss) FROM ProductoSedeStock pss " +
           "WHERE pss.sede.idSede = :sedeId AND pss.stock <= pss.stockMinimo")
    long contarStockBajoPorSede(@Param("sedeId") Long sedeId);

    @Query("SELECT COALESCE(SUM(p.precioBase * pss.stock), 0) " +
           "FROM ProductoSedeStock pss JOIN pss.producto p")
    BigDecimal valorInventarioTotal();
}
