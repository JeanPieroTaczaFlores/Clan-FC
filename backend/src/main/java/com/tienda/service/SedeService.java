package com.tienda.service;

import com.tienda.dto.SedeRequest;
import com.tienda.dto.SedeResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.Sede;
import com.tienda.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestión de sedes (sucursales) de TiendaMenos.
 * Solo ADMIN puede crear/editar/eliminar sedes.
 */
@Service
@RequiredArgsConstructor
public class SedeService {

    private final SedeRepository sedeRepository;

    @Transactional(readOnly = true)
    public List<SedeResponse> listar() {
        return sedeRepository.findAllByActivaTrueOrderByNombreAsc().stream()
                .map(SedeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SedeResponse> listarTodas() {
        return sedeRepository.findAll().stream()
                .map(SedeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SedeResponse obtenerPorId(Long id) {
        return SedeResponse.from(buscarSede(id));
    }

    @Transactional
    public SedeResponse crear(SedeRequest request) {
        if (sedeRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new IllegalArgumentException("Ya existe una sede con el nombre '" + request.nombre() + "'");
        }
        Sede sede = Sede.builder()
                .nombre(request.nombre().trim())
                .direccion(request.direccion())
                .telefono(request.telefono())
                .activa(request.activa() == null || request.activa())
                .build();
        return SedeResponse.from(sedeRepository.save(sede));
    }

    @Transactional
    public SedeResponse actualizar(Long id, SedeRequest request) {
        Sede sede = buscarSede(id);
        sede.setNombre(request.nombre().trim());
        sede.setDireccion(request.direccion());
        sede.setTelefono(request.telefono());
        if (request.activa() != null) sede.setActiva(request.activa());
        return SedeResponse.from(sedeRepository.save(sede));
    }

    @Transactional
    public void eliminar(Long id) {
        sedeRepository.delete(buscarSede(id));
    }

    private Sede buscarSede(Long id) {
        return sedeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sede " + id + " no encontrada"));
    }
}
