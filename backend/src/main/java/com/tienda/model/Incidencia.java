package com.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Incidencia de producto: devolución de cliente, defecto detectado o
 * reclamo de garantía. Se reporta desde el POS y se da seguimiento hasta
 * su resolución.
 */
@Entity
@Table(name = "incidencias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incidencia")
    private Long idIncidencia;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoIncidencia tipo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoIncidencia estado = EstadoIncidencia.REPORTADA;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    /** Orden de la que provino la pieza (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden")
    private Orden orden;

    @NotNull
    @Column(nullable = false)
    private Integer cantidad = 1;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reportado_por", nullable = false)
    private Usuario reportadoPor;

    @Column(name = "fecha_reporte", nullable = false, updatable = false)
    private OffsetDateTime fechaReporte = OffsetDateTime.now();

    /** Qué se hizo al resolverla (cambio, reembolso, reparación...). */
    @Column(length = 500)
    private String resolucion;
}
