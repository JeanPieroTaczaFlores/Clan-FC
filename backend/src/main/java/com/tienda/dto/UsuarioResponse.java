package com.tienda.dto;

import com.tienda.model.Rol;
import com.tienda.model.Pais;

/**
 * Usuario visible en el panel ADMIN. Nunca expone el hash de contraseña.
 * banderaEmoji permite pintar "🇨🇴 María" en la tabla.
 */
public record UsuarioResponse(
        Long idUsuario,
        String username,
        String email,
        String nombreCompleto,
        String rol,
        Long idPais,
        String paisNombre,
        String banderaEmoji,
        Boolean activo
) {
    public static UsuarioResponse from(com.tienda.model.Usuario u) {
        Rol rol = u.getRol();
        Pais pais = u.getPais();
        return new UsuarioResponse(
                u.getIdUsuario(),
                u.getUsername(),
                u.getEmail(),
                u.getNombreCompleto(),
                rol != null ? rol.getNombre() : null,
                pais != null ? pais.getIdPais() : null,
                pais != null ? pais.getNombre() : null,
                Pais.banderaDesde(pais != null ? pais.getCodigoIso2() : null),
                u.getActivo()
        );
    }
}
