package com.tienda.controller;

import com.tienda.dto.RegistroUsuarioRequest;
import com.tienda.dto.UsuarioResponse;
import com.tienda.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GESTIÓN DE USUARIOS — exclusiva del rol ADMIN.
 * Aquí se dan de alta los CAJEROS y otros ADMINS (los clientes se
 * autoregistran en /api/auth/registro).
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuariosController {

    private final UsuarioService usuarioService;

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    /** Alta de usuario con cualquier rol (CAJERO, ADMIN, CLIENTE). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody RegistroUsuarioRequest request,
                                 @RequestParam(defaultValue = "CLIENTE") String rol) {
        return usuarioService.crearPorAdmin(request, rol.toUpperCase());
    }
}
