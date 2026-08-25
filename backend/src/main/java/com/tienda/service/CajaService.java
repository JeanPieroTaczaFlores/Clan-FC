package com.tienda.service;

import com.tienda.dto.CajaMovimientoRequest;
import com.tienda.dto.CajaMovimientoResponse;
import com.tienda.dto.CajaRequest;
import com.tienda.dto.CajaResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.exception.StockInsuficienteException;
import com.tienda.model.Caja;
import com.tienda.model.CajaMovimiento;
import com.tienda.model.Sede;
import com.tienda.model.Usuario;
import com.tienda.repository.CajaMovimientoRepository;
import com.tienda.repository.CajaRepository;
import com.tienda.repository.SedeRepository;
import com.tienda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Gestión de cajas y movimientos de caja por sede.
 * Cada sede tiene al menos una caja que puede habilitarse/cerrarse.
 */
@Service
@RequiredArgsConstructor
public class CajaService {

    private final CajaRepository cajaRepository;
    private final CajaMovimientoRepository cajaMovimientoRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;

    /* ----------------------------- CAJAS ------------------------------- */

    @Transactional(readOnly = true)
    public List<CajaResponse> listarPorSede(Long idSede) {
        return cajaRepository.findAllBySedeIdSedeOrderByNumeroCajaAsc(idSede).stream()
                .map(CajaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CajaResponse obtenerPorId(Long id) {
        return CajaResponse.from(buscarCaja(id));
    }

    @Transactional(readOnly = true)
    public CajaResponse obtenerCajaAbierta(Long idSede) {
        Caja caja = cajaRepository.findBySedeIdSedeAndEstado(idSede, "ABIERTA")
                .orElse(null);
        return caja != null ? CajaResponse.from(caja) : null;
    }

    @Transactional
    public CajaResponse crear(CajaRequest request) {
        Sede sede = sedeRepository.findById(request.idSede())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sede " + request.idSede() + " no encontrada"));

        Integer numCaja = request.numeroCaja() != null ? request.numeroCaja() : 1;

        if (cajaRepository.findBySedeIdSedeAndNumeroCaja(request.idSede(), numCaja).isPresent()) {
            throw new IllegalArgumentException("Ya existe la caja " + numCaja + " en la sede " + sede.getNombre());
        }

        Usuario usuario = null;
        if (request.idUsuario() != null) {
            usuario = usuarioRepository.findById(request.idUsuario())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + request.idUsuario() + " no encontrado"));
        }

        Caja caja = Caja.builder()
                .sede(sede)
                .numeroCaja(numCaja)
                .efectivo(request.efectivo() != null ? request.efectivo() : BigDecimal.ZERO)
                .estado(request.estado() != null ? request.estado() : "CERRADA")
                .usuario(usuario)
                .build();
        return CajaResponse.from(cajaRepository.save(caja));
    }

    @Transactional
    public CajaResponse habilitar(Long idCaja, Long idUsuario, BigDecimal fondosIniciales) {
        Caja caja = buscarCaja(idCaja);

        if ("ABIERTA".equals(caja.getEstado())) {
            throw new IllegalArgumentException("La caja ya está habilitada");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + idUsuario + " no encontrado"));

        caja.setUsuario(usuario);
        caja.setEfectivo(fondosIniciales);
        caja.setEstado("ABIERTA");
        caja.setFechaApertura(OffsetDateTime.now());
        caja.setFechaCierre(null);

        Caja cajaGuardada = cajaRepository.save(caja);

        // Registrar movimiento de fondos iniciales
        CajaMovimiento movimiento = CajaMovimiento.builder()
                .caja(cajaGuardada)
                .tipo("FONDOS_INICIALES")
                .monto(fondosIniciales)
                .saldoDespues(fondosIniciales)
                .referencia("Habilitación de caja")
                .usuario(usuario)
                .sede(cajaGuardada.getSede())
                .build();
        cajaMovimientoRepository.save(movimiento);

        return CajaResponse.from(cajaGuardada);
    }

    @Transactional
    public CajaResponse cerrar(Long idCaja) {
        Caja caja = buscarCaja(idCaja);

        if ("CERRADA".equals(caja.getEstado())) {
            throw new IllegalArgumentException("La caja ya está cerrada");
        }

        caja.setEstado("CERRADA");
        caja.setFechaCierre(OffsetDateTime.now());
        return CajaResponse.from(cajaRepository.save(caja));
    }

    /* ------------------------- MOVIMIENTOS ---------------------------- */

    @Transactional(readOnly = true)
    public List<CajaMovimientoResponse> listarMovimientosPorCaja(Long idCaja) {
        return cajaMovimientoRepository.findAllByCajaIdCajaOrderByFechaDesc(idCaja).stream()
                .map(CajaMovimientoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CajaMovimientoResponse> listarMovimientosPorSede(Long idSede) {
        return cajaMovimientoRepository.findAllBySedeIdSedeOrderByFechaDesc(idSede).stream()
                .map(CajaMovimientoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CajaMovimientoResponse> listarTodosMovimientos() {
        return cajaMovimientoRepository.findAllByOrderByFechaDesc().stream()
                .limit(200)
                .map(CajaMovimientoResponse::from)
                .toList();
    }

    @Transactional
    public CajaMovimientoResponse registrarMovimiento(CajaMovimientoRequest request, Long idUsuario) {
        Caja caja = buscarCaja(request.idCaja());

        if (!"ABIERTA".equals(caja.getEstado())) {
            throw new IllegalArgumentException("La caja no está habilitada para registrar movimientos");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + idUsuario + " no encontrado"));

        BigDecimal monto = request.monto();
        String tipo = request.tipo().toUpperCase();

        // Calcular nuevo saldo
        BigDecimal nuevoSaldo;
        switch (tipo) {
            case "FONDOS_INICIALES", "VENTA", "AJUSTE" -> {
                nuevoSaldo = caja.getEfectivo().add(monto);
            }
            case "RETIRO" -> {
                if (caja.getEfectivo().compareTo(monto) < 0) {
                    throw new StockInsuficienteException("Fondos insuficientes en caja. Disponible: S/ " + caja.getEfectivo());
                }
                nuevoSaldo = caja.getEfectivo().subtract(monto);
            }
            default -> throw new IllegalArgumentException("Tipo de movimiento no válido: " + tipo);
        }

        caja.setEfectivo(nuevoSaldo);
        cajaRepository.save(caja);

        CajaMovimiento movimiento = CajaMovimiento.builder()
                .caja(caja)
                .tipo(tipo)
                .monto(monto)
                .saldoDespues(nuevoSaldo)
                .referencia(request.referencia())
                .usuario(usuario)
                .sede(caja.getSede())
                .build();

        return CajaMovimientoResponse.from(cajaMovimientoRepository.save(movimiento));
    }

    private Caja buscarCaja(Long id) {
        return cajaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Caja " + id + " no encontrada"));
    }
}
