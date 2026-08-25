package com.tienda.service;

import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.*;
import com.tienda.repository.UsuarioRepository;
import com.tienda.repository.RolRepository;
import com.tienda.repository.PaisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService — Unit Tests")
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PaisRepository paisRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private Rol rol;
    private Pais pais;

    @BeforeEach
    void setUp() {
        rol = Rol.builder().idRol(1L).nombre("ADMIN").build();
        pais = Pais.builder().idPais(3L).codigoIso2("PE").nombre("Perú").build();
        usuario = Usuario.builder()
                .idUsuario(1L).username("admin").email("admin@tienda.pe")
                .nombreCompleto("Admin Test").rol(rol).pais(pais).activo(true).build();
    }

    @Test
    @DisplayName("Listar usuarios")
    void listar() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        var resultado = usuarioService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Obtener usuario por ID")
    void obtenerPorId() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        var resultado = usuarioService.obtenerPorId(1L);

        assertThat(resultado.username()).isEqualTo("admin");
        assertThat(resultado.rol()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Obtener usuario inexistente lanza excepción")
    void obtenerInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Crear usuario con username duplicado lanza excepción")
    void crearUsernameDuplicado() {
        when(usuarioRepository.existsByUsernameIgnoreCase("admin")).thenReturn(true);

        var request = new com.tienda.dto.RegistroUsuarioRequest(
                "admin", "admin@test.pe", "pass123", "Admin Test", 3L);

        assertThatThrownBy(() -> usuarioService.crear(request, "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya existe");
    }

    @Test
    @DisplayName("Crear usuario con email duplicado lanza excepción")
    void crearEmailDuplicado() {
        when(usuarioRepository.existsByUsernameIgnoreCase("nuevo")).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("admin@tienda.pe")).thenReturn(true);

        var request = new com.tienda.dto.RegistroUsuarioRequest(
                "nuevo", "admin@tienda.pe", "pass123", "Nuevo", 3L);

        assertThatThrownBy(() -> usuarioService.crear(request, "CLIENTE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("Crear usuario válido")
    void crearUsuarioValido() {
        when(usuarioRepository.existsByUsernameIgnoreCase("nuevo")).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("nuevo@test.pe")).thenReturn(false);
        when(rolRepository.findByNombreIgnoreCase("CLIENTE")).thenReturn(Optional.of(rol));
        when(paisRepository.findById(3L)).thenReturn(Optional.of(pais));
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$10$hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        var request = new com.tienda.dto.RegistroUsuarioRequest(
                "nuevo", "nuevo@test.pe", "pass123", "Nuevo Usuario", 3L);

        var resultado = usuarioService.crear(request, "CLIENTE");

        assertThat(resultado).isNotNull();
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Rol inexistente lanza excepción")
    void rolInexistente() {
        when(usuarioRepository.existsByUsernameIgnoreCase("test")).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("test@test.pe")).thenReturn(false);
        when(rolRepository.findByNombreIgnoreCase("SUPERADMIN")).thenReturn(Optional.empty());

        var request = new com.tienda.dto.RegistroUsuarioRequest(
                "test", "test@test.pe", "pass", "Test", 3L);

        assertThatThrownBy(() -> usuarioService.crear(request, "SUPERADMIN"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
