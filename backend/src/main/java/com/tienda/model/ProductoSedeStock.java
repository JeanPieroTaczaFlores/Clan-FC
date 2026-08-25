package com.tienda.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Stock de un producto en una sede específica.
 * Permite manejar inventario diferenciado por sucursal.
 */
@Entity
@Table(name = "producto_sede_stock", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_producto", "id_sede"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductoSedeStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto_sede_stock")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    /** Stock actual de este producto en esta sede. */
    @Column(nullable = false)
    private Integer stock = 0;

    /** Stock mínimo para alertas en esta sede. */
    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 5;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion = OffsetDateTime.now();

    @PreUpdate
    void alActualizar() {
        this.fechaActualizacion = OffsetDateTime.now();
    }
}
