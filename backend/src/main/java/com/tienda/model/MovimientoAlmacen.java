package com.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Movimiento de almacén (kardex): cada entrada/salida de stock queda
 * registrada con quién la hizo y por qué — auditoría completa del inventario.
 */
@Entity
@Table(name = "movimientos_almacen")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoAlmacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long idMovimiento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoMovimiento tipo;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    /** Siempre positivo; el signo lo determina el tipo. */
    @NotNull
    @Column(nullable = false)
    private Integer cantidad;

    /** Snapshot del stock tras el movimiento (auditoría). */
    @NotNull
    @Column(name = "stock_resultante", nullable = false)
    private Integer stockResultante;

    /** Folio de orden / factura del proveedor / motivo. */
    @Column(length = 60)
    private String referencia;

    @Column(length = 255)
    private String nota;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;

    /** Usuario que registró el movimiento. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha = OffsetDateTime.now();
}
