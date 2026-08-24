package com.tienda.repository;

import com.tienda.model.MovimientoAlmacen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoAlmacenRepository extends JpaRepository<MovimientoAlmacen, Long> {

    List<MovimientoAlmacen> findTop100ByOrderByFechaDesc();
}