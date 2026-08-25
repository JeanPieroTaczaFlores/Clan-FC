package com.tienda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.dto.SedeRequest;
import com.tienda.dto.SedeResponse;
import com.tienda.service.SedeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SedeController.class)
@DisplayName("SedeController — Integration Tests")
class SedeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SedeService sedeService;

    @Test
    @DisplayName("GET /api/sedes retorna lista de sedes")
    @WithMockUser(roles = "ADMIN")
    void listarSedes() throws Exception {
        SedeResponse sede = new SedeResponse(1L, "Villa El Salvador", "Av. Los Héroes 200",
                "01-555-0001", true, java.math.BigDecimal.ZERO, 0, 0, 0);
        when(sedeService.listar()).thenReturn(List.of(sede));

        mockMvc.perform(get("/api/sedes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre", is("Villa El Salvador")));
    }

    @Test
    @DisplayName("POST /api/sedes crea sede válida")
    @WithMockUser(roles = "ADMIN")
    void crearSede() throws Exception {
        SedeRequest request = new SedeRequest("Nueva Sede", "Av. Principal", "01-555-9999", true);
        SedeResponse respuesta = new SedeResponse(1L, "Nueva Sede", "Av. Principal",
                "01-555-9999", true, java.math.BigDecimal.ZERO, 0, 0, 0);
        when(sedeService.crear(any())).thenReturn(respuesta);

        mockMvc.perform(post("/api/sedes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre", is("Nueva Sede")));
    }

    @Test
    @DisplayName("POST /api/sedes sin nombre retorna 400")
    @WithMockUser(roles = "ADMIN")
    void crearSedeSinNombre() throws Exception {
        SedeRequest request = new SedeRequest("", null, null, true);

        mockMvc.perform(post("/api/sedes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/sedes sin autenticación retorna 401")
    void listarSinAuth() throws Exception {
        mockMvc.perform(get("/api/sedes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/sedes con rol CLIENTE retorna 403")
    @WithMockUser(roles = "CLIENTE")
    void crearSedeCliente() throws Exception {
        SedeRequest request = new SedeRequest("Test", null, null, true);

        mockMvc.perform(post("/api/sedes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/sedes/{id} retorna sede específica")
    @WithMockUser(roles = "ADMIN")
    void obtenerSedePorId() throws Exception {
        SedeResponse sede = new SedeResponse(1L, "Chorrillos", "Av. Juan de Mariana 500",
                "01-555-0002", true, java.math.BigDecimal.ZERO, 0, 0, 0);
        when(sedeService.obtenerPorId(1L)).thenReturn(sede);

        mockMvc.perform(get("/api/sedes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Chorrillos")));
    }

    @Test
    @DisplayName("DELETE /api/sedes/{id} elimina sede")
    @WithMockUser(roles = "ADMIN")
    void eliminarSede() throws Exception {
        mockMvc.perform(delete("/api/sedes/1"))
                .andExpect(status().isNoContent());
    }
}
