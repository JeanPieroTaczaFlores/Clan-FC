package com.tienda.config;

import com.tienda.security.UsuarioDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración base de seguridad (Entregable 1).
 *
 * Modelo de autorización:
 *  - Lecturas del catálogo (GET productos/categorías): público.
 *  - Escrituras de inventario: solo ROLE_ADMIN.
 *  - Resto de endpoints futuros (/api/ordenes, /api/dashboard): autenticados.
 *
 * Autenticación HTTP Basic sin estado (stateless); en Entregables posteriores
 * puede migrarse a JWT manteniendo las mismas reglas de roles.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize por método si se necesita afinar
@RequiredArgsConstructor
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API REST stateless consumida por el frontend con fetch(): no usamos cookies de sesión ni CSRF token.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- Catálogo público (vista CLIENTE) ---
                        .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**").permitAll()
                        // --- Gestión de inventario (vista ADMIN) ---
                        .requestMatchers("/api/productos/**", "/api/categorias/**").hasRole("ADMIN")
                        // --- Endpoints de entregables siguientes ---
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/pos/**").hasAnyRole("CAJERO", "ADMIN")
                        // --- Todo lo demás requiere sesión ---
                        .anyRequest().authenticated())
                // Basic Auth temporal; valida contra la tabla usuarios vía UsuarioDetailsService.
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /** BCrypt para hashear y verificar contraseñas de usuarios. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS: permite que Live Server (VS Code, puertos 5500-5501) llame a la API.
     * Sin esto, el navegador bloquearía fetch() cross-origin.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://127.0.0.1:5500", "http://localhost:5500",
                "http://127.0.0.1:5501", "http://localhost:5501"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Basic Auth viaja en el header Authorization.
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
