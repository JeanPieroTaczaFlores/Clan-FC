package com.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Rol de usuario del sistema (ADMIN, CAJERO, CLIENTE).
 * Spring Security lo traduce a "ROLE_<NOMBRE>" en tiempo de autenticación.
 */
@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long idRol;

    /** Nombre corto del rol: ADMIN | CAJERO | CLIENTE. */
    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String nombre;

    @Column(length = 200)
    private String descripcion;
}
