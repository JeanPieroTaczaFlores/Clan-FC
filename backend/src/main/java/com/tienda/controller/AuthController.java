package com.tienda.controller;

import com.tienda.dto.RegistroUsuarioRequest;
import com.tienda.dto.UsuarioResponse;
import com.tienda.model.Usuario;
import com.tienda.repository.UsuarioRepository;
import com.tienda.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Autenticación y REGISTRO.
 *
 *  GET  /api/auth/login    -> valida Basic Auth (Spring Security) y devuelve
 *                             { username, rol, país, bandera } para la sesión.
 *  POST /api/auth/registro -> AUTOREGISTRO público: crea cuenta CLIENTE con
 *                             país fiscal (define su IVA de consumidor final).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/login")
    public Map<String, Object> login(Authentication authentication) {
        // Extrae el rol sin el prefijo "ROLE_" que agrega Spring Security.
        String rol = authentication.getAuthorities().stream()
                .map(Object::toString)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("");

        // País del usuario para pintar banderas en la UI y resolver su IVA.
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        var pais = (usuario != null) ? usuario.getPais() : null;

        Map<String, Object> datos = new java.util.HashMap<>();
        datos.put("username", authentication.getName());
        datos.put("nombreCompleto", usuario != null ? usuario.getNombreCompleto() : null);
        datos.put("rol", rol);
        datos.put("paisCodigo", pais != null ? pais.getCodigoIso2() : null);
        datos.put("paisNombre", pais != null ? pais.getNombre() : null);
        datos.put("banderaEmoji",
                com.tienda.model.Pais.banderaDesde(pais != null ? pais.getCodigoIso2() : null));
        datos.put("mensaje", "Autenticación exitosa");
        return datos;
    }

    /** Registro PÚBLICO: no requiere sesión; asigna rol CLIENTE. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrarme(@Valid @RequestBody RegistroUsuarioRequest request) {
        return usuarioService.registrarse(request);
    }
}
