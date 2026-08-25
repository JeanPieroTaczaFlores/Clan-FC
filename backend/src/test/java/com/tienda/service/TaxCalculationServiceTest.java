package com.tienda.service;

import com.tienda.model.Pais;
import com.tienda.model.RegimenFiscal;
import com.tienda.model.EmpresaCliente;
import com.tienda.repository.PaisRepository;
import com.tienda.repository.EmpresaClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaxCalculationService — Unit Tests")
class TaxCalculationServiceTest {

    @Mock private PaisRepository paisRepository;
    @Mock private EmpresaClienteRepository empresaClienteRepository;

    @InjectMocks
    private TaxCalculationService taxCalculationService;

    private Pais peru;

    @BeforeEach
    void setUp() {
        peru = Pais.builder()
                .idPais(3L).codigoIso2("PE").nombre("Perú")
                .tasaIvaGeneral(BigDecimal.valueOf(18))
                .tasaIvaReducido(BigDecimal.valueOf(18))
                .build();
    }

    @Test
    @DisplayName("Calcular IVA con tasa general de Perú")
    void calcularIvaPeru() {
        when(paisRepository.findByCodigoIso2IgnoreCase("PE")).thenReturn(Optional.of(peru));

        var resultado = taxCalculationService.calcular(BigDecimal.valueOf(1000), "PE", RegimenFiscal.GENERAL, null);

        assertThat(resultado.tasaIva()).isEqualByComparingTo(BigDecimal.valueOf(18));
        assertThat(resultado.iva()).isEqualByComparingTo(BigDecimal.valueOf(180));
        assertThat(resultado.total()).isEqualByComparingTo(BigDecimal.valueOf(1180));
        assertThat(resultado.paisCodigo()).isEqualTo("PE");
    }

    @Test
    @DisplayName("Calcular IVA exento")
    void calcularIvaExento() {
        when(paisRepository.findByCodigoIso2IgnoreCase("PE")).thenReturn(Optional.of(peru));

        var resultado = taxCalculationService.calcular(BigDecimal.valueOf(500), "PE", RegimenFiscal.EXENTO, null);

        assertThat(resultado.tasaIva()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.iva()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.total()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    @DisplayName("Calcular IVA con tasa reducida")
    void calcularIvaReducido() {
        when(paisRepository.findByCodigoIso2IgnoreCase("PE")).thenReturn(Optional.of(peru));

        var resultado = taxCalculationService.calcular(BigDecimal.valueOf(2000), "PE", RegimenFiscal.REDUCIDO, null);

        assertThat(resultado.tasaIva()).isEqualByComparingTo(BigDecimal.valueOf(18));
        assertThat(resultado.iva()).isEqualByComparingTo(BigDecimal.valueOf(360));
        assertThat(resultado.total()).isEqualByComparingTo(BigDecimal.valueOf(2360));
    }

    @Test
    @DisplayName("Empresa cliente con tasa override")
    void empresaConTasaOverride() {
        EmpresaCliente empresa = EmpresaCliente.builder()
                .idEmpresa(1L).rfc("TEST123").razonSocial("Test SA")
                .regimenFiscal(RegimenFiscal.GENERAL)
                .tasaIva(BigDecimal.valueOf(12))
                .pais(peru)
                .build();
        when(empresaClienteRepository.findById(1L)).thenReturn(Optional.of(empresa));

        var resultado = taxCalculationService.calcular(
                BigDecimal.valueOf(1000), "PE", RegimenFiscal.GENERAL, 1L);

        assertThat(resultado.tasaIva()).isEqualByComparingTo(BigDecimal.valueOf(12));
        assertThat(resultado.iva()).isEqualByComparingTo(BigDecimal.valueOf(120));
    }

    @Test
    @DisplayName("Resolver tasa IVA desde usuario")
    void resolverDesdeUsuario() {
        com.tienda.model.Usuario usuario = com.tienda.model.Usuario.builder()
                .idUsuario(1L).pais(peru).build();

        var resultado = taxCalculationService.resolverTasaIva(usuario, null);

        assertThat(resultado.tasaIva()).isEqualByComparingTo(BigDecimal.valueOf(18));
        assertThat(resultado.pais()).isEqualTo(peru);
    }

    @Test
    @DisplayName("Resolver tasa IVA sin país usa default")
    void resolverSinPais() {
        com.tienda.model.Usuario usuario = com.tienda.model.Usuario.builder()
                .idUsuario(1L).pais(null).build();

        var resultado = taxCalculationService.resolverTasaIva(usuario, null);

        // Default de application.properties: 16%
        assertThat(resultado.tasaIva()).isEqualByComparingTo(BigDecimal.valueOf(16));
    }

    @Test
    @DisplayName("Bandera emoji desde país")
    void banderaEmoji() {
        when(paisRepository.findByCodigoIso2IgnoreCase("PE")).thenReturn(Optional.of(peru));

        var resultado = taxCalculationService.calcular(BigDecimal.valueOf(100), "PE", RegimenFiscal.GENERAL, null);

        assertThat(resultado.banderaEmoji()).isEqualTo("🇵🇪");
    }
}
