package com.tienda.repository;

import com.tienda.model.Pais;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaisRepository extends JpaRepository<Pais, Long> {

    List<Pais> findAllByActivoTrueOrderByNombreAsc();

    Optional<Pais> findByCodigoIso2IgnoreCase(String codigoIso2);
}
