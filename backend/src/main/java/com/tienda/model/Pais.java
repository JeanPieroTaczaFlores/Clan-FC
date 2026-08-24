package com.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.Transient;
import lombok.*;

import java.math.BigDecimal;

/**
 * País fiscal del cliente: el IVA depende del país (no de valores fijos).
 * codigoIso2 (ISO 3166-1 alpha-2) permite derivar la bandera emoji en UI.
 */
@Entity
@Table(name = "paises")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pais")
    private Long idPais;

    /** Código ISO 3166-1 alpha-2 en mayúsculas (MX, CO, ES...). */
    @NotBlank
    @Column(name = "codigo_iso2", nullable = false, unique = true, length = 2)
    private String codigoIso2;

    @NotBlank
    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    /** Tasa general del país (consumidor final / régimen GENERAL). */
    @NotNull
    @DecimalMin("0.00") @DecimalMax("100.00")
    @Column(name = "tasa_iva_general", nullable = false, precision = 5, scale = 2)
    private BigDecimal tasaIvaGeneral;

    /** Tasa reducida del país (régimen REDUCIDO). */
    @NotNull
    @DecimalMin("0.00") @DecimalMax("100.00")
    @Column(name = "tasa_iva_reducido", nullable = false, precision = 5, scale = 2)
    private BigDecimal tasaIvaReducido;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    /**
     * Bandera emoji derivada del código ISO (regional indicators).
     * Ej.: "MX" -> 🇲🇽. No se persiste (@Transient).
     */
    @Transient
    public String getBanderaEmoji() {
        return banderaDesde(codigoIso2);
    }

    public static String banderaDesde(String iso2) {
        if (iso2 == null || iso2.length() != 2) return "🌐";
        int[] puntos = iso2.toUpperCase().chars()
                .map(c -> 0x1F1E6 + (c - 'A'))
                .toArray();
        return new String(puntos, 0, 2);
    }
}
