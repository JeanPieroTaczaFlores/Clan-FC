package com.tienda.model;

/**
 * Regímenes fiscales soportados para la parametrización del IVA.
 * Se persiste como VARCHAR en empresas_clientes.regimen_fiscal.
 */
public enum RegimenFiscal {
    /** Cliente exento: no paga IVA (0%). */
    EXENTO,
    /** Régimen general: aplica la tasa estándar (16% por defecto). */
    GENERAL,
    /** Régimen reducido: tasa preferencial (8% por defecto, frontera/zonas especiales). */
    REDUCIDO
}
