package com.tienda.service;

import com.tienda.dto.IncidenciaRequest;
import com.tienda.dto.IncidenciaResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.*;
import com.tienda.repository.IncidenciaRepository;
import com.tienda.repository.OrdenRepository;
import com.tienda.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * INCIDENCIAS — devoluciones, defectos y reclamos de garantía.
 * Al reportar una DEVOLUCION el producto vuelve al stock automáticamente
 * (movimiento kardex) salvo que esté dañado (DEFECTO/MERMA).
 */
@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final ProductoRepository productoRepository;
    private final OrdenRepository ordenRepository;
    private final AlmacenService almacenService;

    @Transactional
    public IncidenciaResponse crear(IncidenciaRequest request, Usuario reporta) {
        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + request.productoId() + " no encontrado"));

        TipoIncidencia tipo = normalizarTipo(request.tipo());

        Orden orden = null;
        if (request.ordenId() != null) {
            orden = ordenRepository.findById(request.ordenId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Orden " + request.ordenId() + " no encontrada"));
        }

        // Devolución en buen estado -> reingresa al stock como movimiento kardex.
        if (tipo == TipoIncidencia.DEVOLUCION) {
            almacenService.registrar(producto,
                    new com.tienda.dto.MovimientoAlmacenRequest(
                            producto.getIdProducto(), request.cantidad(), "DEVOLUCION",
                            null, orden != null ? orden.getFolio() : null,
                            "Devolución de cliente"),
                    reporta);
        }

        return IncidenciaResponse.from(incidenciaRepository.save(Incidencia.builder()
                .tipo(tipo)
                .estado(EstadoIncidencia.REPORTADA)
                .producto(producto)
                .orden(orden)
                .cantidad(request.cantidad())
                .descripcion(request.descripcion().trim())
                .reportadoPor(reporta)
                .build()));
    }

    @Transactional(readOnly = true)
    public List<IncidenciaResponse> listar() {
        return incidenciaRepository.findAllByOrderByFechaReporteDesc().stream()
                .map(IncidenciaResponse::from).toList();
    }

    /** Flujo: REPORTADA -> EN_REVISION -> RESUELTA/CANCELADA. */
    @Transactional
    public IncidenciaResponse cambiarEstado(Long id, String nuevoEstadoTexto, String resolucion, Usuario usuario) {
        Incidencia incidencia = incidenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Incidencia " + id + " no encontrada"));

        EstadoIncidencia estado;
        try {
            estado = EstadoIncidencia.valueOf(nuevoEstadoTexto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado inválido: " + nuevoEstadoTexto);
        }

        // RESUELTA de una GARANTIA/DEFECTO con merma implícita: saca del stock.
        if (estado == EstadoIncidencia.RESUELTA
                && incidencia.getTipo() != TipoIncidencia.DEVOLUCION
                && incidencia.getEstado() != EstadoIncidencia.RESUELTA) {
            try {
                almacenService.registrar(incidencia.getProducto(),
                        new com.tienda.dto.MovimientoAlmacenRequest(
                                incidencia.getProducto().getIdProducto(), incidencia.getCantidad(),
                                "MERMA", null, "INC-" + incidencia.getIdIncidencia(),
                                "Merma por " + incidencia.getTipo()),
                        usuario);
            } catch (IllegalArgumentException ignored) {
                // sin stock físico ya descontado; la resolución sigue válida
            }
        }

        incidencia.setEstado(estado);
        if (resolucion != null && !resolucion.isBlank()) {
            incidencia.setResolucion(resolucion.trim());
        }
        return IncidenciaResponse.from(incidenciaRepository.save(incidencia));
    }

    private TipoIncidencia normalizarTipo(String tipo) {
        try {
            return TipoIncidencia.valueOf(
                    (tipo == null ? "DEFECTO" : tipo).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de incidencia inválido: " + tipo);
        }
    }
}
