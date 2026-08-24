package com.tienda.controller;

import com.tienda.dto.IncidenciaRequest;
import com.tienda.dto.IncidenciaResponse;
import com.tienda.model.Usuario;
import com.tienda.repository.UsuarioRepository;
import com.tienda.service.IncidenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * INCIDENCIAS — productos devueltos, defectuosos o en garantía.
 * El cajero reporta desde el POS; se da seguimiento hasta resolverlas.
 */
@RestController
@RequestMapping("/api/incidencias")
@RequiredArgsConstructor
public class IncidenciasController {

    private final IncidenciaService incidenciaService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenciaResponse crear(@Valid @RequestBody IncidenciaRequest request,
                                    Authentication authentication) {
        return incidenciaService.crear(request, obtenerUsuario(authentication));
    }

    @GetMapping
    public List<IncidenciaResponse> listar() {
        return incidenciaService.listar();
    }

    /** Cambia estado (REPORTADA/EN_REVISION/RESUELTA/CANCELADA) con resolución opcional. */
    @PatchMapping("/{id}/estado")
    public IncidenciaResponse cambiarEstado(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            Authentication authentication) {
        return incidenciaService.cambiarEstado(
                id,
                body.getOrDefault("estado", "EN_REVISION"),
                body.get("resolucion"),
                obtenerUsuario(authentication));
    }

    private Usuario obtenerUsuario(Authentication authentication) {
        return usuarioRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Usuario no encontrado"));
    }
}
