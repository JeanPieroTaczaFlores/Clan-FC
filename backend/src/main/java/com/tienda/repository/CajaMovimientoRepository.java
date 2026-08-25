package com.tienda.repository;

import com.tienda.model.CajaMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CajaMovimientoRepository extends JpaRepository<CajaMovimiento, Long> {

    List<CajaMovimiento> findAllByCajaIdCajaOrderByFechaDesc(Long idCaja);

    List<CajaMovimiento> findAllBySedeIdSedeOrderByFechaDesc(Long idSede);

    List<CajaMovimiento> findTop50ByOrderByFechaDesc();

    List<CajaMovimiento> findAllByOrderByFechaDesc();
}
