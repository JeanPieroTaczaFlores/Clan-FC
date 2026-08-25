package com.tienda.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Movimiento de caja: cada entrada/salida de efectivo queda registrada.
 * FONDOS_INICIALES = habilitación de caja, VENTA = cobro, RETIRO = retiro parcial.
 */
@Entity
@Table(name = "caja_movimientos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CajaMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long idMovimiento;

    /** Caja a la que pertenece el movimiento. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_caja", nullable = false)
    private Caja caja;

    /** Tipo de movimiento. */
    @Column(nullable = false, length = 20)
    private String tipo; // FONDOS_INICIALES, VENTA, RETIRO, AJUSTE

    /** Monto del movimiento (positivo = entrada, negativo = salida). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** Saldo después del movimiento. */
    @Column(name = "saldo_despues", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoDespues;

    /** Referencia (folio de orden, notas, etc.). */
    @Column(length = 100)
    private String referencia;

    /** Usuario que realizó el movimiento. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /** Sede del movimiento (denormalizado para consultas rápidas). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha = OffsetDateTime.now();
}
