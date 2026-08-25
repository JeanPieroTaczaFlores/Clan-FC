package com.tienda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.dto.CajaRequest;
import com.tienda.dto.CajaResponse;
import com.tienda.service.CajaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CajaController.class)
@DisplayName("CajaController — Integration Tests")
class CajaControllerTest {

    @Autowired
    private MockMockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CajaService cajaService;

    @Test
    @DisplayName("GET /api/cajas/sede/{id} retorna cajas de la sede")
    @WithMockUser(roles = "ADMIN")
    void listarCajasPorSede() throws Exception {
        CajaResponse caja = new CajaResponse(1L, 1L, "Villa El Salvador", 1,
                BigDecimal.valueOf(1500), "CERRADA", null, null, null, null);
        when(cajaService.listarPorSede(1L)).thenReturn(List.of(caja));

        mockMvc.perform(get("/api/cajas/sede/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numeroCaja", is(1)));
    }

    @Test
    @DisplayName("POST /api/cajas crea caja")
    @WithMockUser(roles = "ADMIN")
    void crearCaja() throws Exception {
        CajaRequest request = new CajaRequest(1L, 2, BigDecimal.ZERO, "CERRADA", null);
        CajaResponse respuesta = new CajaResponse(2L, 1L, "Villa El Salvador", 2,
                BigDecimal.ZERO, "CERRADA", null, null, null, null);
        when(cajaService.crear(any())).thenReturn(respuesta);

        mockMvc.perform(post("/api/cajas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCaja", is(2)));
    }

    @Test
    @DisplayName("POST /api/cajas/{id}/habilitar habilita caja")
    @WithMockUser(roles = "CAJERO")
    void habilitarCaja() throws Exception {
        CajaResponse respuesta = new CajaResponse(1L, 1L, "Villa El Salvador", 1,
                BigDecimal.valueOf(500), "ABIERTA", 1L, "cajero1", null, null);
        when(cajaService.habilitar(1L, 1L, BigDecimal.valueOf(500))).thenReturn(respuesta);

        mockMvc.perform(post("/api/cajas/1/habilitar")
                        .param("idUsuario", "1")
                        .param("fondosIniciales", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("ABIERTA")));
    }

    @Test
    @DisplayName("POST /api/cajas/{id}/cerrar cierra caja")
    @WithMockUser(roles = "ADMIN")
    void cerrarCaja() throws Exception {
        CajaResponse respuesta = new CajaResponse(1L, 1L, "Villa El Salvador", 1,
                BigDecimal.valueOf(1500), "CERRADA", 1L, "cajero1", null, null);
        when(cajaService.cerrar(1L)).thenReturn(respuesta);

        mockMvc.perform(post("/api/cajas/1/cerrar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CERRADA")));
    }

    @Test
    @DisplayName("GET /api/cajas/abierta/sede/{id} retorna caja abierta")
    @WithMockUser(roles = "CAJERO")
    void obtenerCajaAbierta() throws Exception {
        CajaResponse caja = new CajaResponse(1L, 1L, "Villa El Salvador", 1,
                BigDecimal.valueOf(500), "ABIERTA", 1L, "cajero1", null, null);
        when(cajaService.obtenerCajaAbierta(1L)).thenReturn(caja);

        mockMvc.perform(get("/api/cajas/abierta/sede/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("ABIERTA")));
    }

    @Test
    @DisplayName("POST /api/cajas/movimientos registra movimiento")
    @WithMockUser(roles = "CAJERO")
    void registrarMovimiento() throws Exception {
        var request = new com.tienda.dto.CajaMovimientoRequest(1L, "VENTA", BigDecimal.valueOf(250), "ORD-001");
        var respuesta = new com.tienda.dto.CajaMovimientoResponse(1L, 1L, 1, "Villa El Salvador",
                "VENTA", BigDecimal.valueOf(250), BigDecimal.valueOf(750), "ORD-001", "cajero1", null);
        when(cajaService.registrarMovimiento(any(), any())).thenReturn(respuesta);

        mockMvc.perform(post("/api/cajas/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo", is("VENTA")));
    }

    @Test
    @DisplayName("GET /api/cajas sin auth retorna 401")
    void sinAuth() throws Exception {
        mockMvc.perform(get("/api/cajas/sede/1"))
                .andExpect(status().isUnauthorized());
    }
}
