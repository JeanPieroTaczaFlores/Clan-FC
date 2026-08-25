package com.tienda.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Caja registradora de cada sede.
 * Cada sede tiene al menos una caja con su efectivo y estado.
 */
@Entity
@Table(name = "cajas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caja")
    private Long idCaja;

    /** Sede a la que pertenece esta caja. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    /** Número de caja dentro de la sede (1, 2, 3...). */
    @Column(name = "numero_caja", nullable = false)
    private Integer numeroCaja = 1;

    /** Saldo actual de efectivo en la caja. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal efectivo = BigDecimal.ZERO;

    /** Estado: ABIERTA, CERRADA, HABILITADA. */
    @Column(nullable = false, length = 15)
    private String estado = "CERRADA";

    /** Cajero asignado a esta caja (opcional, se asigna al habilitar). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();

    @Column(name = "fecha_apertura")
    private OffsetDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private OffsetDateTime fechaCierre;
}
