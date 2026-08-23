package com.tienda.service;

import com.tienda.dto.CategoriaRequest;
import com.tienda.dto.CategoriaResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.Categoria;
import com.tienda.repository.CategoriaRepository;
import com.tienda.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lógica de negocio para categorías.
 * Reglas: nombre único (case-insensitive) y borrado físico solo si no
 * tiene productos; en caso contrario se desactiva (borrado lógico).
 */
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarActivas() {
        return categoriaRepository.findAllByActivaTrueOrderByNombreAsc()
                .stream().map(CategoriaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodas() {
        return categoriaRepository.findAll().stream()
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .map(CategoriaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtenerPorId(Long id) {
        return CategoriaResponse.from(buscarCategoria(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        if (categoriaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + request.nombre());
        }
        Categoria categoria = Categoria.builder()
                .nombre(request.nombre().trim())
                .descripcion(request.descripcion())
                .activa(request.activa() == null || request.activa())
                .build();
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarCategoria(id);
        categoria.setNombre(request.nombre().trim());
        categoria.setDescripcion(request.descripcion());
        if (request.activa() != null) categoria.setActiva(request.activa());
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    /**
     * Elimina si no tiene productos; si tiene, lanza conflicto para forzar
     * el uso de desactivación (evita huérfanos en inventario).
     */
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarCategoria(id);
        if (!productoRepository.buscarInventario(null, id).isEmpty()) {
            throw new IllegalArgumentException(
                    "La categoría tiene productos asociados; desactívela en lugar de eliminarla");
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría " + id + " no encontrada"));
    }
}
