package com.tienda.repository;

import com.tienda.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Acceso a la tabla categorias. */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByActivaTrueOrderByNombreAsc();

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);
}
