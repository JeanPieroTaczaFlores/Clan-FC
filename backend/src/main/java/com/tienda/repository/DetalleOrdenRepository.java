package com.tienda.repository;

import com.tienda.model.DetalleOrden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a la tabla detalle_ordenes (líneas de venta). */
public interface DetalleOrdenRepository extends JpaRepository<DetalleOrden, Long> {

    List<DetalleOrden> findAllByOrdenIdOrden(Long idOrden);
}
