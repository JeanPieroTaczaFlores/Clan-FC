package com.tienda.repository;

import com.tienda.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findAllByActivoTrueOrderByNombreAsc();
}