package com.tienda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.dto.ProductoRequest;
import com.tienda.dto.ProductoResponse;
import com.tienda.service.ProductoService;
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

@WebMvcTest(ProductoController.class)
@DisplayName("ProductoController — Integration Tests")
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductoService productoService;

    @Test
    @DisplayName("GET /api/productos retorna catálogo público")
    void listarCatalogo() throws Exception {
        ProductoResponse prod = new ProductoResponse(1L, "AUD-001", "Audífonos OneOdio",
                "Over-ear 50mm", BigDecimal.valueOf(549), 28, 5, false,
                1L, "Audio", 12, 1L, "AudioMax", null, true);
        when(productoService.buscarCatalogo(null, null)).thenReturn(List.of(prod));

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku", is("AUD-001")));
    }

    @Test
    @DisplayName("GET /api/productos/{id} retorna producto")
    void obtenerProducto() throws Exception {
        ProductoResponse prod = new ProductoResponse(1L, "AUD-001", "Audífonos OneOdio",
                "Over-ear 50mm", BigDecimal.valueOf(549), 28, 5, false,
                1L, "Audio", 12, 1L, "AudioMax", null, true);
        when(productoService.obtenerPorId(1L)).thenReturn(prod);

        mockMvc.perform(get("/api/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Audífonos OneOdio")));
    }

    @Test
    @DisplayName("POST /api/productos requiere rol ADMIN")
    @WithMockUser(roles = "ADMIN")
    void crearProductoAdmin() throws Exception {
        ProductoRequest request = new ProductoRequest("AUD-099", "Nuevo", "Desc",
                BigDecimal.valueOf(299), 10, 5, 1L, 12, 1L, null, true);
        ProductoResponse respuesta = new ProductoResponse(1L, "AUD-099", "Nuevo", "Desc",
                BigDecimal.valueOf(299), 10, 5, false, 1L, "Audio", 12, 1L, "AudioMax", null, true);
        when(productoService.crear(any())).thenReturn(respuesta);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku", is("AUD-099")));
    }

    @Test
    @DisplayName("POST /api/productos con rol CLIENTE retorna 403")
    @WithMockUser(roles = "CLIENTE")
    void crearProductoCliente() throws Exception {
        ProductoRequest request = new ProductoRequest("AUD-099", "Nuevo", null,
                BigDecimal.valueOf(100), 5, 5, 1L, 12, null, null, true);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/productos/{id} actualiza producto")
    @WithMockUser(roles = "ADMIN")
    void actualizarProducto() throws Exception {
        ProductoRequest request = new ProductoRequest("AUD-001", "Audífonos V2", "Actualizado",
                BigDecimal.valueOf(599), 30, 5, 1L, 12, 1L, null, true);
        ProductoResponse respuesta = new ProductoResponse(1L, "AUD-001", "Audífonos V2", "Actualizado",
                BigDecimal.valueOf(599), 30, 5, false, 1L, "Audio", 12, 1L, "AudioMax", null, true);
        when(productoService.actualizar(1L, request)).thenReturn(respuesta);

        mockMvc.perform(put("/api/productos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Audífonos V2")));
    }

    @Test
    @DisplayName("DELETE /api/productos/{id} elimina producto")
    @WithMockUser(roles = "ADMIN")
    void eliminarProducto() throws Exception {
        mockMvc.perform(delete("/api/productos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/productos sin auth también funciona (público)")
    void catalogoPublico() throws Exception {
        when(productoService.buscarCatalogo(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk());
    }
}
