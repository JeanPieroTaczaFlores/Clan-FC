package com.tienda.bootstrap;

import com.tienda.model.Rol;
import com.tienda.model.Usuario;
import com.tienda.repository.RolRepository;
import com.tienda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra inicial de datos de seguridad al arrancar la aplicación.
 *
 * ¿Por qué aquí y no en schema.sql? Las contraseñas deben guardarse como
 * hash BCrypt generado por el PasswordEncoder de Spring; sembrarlas desde
 * Java garantiza hashes válidos y consistentes.
 *
 * Credenciales demo (solo desarrollo):
 *   admin / admin123 · cajero / cajero123 · cliente / cliente123
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Rol admin   = crearRolSiNoExiste("ADMIN",   "Administrador: gestión total, inventario y usuarios");
        Rol cajero  = crearRolSiNoExiste("CAJERO",  "Cajero: punto de venta y emisión de comprobantes");
        Rol cliente = crearRolSiNoExiste("CLIENTE", "Cliente: compra web, carrito y checkout");

        crearUsuarioSiNoExiste("admin",   "admin123",   "admin@tiendamenos.mx",   "Laura Directora",    admin);
        crearUsuarioSiNoExiste("cajero",  "cajero123",  "cajero@tiendamenos.mx",  "Carlos Punto Venta", cajero);
        crearUsuarioSiNoExiste("cliente", "cliente123", "cliente@correo.mx",      "María Compradora",   cliente);
    }

    private Rol crearRolSiNoExiste(String nombre, String descripcion) {
        return rolRepository.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> rolRepository.save(Rol.builder()
                        .nombre(nombre).descripcion(descripcion).build()));
    }

    private void crearUsuarioSiNoExiste(String username, String passwordPlano,
                                        String email, String nombreCompleto, Rol rol) {
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) return;

        usuarioRepository.save(Usuario.builder()
                .username(username)
                // BCrypt genera salt aleatorio por usuario; nunca guardar texto plano.
                .passwordHash(passwordEncoder.encode(passwordPlano))
                .email(email)
                .nombreCompleto(nombreCompleto)
                .rol(rol)
                .activo(true)
                .build());
        log.info("DataLoader: usuario '{}' creado con rol {}", username, rol.getNombre());
    }
}
