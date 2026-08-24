package com.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Empresa cliente (B2B). Su país + régimen fiscal determinan el IVA aplicable
 * en sus compras — base del TaxCalculationService (Entregable 2).
 */
@Entity
@Table(name = "empresas_clientes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmpresaCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Long idEmpresa;

    @NotBlank
    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    /** Identificador fiscal único: RFC (MX), NIT (CO), RUC (PE)... */
    @NotBlank
    @Column(nullable = false, unique = true, length = 13)
    private String rfc;

    /**
     * País fiscal: sus tasas definen el IVA según el régimen.
     * Precedencia de resolución en TaxCalculationService:
     *   1) pais.tasaIvaGeneral/Reducido (dinámico)
     *   2) tasaIva (override manual / snapshot)
     *   3) defaults application.properties
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_pais")
    private Pais pais;

    /** Régimen fiscal: EXENTO | GENERAL | REDUCIDO. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "regimen_fiscal", nullable = false, length = 20)
    private RegimenFiscal regimenFiscal;

    /**
     * Tasa de IVA (%) snapshot/override. Se recalcula desde el país al
     * registrar la empresa; sirve de respaldo si el país no está vinculado.
     */
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Column(name = "tasa_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal tasaIva = new BigDecimal("16.00");

    @Email
    @Column(name = "contacto_email", length = 100)
    private String contactoEmail;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private OffsetDateTime fechaRegistro = OffsetDateTime.now();
}
