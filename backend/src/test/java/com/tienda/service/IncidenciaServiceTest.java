package com.tienda.service;

import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.*;
import com.tienda.repository.IncidenciaRepository;
import com.tienda.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncidenciaService — Unit Tests")
class IncidenciaServiceTest {

    @Mock private IncidenciaRepository incidenciaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private AlmacenService almacenService;

    @InjectMocks
    private IncidenciaService incidenciaService;

    private Incidencia incidencia;
    private Producto producto;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        producto = Producto.builder().idProducto(1L).sku("AUD-001").nombre("Audífonos").stock(28).build();
        usuario = Usuario.builder().idUsuario(1L).username("cajero1").build();
        incidencia = Incidencia.builder()
                .idIncidencia(1L)
                .tipo(TipoIncidencia.DEFECTO)
                .estado(EstadoIncidencia.REPORTADA)
                .producto(producto)
                .cantidad(2)
                .descripcion("Audífonos con defecto de fábrica")
                .reportadoPor(usuario)
                .build();
    }

    @Test
    @DisplayName("Listar incidencias")
    void listarIncidencias() {
        when(incidenciaRepository.findAllByOrderByFechaReporteDesc())
                .thenReturn(List.of(incidencia));

        List<Incidencia> resultado = incidenciaService.listar();

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("Obtener incidencia por ID")
    void obtenerPorId() {
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));

        Incidencia resultado = incidenciaService.obtenerPorId(1L);

        assertThat(resultado.getTipo()).isEqualTo(TipoIncidencia.DEFECTO);
    }

    @Test
    @DisplayName("Obtener incidencia inexistente lanza excepción")
    void obtenerInexistente() {
        when(incidenciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidenciaService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Cambiar estado de REPORTADA a EN_REVISION")
    void cambiarEstado() {
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));
        when(incidenciaRepository.save(any())).thenReturn(incidencia);

        Incidencia resultado = incidenciaService.cambiarEstado(1L, "EN_REVISION", "Revisión inicial");

        assertThat(resultado.getEstado()).isEqualTo(EstadoIncidencia.EN_REVISION);
    }

    @Test
    @DisplayName("Resolver incidencia DEFECTO crea merma")
    void resolverDefectoCreaMerma() {
        incidencia.setEstado(EstadoIncidencia.EN_REVISION);
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));
        when(incidenciaRepository.save(any())).thenReturn(incidencia);

        Incidencia resultado = incidenciaService.cambiarEstado(1L, "RESUELTA", "Producto reemplazado");

        assertThat(resultado.getEstado()).isEqualTo(EstadoIncidencia.RESUELTA);
        verify(almacenService).registrarSalidaVenta(1L, 2, "MERMA",
                "MERMA por incidencia #1 - DEFECTO", 1L);
    }

    @Test
    @DisplayName("Transición de estado inválida lanza excepción")
    void transicionInvalida() {
        incidencia.setEstado(EstadoIncidencia.RESUELTA);
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));

        assertThatThrownBy(() -> incidenciaService.cambiarEstado(1L, "REPORTADA", "Volver"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no permite");
    }

    @Test
    @DisplayName("Crear incidencia DEVOLUCION")
    void crearDevolucion() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(incidenciaRepository.save(any())).thenReturn(incidencia);

        Incidencia resultado = incidenciaService.crear(1L, null, "DEVOLUCION", 1,
                "Cliente devolvió producto", 1L);

        assertThat(resultado).isNotNull();
        verify(incidenciaRepository).save(any(Incidencia.class));
    }
}
