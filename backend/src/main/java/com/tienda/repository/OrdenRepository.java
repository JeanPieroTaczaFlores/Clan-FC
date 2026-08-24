package com.tienda.repository;

import com.tienda.model.CanalVenta;
import com.tienda.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/** Acceso a la tabla ordenes (ventas web y de caja). */
public interface OrdenRepository extends JpaRepository<Orden, Long> {

    List<Orden> findAllByOrderByFechaCreacionDesc();

    List<Orden> findAllByCanalOrderByFechaCreacionDesc(CanalVenta canal);

    List<Orden> findAllByUsuarioIdUsuarioOrderByFechaCreacionDesc(Long idUsuario);

    boolean existsByFolio(String folio);

    /** Suma de totales pagados por canal (métricas del dashboard, Entregable 3). */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Orden o WHERE o.canal = :canal AND o.estado = 'PAGADA'")
    BigDecimal sumarTotalPorCanal(@Param("canal") CanalVenta canal);
}
