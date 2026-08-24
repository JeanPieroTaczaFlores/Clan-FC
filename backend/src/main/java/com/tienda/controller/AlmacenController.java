package com.tienda.controller;

import com.tienda.dto.MovimientoAlmacenRequest;
import com.tienda.dto.MovimientoResponse;
import com.tienda.dto.ReporteSemanalResponse;
import com.tienda.model.Producto;
import com.tienda.model.Usuario;
import com.tienda.repository.ProductoRepository;
import com.tienda.repository.UsuarioRepository;
import com.tienda.service.AlmacenService;
import com.tienda.service.ReporteSemanalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ALMACÉN + REPORTES del cajero:
 *  POST /api/almacen/movimientos -> entrada/merma/devolución de stock (kardex)
 *  GET  /api/almacen/movimientos -> kardex reciente
 *  GET  /api/reportes/semanal    -> ventas 7 días, top productos, bajo stock
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AlmacenController {

    private final AlmacenService almacenService;
    private final ReporteSemanalService reporteService;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/almacen/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse registrar(@Valid @RequestBody MovimientoAlmacenRequest request,
                                        Authentication authentication) {
        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> new com.tienda.exception.RecursoNoEncontradoException(
                        "Producto " + request.productoId() + " no encontrado"));

        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "Usuario no encontrado"));

        return almacenService.registrar(producto, request, usuario);
    }

    @GetMapping("/almacen/movimientos")
    public List<MovimientoResponse> kardex() {
        return almacenService.listarRecientes();
    }

    @GetMapping("/reportes/semanal")
    public ReporteSemanalResponse reporteSemanal() {
        return reporteService.generar();
    }
}
