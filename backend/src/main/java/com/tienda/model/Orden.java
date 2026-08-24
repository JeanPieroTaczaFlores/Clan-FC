package com.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera de una venta (web o de mostrador).
 * Congela el régimen fiscal y la tasa de IVA aplicados en el momento de la
 * compra: si mañana cambia la parametrización, los comprobantes históricos
 * conservan sus cifras originales.
 */
@Entity
@Table(name = "ordenes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Long idOrden;

    /** Folio legible único para comprobantes (ORD-yyyyMMdd-HHmmss-XXX). */
    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String folio;

    /** WEB (checkout cliente) o CAJA (cobro del cajero). */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CanalVenta canal;

    /** Usuario autenticado que compró (WEB) o cobró (CAJA). */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /**
     * Empresa cliente B2B asociada a la compra.
     * NULL = consumidor final web (aplica tasa GENERAL del país por defecto).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_empresa_cliente")
    private EmpresaCliente empresaCliente;

    /** País fiscal de la venta (para reportes por país). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pais")
    private Pais pais;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "regimen_fiscal", nullable = false, length = 20)
    private RegimenFiscal regimenFiscal;

    @NotNull
    @Column(name = "tasa_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal tasaIva;

    /** Suma de precios base (sin IVA). */
    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal iva;

    /** subtotal + iva. */
    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @NotNull
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago = "EFECTIVO";

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoOrden estado = EstadoOrden.PAGADA;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();

    /** Líneas de la venta; se persisten en cascada con la orden. */
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleOrden> detalles = new ArrayList<>();

    public void addItem(DetalleOrden detalle) {
        detalles.add(detalle);
        detalle.setOrden(this);
    }
}
