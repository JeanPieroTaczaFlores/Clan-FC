package com.tienda.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de autenticación para la pantalla de Login del frontend.
 *
 * Flujo: el frontend envía Basic Auth con las credenciales escritas en el
 * formulario; Spring Security las valida (contra la tabla usuarios vía
 * UsuarioDetailsService y BCrypt) ANTES de ejecutar este método.
 *  - Credenciales correctas  -> 200 con { username, rol }
 *  - Credenciales incorrectas -> 401 automático (nunca llega aquí)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/login")
    public Map<String, Object> login(Authentication authentication) {
        // Extrae el rol sin el prefijo "ROLE_" que agrega Spring Security.
        String rol = authentication.getAuthorities().stream()
                .map(Object::toString)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("");

        return Map.of(
                "username", authentication.getName(),
                "rol", rol,
                "mensaje", "Autenticación exitosa");
    }
}
