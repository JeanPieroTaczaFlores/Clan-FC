package com.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.OffsetDateTime;

/** Proveedor: quién surte la mercancía electrónica a la tienda. */
@Entity
@Table(name = "proveedores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Long idProveedor;

    @NotBlank
    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(name = "contacto_nombre", length = 120)
    private String contactoNombre;

    @Column(length = 30)
    private String telefono;

    @Email
    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private OffsetDateTime fechaRegistro = OffsetDateTime.now();
}
