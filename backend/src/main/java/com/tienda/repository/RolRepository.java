package com.tienda.repository;

import com.tienda.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acceso a la tabla roles. */
public interface RolRepository extends JpaRepository<Rol, Long> {

    /** Búsqueda por nombre corto (ADMIN, CAJERO, CLIENTE). */
    Optional<Rol> findByNombreIgnoreCase(String nombre);
}
