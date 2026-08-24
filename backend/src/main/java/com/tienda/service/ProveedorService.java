package com.tienda.service;

import com.tienda.dto.ProveedorRequest;
import com.tienda.model.Proveedor;
import com.tienda.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Catalogo de proveedores (ADMIN). */
@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<Proveedor> listarActivos() {
        return proveedorRepository.findAllByActivoTrueOrderByNombreAsc();
    }

    @Transactional
    public Proveedor crear(ProveedorRequest request) {
        if (proveedorRepository.findAll().stream()
                .anyMatch(p -> p.getNombre().equalsIgnoreCase(request.nombre().trim()))) {
            throw new DataIntegrityViolationException("Ya existe un proveedor con ese nombre");
        }
        return proveedorRepository.save(Proveedor.builder()
                .nombre(request.nombre().trim())
                .contactoNombre(request.contactoNombre())
                .telefono(request.telefono())
                .email(request.email())
                .activo(Boolean.TRUE)
                .build());
    }
}