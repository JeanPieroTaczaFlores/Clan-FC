package com.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Usuario del sistema. Su rol define las vistas y permisos:
 * CLIENTE (catálogo/checkout), CAJERO (POS) y ADMIN (gestión).
 */
@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    /** Rol asociado (FK a roles). Relación N:1. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @NotBlank
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** Hash BCrypt de la contraseña (nunca texto plano). */
    @NotBlank
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();
}
