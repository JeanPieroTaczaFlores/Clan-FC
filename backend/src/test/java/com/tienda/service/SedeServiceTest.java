package com.tienda.service;

import com.tienda.dto.SedeRequest;
import com.tienda.dto.SedeResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.Sede;
import com.tienda.repository.SedeRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SedeService — Unit Tests")
class SedeServiceTest {

    @Mock
    private SedeRepository sedeRepository;

    @InjectMocks
    private SedeService sedeService;

    private Sede sedeEjemplo;

    @BeforeEach
    void setUp() {
        sedeEjemplo = Sede.builder()
                .idSede(1L)
                .nombre("Villa El Salvador")
                .direccion("Av. Los Héroes 200")
                .telefono("01-555-0001")
                .activa(true)
                .build();
    }

    @Test
    @DisplayName("Listar sedes activas")
    void listarSedesActivas() {
        when(sedeRepository.findAllByActivaTrueOrderByNombreAsc())
                .thenReturn(List.of(sedeEjemplo));

        List<SedeResponse> resultado = sedeService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Villa El Salvador");
        verify(sedeRepository).findAllByActivaTrueOrderByNombreAsc();
    }

    @Test
    @DisplayName("Listar todas las sedes incluye inactivas")
    void listarTodasLasSedes() {
        Sede sedeInactiva = Sede.builder()
                .idSede(2L).nombre("Chorrillos").activa(false).build();
        when(sedeRepository.findAll()).thenReturn(List.of(sedeEjemplo, sedeInactiva));

        List<SedeResponse> resultado = sedeService.listarTodas();

        assertThat(resultado).hasSize(2);
        verify(sedeRepository).findAll();
    }

    @Test
    @DisplayName("Obtener sede por ID existente")
    void obtenerSedePorId() {
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(sedeEjemplo));

        SedeResponse respuesta = sedeService.obtenerPorId(1L);

        assertThat(respuesta.nombre()).isEqualTo("Villa El Salvador");
        assertThat(respuesta.direccion()).isEqualTo("Av. Los Héroes 200");
    }

    @Test
    @DisplayName("Obtener sede inexistente lanza excepción")
    void obtenerSedeInexistente() {
        when(sedeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sedeService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Crear sede válida")
    void crearSedeValida() {
        when(sedeRepository.existsByNombreIgnoreCase("Nueva Sede")).thenReturn(false);
        when(sedeRepository.save(any(Sede.class))).thenReturn(sedeEjemplo);

        SedeRequest request = new SedeRequest("Nueva Sede", "Av. Principal", "01-555-9999", true);
        SedeResponse respuesta = sedeService.crear(request);

        assertThat(respuesta).isNotNull();
        verify(sedeRepository).save(any(Sede.class));
    }

    @Test
    @DisplayName("Crear sede con nombre duplicado lanza excepción")
    void crearSedeNombreDuplicado() {
        when(sedeRepository.existsByNombreIgnoreCase("Villa El Salvador")).thenReturn(true);

        SedeRequest request = new SedeRequest("Villa El Salvador", "Otra dirección", null, true);

        assertThatThrownBy(() -> sedeService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya existe");
    }

    @Test
    @DisplayName("Actualizar sede existente")
    void actualizarSede() {
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(sedeEjemplo));
        when(sedeRepository.save(any(Sede.class))).thenReturn(sedeEjemplo);

        SedeRequest request = new SedeRequest("V.E.S. Actualizado", "Nueva dirección", "01-555-1111", true);
        SedeResponse respuesta = sedeService.actualizar(1L, request);

        assertThat(respuesta.nombre()).isEqualTo("V.E.S. Actualizado");
        verify(sedeRepository).save(any(Sede.class));
    }

    @Test
    @DisplayName("Eliminar sede existente")
    void eliminarSede() {
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(sedeEjemplo));

        sedeService.eliminar(1L);

        verify(sedeRepository).delete(sedeEjemplo);
    }

    @Test
    @DisplayName("Eliminar sede inexistente lanza excepción")
    void eliminarSedeInexistente() {
        when(sedeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sedeService.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
