package com.tienda.security;

import com.tienda.model.Usuario;
import com.tienda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Puente entre la tabla usuarios y Spring Security.
 * Traduce el nombre del rol (ADMIN) a la autoridad "ROLE_ADMIN" que
 * usan las reglas hasRole() de SecurityConfig.
 */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UsernameNotFoundException("Usuario desactivado: " + username);
        }

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPasswordHash()) // hash BCrypt almacenado en BD
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre())))
                .disabled(!usuario.getActivo())
                .build();
    }
}
