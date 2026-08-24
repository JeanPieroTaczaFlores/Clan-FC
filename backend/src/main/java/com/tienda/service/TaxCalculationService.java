package com.tienda.service;

import com.tienda.dto.TotalesImpuestosResponse;
import com.tienda.model.EmpresaCliente;
import com.tienda.model.Pais;
import com.tienda.model.RegimenFiscal;import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * NÚCLEO de la parametrización de IVA (Entregable 2/3).
 *
 * El IVA va POR PAÍS. Precedencia de resolución:
 *  1) PAÍS + RÉGIMEN de la empresa: pais.tasa_iva_general / tasa_iva_reducido
 *     (dinámico: si el país cambia su ley fiscal, aplica sin tocar código).
 *  2) tasa_iva de la empresa (override manual / snapshot histórico).
 *  3) Defaults del consumidor final en application.properties.
 */
@Service
public class TaxCalculationService {

    /** Tasa por defecto para régimen GENERAL (consumidor final, país desconocido). */
    @Value("${tienda.iva.general:16}")
    private BigDecimal tasaGeneral;

    @Value("${tienda.iva.reducido:8}")
    private BigDecimal tasaReducido;

    @Value("${tienda.iva.exento:0}")
    private BigDecimal tasaExenta;

    /**
     * Resuelve la tasa de IVA (%) aplicable a la venta según país y régimen.
     * Precedencia: empresa (país+régimen) > usuario sin empresa (país general)
     * > defaults de application.properties.
     *
     * @param empresa     empresa compradora; null = consumidor final
     * @param paisUsuario país del usuario logueado (consumidor final con país)
     */
    public BigDecimal resolverTasa(EmpresaCliente empresa, Pais paisUsuario) {
        // 1) País vinculado: sus tasas mandan (IVA por país).
        if (empresa != null && empresa.getPais() != null) {
            Pais pais = empresa.getPais();
            return switch (regimenDe(empresa)) {
                case EXENTO -> BigDecimal.ZERO;                       // exención siempre es 0%
                case REDUCIDO -> pais.getTasaIvaReducido();
                case GENERAL -> pais.getTasaIvaGeneral();
            };
        }
        // 2) Override manual guardado en la empresa (sin país vinculado).
        if (empresa != null && empresa.getTasaIva() != null) {
            return empresa.getTasaIva();
        }
        // 3) Consumidor final CON país registrado: tasa GENERAL de su país.
        if (paisUsuario != null) {
            return paisUsuario.getTasaIvaGeneral();
        }
        // 4) Consumidor final anónimo/sin país: tasas por defecto configurables.
        return switch (regimenDe(empresa)) {
            case EXENTO -> tasaExenta;
            case REDUCIDO -> tasaReducido;
            case GENERAL -> tasaGeneral;
        };
    }

    /** Compatibilidad con llamadas existentes. */
    public BigDecimal resolverTasa(EmpresaCliente empresa) {
        return resolverTasa(empresa, null);
    }

    /** Régimen efectivo de la venta (GENERAL cuando no hay empresa). */
    public RegimenFiscal regimenDe(EmpresaCliente empresa) {
        return (empresa != null) ? empresa.getRegimenFiscal() : RegimenFiscal.GENERAL;
    }

    /** Tasa del país según régimen — usada al registrar empresas nuevas. */
    public BigDecimal resolverTasaConPais(Pais pais, RegimenFiscal regimen) {
        return switch (regimen) {
            case EXENTO -> BigDecimal.ZERO;
            case REDUCIDO -> pais.getTasaIvaReducido();
            case GENERAL -> pais.getTasaIvaGeneral();
        };
    }

    /** IVA = base * tasa / 100, redondeo comercial a 2 decimales (HALF_UP). */
    public BigDecimal calcularIva(BigDecimal baseSinIva, BigDecimal tasa) {
        return baseSinIva.multiply(tasa)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Desglose completo subtotal/IVA/total + datos de país/bandera — usado por
     * la calculadora en vivo del frontend y como snapshot al confirmar órdenes.
     * Para consumidor final usa el PAÍS DEL USUARIO logueado (si tiene).
     */
    public TotalesImpuestosResponse calcularTotales(BigDecimal subtotalBase, EmpresaCliente empresa, Pais paisUsuario) {
        BigDecimal tasa = resolverTasa(empresa, paisUsuario);
        BigDecimal iva = calcularIva(subtotalBase, tasa);

        // País mostrado: el de la empresa; si no hay, el del usuario consumidor final.
        Pais pais = (empresa != null) ? empresa.getPais() : paisUsuario;
        String iso = (pais != null) ? pais.getCodigoIso2() : null;

        return new TotalesImpuestosResponse(
                escala2(subtotalBase),
                escala2(tasa),
                regimenDe(empresa).name(),
                iva,
                escala2(subtotalBase).add(iva),
                iso,
                (pais != null) ? pais.getNombre() : null,
                Pais.banderaDesde(iso)
        );
    }

    /** Compatibilidad con llamadas sin contexto de usuario. */
    public TotalesImpuestosResponse calcularTotales(BigDecimal subtotalBase, EmpresaCliente empresa) {
        return calcularTotales(subtotalBase, empresa, null);
    }

    private BigDecimal escala2(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
