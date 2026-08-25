package com.tienda.repository;

import com.tienda.model.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    List<Caja> findAllBySedeIdSedeOrderByNumeroCajaAsc(Long idSede);

    Optional<Caja> findBySedeIdSedeAndNumeroCaja(Long idSede, Integer numeroCaja);

    Optional<Caja> findBySedeIdSedeAndUsuarioUsername(Long idSede, String username);

    Optional<Caja> findBySedeIdSedeAndEstado(Long idSede, String estado);

    @Query("SELECT COALESCE(SUM(c.efectivo), 0) FROM Caja c WHERE c.sede.idSede = :sedeId")
    java.math.BigDecimal sumarEfectivoPorSede(@Param("sedeId") Long sedeId);

    long countBySedeIdSedeAndEstado(Long idSede, String estado);
}
