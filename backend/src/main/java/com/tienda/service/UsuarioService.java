package com.tienda.service;

import com.tienda.dto.RegistroUsuarioRequest;
import com.tienda.dto.UsuarioResponse;
import com.tienda.exception.RecursoNoEncontradoException;
import com.tienda.model.Pais;
import com.tienda.model.Rol;
import com.tienda.model.Usuario;
import com.tienda.repository.PaisRepository;
import com.tienda.repository.RolRepository;
import com.tienda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestión de cuentas:
 *  - REGISTRO PÚBLICO: cualquiera crea su cuenta; el rol asignado siempre es
 *    CLIENTE y el país elegido define su IVA de consumidor final.
 *  - ALTA DE PERSONAL: solo ADMIN puede crear usuarios CAJERO/ADMIN/CLIENTE
 *    (endpoint /api/usuarios protegido por rol).
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PaisRepository paisRepository;
    private final PasswordEncoder passwordEncoder;

    /** Autoregistro público -> rol CLIENTE obligatorio. */
    @Transactional
    public UsuarioResponse registrarse(RegistroUsuarioRequest request) {
        return crear(request.username(), request.email(), request.password(),
                request.nombreCompleto(), "CLIENTE", request.idPais());
    }

    /** Alta de usuario por ADMIN (permite roles internos). */
    @Transactional
    public UsuarioResponse crearPorAdmin(RegistroUsuarioRequest request, String rolNombre) {
        return crear(request.username(), request.email(), request.password(),
                request.nombreCompleto(), rolNombre, request.idPais());
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    /* ------------------------------ Privados ------------------------------ */

    private UsuarioResponse crear(String username, String email, String passwordPlano,
                                  String nombreCompleto, String rolNombre, Long idPais) {
        // Unicidad amigable (las columnas UNIQUE son la última defensa).
        if (usuarioRepository.existsByUsernameIgnoreCase(username.trim()))
            throw new DataIntegrityViolationException("El nombre de usuario ya está en uso");
        if (usuarioRepository.existsByEmailIgnoreCase(email.trim()))
            throw new DataIntegrityViolationException("El correo ya está registrado");

        Rol rol = rolRepository.findByNombreIgnoreCase(rolNombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado: " + rolNombre));

        Pais pais = paisRepository.findById(idPais)
                .filter(Pais::getActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("País no encontrado (id=" + idPais + ")"));

        Usuario usuario = Usuario.builder()
                .rol(rol)
                .username(username.trim())
                .email(email.trim().toLowerCase())
                // BCrypt con salt aleatoria: nunca se guarda texto plano.
                .passwordHash(passwordEncoder.encode(passwordPlano))
                .nombreCompleto(nombreCompleto.trim())
                .pais(pais)
                .activo(Boolean.TRUE)
                .build();

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }
}
