package com.tienda.repository;

import com.tienda.model.EmpresaCliente;
import com.tienda.model.RegimenFiscal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Acceso a la tabla empresas_clientes (clientes B2B con régimen fiscal). */
public interface EmpresaClienteRepository extends JpaRepository<EmpresaCliente, Long> {

    Optional<EmpresaCliente> findByRfcIgnoreCase(String rfc);

    List<EmpresaCliente> findAllByRegimenFiscal(RegimenFiscal regimenFiscal);

    boolean existsByRfcIgnoreCase(String rfc);
}
