package com.tienda.controller;

import com.tienda.dto.EmpresaClienteRequest;
import com.tienda.dto.EmpresaClienteResponse;
import com.tienda.model.Pais;
import com.tienda.repository.PaisRepository;
import com.tienda.service.EmpresaClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catálogo de PAÍSES (con bandera y tasas de IVA por país) y registro de
 * EMPRESAS CLIENTES B2B. El frontend pinta las banderas junto al nombre.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EmpresasController {

    private final EmpresaClienteService empresaService;
    private final PaisRepository paisRepository;

    /** Catálogo de países activos para selectores (ordenado alfabético). */
    @GetMapping("/paises")
    public List<Pais> listarPaises() {
        return paisRepository.findAllByActivoTrueOrderByNombreAsc();
    }

    @GetMapping("/empresas")
    public List<EmpresaClienteResponse> listarEmpresas() {
        return empresaService.listarActivas();
    }

    /** Registro B2B: solo ADMIN. La tasa se deriva del país elegido. */
    @PostMapping("/empresas")
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaClienteResponse crearEmpresa(@Valid @RequestBody EmpresaClienteRequest request) {
        return empresaService.crear(request);
    }
}
