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
 * Empresa cliente (B2B). Define el régimen fiscal que determina el IVA
 * aplicable en sus compras — base del TaxCalculationService (Entregable 2).
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

    /** RFC fiscal único (México): persona física (13) o moral (12). */
    @NotBlank
    @Column(nullable = false, unique = true, length = 13)
    private String rfc;

    /** Régimen fiscal: EXENTO | GENERAL | REDUCIDO. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "regimen_fiscal", nullable = false, length = 20)
    private RegimenFiscal regimenFiscal;

    /**
     * Tasa de IVA (%) parametrizable por empresa.
     * Convención sugerida: EXENTO=0.00 | REDUCIDO=8.00 | GENERAL=16.00.
     * El servicio de cálculo usa ESTA tasa (no valores fijos) para permitir
     * cambios de política sin recompilar.
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
