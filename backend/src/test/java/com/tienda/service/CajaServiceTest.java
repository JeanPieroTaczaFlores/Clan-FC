package com.tienda.service;

import com.tienda.dto.CajaMovimientoRequest;
import com.tienda.dto.CajaMovimientoResponse;
import com.tienda.dto.CajaRequest;
import com.tienda.dto.CajaResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.exception.StockInsuficienteException;
import com.tienda.model.Caja;
import com.tienda.model.Sede;
import com.tienda.model.Usuario;
import com.tienda.model.Rol;
import com.tienda.repository.CajaMovimientoRepository;
import com.tienda.repository.CajaRepository;
import com.tienda.repository.SedeRepository;
import com.tienda.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CajaService — Unit Tests")
class CajaServiceTest {

    @Mock private CajaRepository cajaRepository;
    @Mock private CajaMovimientoRepository cajaMovimientoRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CajaService cajaService;

    private Sede sede;
    private Caja caja;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        sede = Sede.builder().idSede(1L).nombre("Villa El Salvador").activa(true).build();
        Rol rol = Rol.builder().idRol(2L).nombre("CAJERO").build();
        usuario = Usuario.builder().idUsuario(1L).username("cajero1").rol(rol).activo(true).build();
        caja = Caja.builder()
                .idCaja(1L).sede(sede).numeroCaja(1)
                .efectivo(BigDecimal.valueOf(1500)).estado("CERRADA").build();
    }

    @Test
    @DisplayName("Listar cajas por sede")
    void listarCajasPorSede() {
        when(cajaRepository.findAllBySedeIdSedeOrderByNumeroCajaAsc(1L))
                .thenReturn(List.of(caja));

        List<CajaResponse> resultado = cajaService.listarPorSede(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).numeroCaja()).isEqualTo(1);
    }

    @Test
    @DisplayName("Obtener caja abierta de sede")
    void obtenerCajaAbierta() {
        Caja cajaAbierta = Caja.builder()
                .idCaja(2L).sede(sede).numeroCaja(1)
                .efectivo(BigDecimal.ZERO).estado("ABIERTA").build();
        when(cajaRepository.findBySedeIdSedeAndEstado(1L, "ABIERTA"))
                .thenReturn(Optional.of(cajaAbierta));

        CajaResponse resultado = cajaService.obtenerCajaAbierta(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.estado()).isEqualTo("ABIERTA");
    }

    @Test
    @DisplayName("Crear caja válida")
    void crearCaja() {
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(sede));
        when(cajaRepository.findBySedeIdSedeAndNumeroCaja(1L, 1)).thenReturn(Optional.empty());
        when(cajaRepository.save(any(Caja.class))).thenReturn(caja);

        CajaRequest request = new CajaRequest(1L, 1, BigDecimal.valueOf(1000), "CERRADA", null);
        CajaResponse respuesta = cajaService.crear(request);

        assertThat(respuesta).isNotNull();
        verify(cajaRepository).save(any(Caja.class));
    }

    @Test
    @DisplayName("Crear caja duplicada lanza excepción")
    void crearCajaDuplicada() {
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(sede));
        when(cajaRepository.findBySedeIdSedeAndNumeroCaja(1L, 1)).thenReturn(Optional.of(caja));

        CajaRequest request = new CajaRequest(1L, 1, BigDecimal.ZERO, "CERRADA", null);

        assertThatThrownBy(() -> cajaService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya existe");
    }

    @Test
    @DisplayName("Habilitar caja con fondos iniciales")
    void habilitarCaja() {
        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(cajaRepository.save(any(Caja.class))).thenReturn(caja);

        CajaResponse resultado = cajaService.habilitar(1L, 1L, BigDecimal.valueOf(500));

        assertThat(resultado).isNotNull();
        verify(cajaRepository, times(2)).save(any(Caja.class));
        verify(cajaMovimientoRepository).save(any());
    }

    @Test
    @DisplayName("Habilitar caja ya abierta lanza excepción")
    void habilitarCajaYaAbierta() {
        caja.setEstado("ABIERTA");
        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));

        assertThatThrownBy(() -> cajaService.habilitar(1L, 1L, BigDecimal.valueOf(500)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está habilitada");
    }

    @Test
    @DisplayName("Cerrar caja abierta")
    void cerrarCaja() {
        caja.setEstado("ABIERTA");
        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(cajaRepository.save(any(Caja.class))).thenReturn(caja);

        CajaResponse resultado = cajaService.cerrar(1L);

        assertThat(resultado).isNotNull();
        verify(cajaRepository).save(any(Caja.class));
    }

    @Test
    @DisplayName("Cerrar caja ya cerrada lanza excepción")
    void cerrarCajaYaCerrada() {
        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));

        assertThatThrownBy(() -> cajaService.cerrar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está cerrada");
    }

    @Test
    @DisplayName("Registrar movimiento de venta")
    void registrarMovimientoVenta() {
        caja.setEstado("ABIERTA");
        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(cajaRepository.save(any(Caja.class))).thenReturn(caja);
        when(cajaMovimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CajaMovimientoRequest request = new CajaMovimientoRequest(1L, "VENTA", BigDecimal.valueOf(250), "ORD-001");
        CajaMovimientoResponse resultado = cajaService.registrarMovimiento(request, 1L);

        assertThat(resultado.tipo()).isEqualTo("VENTA");
        assertThat(resultado.monto()).isEqualByComparingTo(BigDecimal.valueOf(250));
    }

    @Test
    @DisplayName("Registrar retiro con fondos insuficientes lanza excepción")
    void retiroFondosInsuficientes() {
        caja.setEstado("ABIERTA");
        caja.setEfectivo(BigDecimal.valueOf(100));
        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        CajaMovimientoRequest request = new CajaMovimientoRequest(1L, "RETIRO", BigDecimal.valueOf(500), "Retiro grande");

        assertThatThrownBy(() -> cajaService.registrarMovimiento(request, 1L))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("insuficientes");
    }

    @Test
    @DisplayName("Registrar retiro válido")
    void retiroValido() {
        caja.setEstado("ABIERTA");
        caja.setEfectivo(BigDecimal.valueOf(1000));
        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(cajaRepository.save(any(Caja.class))).thenReturn(caja);
        when(cajaMovimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CajaMovimientoRequest request = new CajaMovimientoRequest(1L, "RETIRO", BigDecimal.valueOf(300), "Retiro parcial");
        CajaMovimientoResponse resultado = cajaService.registrarMovimiento(request, 1L);

        assertThat(resultado.tipo()).isEqualTo("RETIRO");
    }

    @Test
    @DisplayName("Movimiento en caja cerrada lanza excepción")
    void movimientoCajaCerrada() {
        when(cajaRepository.findById(1L)).thenReturn(Optional.of(caja)); // estado = CERRADA
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        CajaMovimientoRequest request = new CajaMovimientoRequest(1L, "VENTA", BigDecimal.valueOf(100), null);

        assertThatThrownBy(() -> cajaService.registrarMovimiento(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no está habilitada");
    }
}
