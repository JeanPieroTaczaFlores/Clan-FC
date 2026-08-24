package com.tienda.service;

import com.tienda.dto.EmpresaClienteRequest;
import com.tienda.dto.EmpresaClienteResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.EmpresaCliente;
import com.tienda.model.Pais;
import com.tienda.repository.EmpresaClienteRepository;
import com.tienda.repository.PaisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogo de empresas clientes B2B.
 * El registro exige país fiscal: la tasa_iva se recalcula desde
 * PAÍS + RÉGIMEN (TaxCalculationService) en vez de aceptarse del cliente.
 */
@Service
@RequiredArgsConstructor
public class EmpresaClienteService {

    private final EmpresaClienteRepository empresaRepository;
    private final PaisRepository paisRepository;
    private final TaxCalculationService taxService;

    @Transactional(readOnly = true)
    public List<EmpresaClienteResponse> listarActivas() {
        return empresaRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .sorted((a, b) -> a.getRazonSocial().compareToIgnoreCase(b.getRazonSocial()))
                .map(EmpresaClienteResponse::from)
                .toList();
    }

    /**
     * Registra una empresa cliente con su país. La tasa de IVA se deriva
     * automáticamente del país + régimen (snapshot para respaldo).
     */
    @Transactional
    public EmpresaClienteResponse crear(EmpresaClienteRequest request) {
        if (empresaRepository.existsByRfcIgnoreCase(request.rfc())) {
            throw new DataIntegrityViolationException("Ya existe una empresa con ese RFC/NIT");
        }
        Pais pais = paisRepository.findById(request.idPais())
                .filter(Pais::getActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "País no encontrado (id=" + request.idPais() + ")"));

        EmpresaCliente empresa = EmpresaCliente.builder()
                .razonSocial(request.razonSocial().trim())
                .rfc(request.rfc().trim().toUpperCase())
                .pais(pais)
                .regimenFiscal(request.regimenFiscal())
                // Snapshot derivado del país; TaxCalculationService prioriza el país dinámico.
                .tasaIva(taxService.resolverTasaConPais(pais, request.regimenFiscal()))
                .contactoEmail(request.contactoEmail())
                .activo(Boolean.TRUE)
                .build();

        return EmpresaClienteResponse.from(empresaRepository.save(empresa));
    }
}
