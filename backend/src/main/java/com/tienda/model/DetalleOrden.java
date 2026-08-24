package com.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

/**
 * Línea de venta dentro de una orden.
 * sku/nombre_producto son SNAPSHOT: el comprobante histórico no se rompe
 * aunque el producto cambie de nombre o se elimine después.
 */
@Entity
@Table(name = "detalle_ordenes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    /** Orden a la que pertenece la línea (lado inverso de la relación). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden", nullable = false)
    private Orden orden;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @NotBlank
    @Column(nullable = false, length = 40)
    private String sku;

    @NotBlank
    @Column(name = "nombre_producto", nullable = false, length = 120)
    private String nombreProducto;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer cantidad;

    /** Precio base unitario congelado al momento de la venta (sin IVA). */
    @NotNull
    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    /** IVA calculado para toda la línea según la tasa aplicada. */
    @NotNull
    @Column(name = "iva_linea", nullable = false, precision = 12, scale = 2)
    private BigDecimal ivaLinea;

    /** precio_unitario * cantidad (sin IVA). */
    @NotNull
    @Column(name = "subtotal_linea", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalLinea;
}
