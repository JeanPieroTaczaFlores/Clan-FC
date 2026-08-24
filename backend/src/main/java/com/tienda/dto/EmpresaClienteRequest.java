package com.tienda.dto;

import com.tienda.model.RegimenFiscal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de empresa cliente B2B desde el panel ADMIN.
 * El país es obligatorio: define las tasas de IVA aplicables (IVA por país).
 */
public record EmpresaClienteRequest(
        @NotBlank @Size(max = 150) String razonSocial,
        @NotBlank @Size(min = 10, max = 13) String rfc,       // RFC/NIT/RUC según país
        @NotNull Long idPais,                                  // país fiscal (obligatorio)
        @NotNull RegimenFiscal regimenFiscal,
        @Email String contactoEmail
) {
}