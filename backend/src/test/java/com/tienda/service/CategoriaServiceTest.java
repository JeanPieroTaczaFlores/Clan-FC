package com.tienda.service;

import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.*;
import com.tienda.repository.CategoriaRepository;
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
@DisplayName("CategoriaService — Unit Tests")
class CategoriaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = Categoria.builder()
                .idCategoria(1L).nombre("Audio").descripcion("Audífonos y bocinas").activa(true).build();
    }

    @Test
    @DisplayName("Listar categorías activas")
    void listarActivas() {
        when(categoriaRepository.findAllByActivaTrueOrderByNombreAsc())
                .thenReturn(List.of(categoria));

        var resultado = categoriaService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Audio");
    }

    @Test
    @DisplayName("Obtener categoría por ID")
    void obtenerPorId() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        var resultado = categoriaService.obtenerPorId(1L);

        assertThat(resultado.nombre()).isEqualTo("Audio");
    }

    @Test
    @DisplayName("Obtener categoría inexistente lanza excepción")
    void obtenerInexistente() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Crear categoría válida")
    void crearCategoria() {
        when(categoriaRepository.existsByNombreIgnoreCase("Gaming")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoria);

        var request = new com.tienda.dto.CategoriaRequest("Gaming", "Consolas y accesorios", true);
        var resultado = categoriaService.crear(request);

        assertThat(resultado).isNotNull();
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    @DisplayName("Crear categoría con nombre duplicado lanza excepción")
    void crearNombreDuplicado() {
        when(categoriaRepository.existsByNombreIgnoreCase("Audio")).thenReturn(true);

        var request = new com.tienda.dto.CategoriaRequest("Audio", "Duplicada", true);

        assertThatThrownBy(() -> categoriaService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya existe");
    }

    @Test
    @DisplayName("Eliminar categoría existente")
    void eliminarCategoria() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        categoriaService.eliminar(1L);

        verify(categoriaRepository).delete(categoria);
    }
}
