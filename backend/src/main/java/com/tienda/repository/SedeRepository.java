package com.tienda.repository;

import com.tienda.model.Sede;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SedeRepository extends JpaRepository<Sede, Long> {
    List<Sede> findAllByActivaTrueOrderByNombreAsc();
    boolean existsByNombreIgnoreCase(String nombre);
}
