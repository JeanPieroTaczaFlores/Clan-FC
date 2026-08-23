package com.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Producto del inventario.
 * precio_base es SIN impuestos: el IVA se calcula según el régimen fiscal
 * del comprador (ver TaxCalculationService en Entregable 2).
 */
@Entity
@Table(name = "productos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long idProducto;

    /** Categoría a la que pertenece (FK, obligatoria). */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    /** Código único de inventario (ej. ELEC-001). */
    @NotBlank
    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    /** Precio sin IVA (>= 0). */
    @NotNull
    @DecimalMin("0.00")
    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    /** Unidades disponibles; nunca negativo (restricción CHECK en BD). */
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer stock = 0;

    /** Umbral para alertas de stock bajo (dashboard Entregable 3). */
    @NotNull
    @PositiveOrZero
    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 5;

    @Column(name = "imagen_url", length = 300)
    private String imagenUrl;

    /** Baja lógica: los productos inactivos no aparecen en el catálogo. */
    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion = OffsetDateTime.now();

    @PreUpdate
    void alActualizar() {
        this.fechaActualizacion = OffsetDateTime.now();
    }
}
