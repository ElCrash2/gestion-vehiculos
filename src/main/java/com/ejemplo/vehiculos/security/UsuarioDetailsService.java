package com.ejemplo.vehiculos.security;

import com.ejemplo.vehiculos.model.Usuario;
import com.ejemplo.vehiculos.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Adaptador entre el repositorio de usuarios de la aplicación y Spring Security.
 * Convierte la entidad persistida en el principal utilizado por HTTP Basic.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Carga usuario por username y transforma su rol a una autoridad ROLE_*. */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(usuario.getRol().name())   // genera ROLE_ADMIN / ROLE_CONDUCTOR
                .build();
    }
}
