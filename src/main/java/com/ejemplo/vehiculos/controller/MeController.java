package com.ejemplo.vehiculos.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de identidad para que la interfaz confirme el login y conozca el rol
 * del principal autenticado. La autenticación se configura en {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    /** Proyección de los datos de autenticación que necesita el cliente web. */
    public record MeResponse(String username, String rol) { }

    /** Extrae el principal y la primera autoridad ROLE_* de la autenticación actual. */
    @GetMapping
    public MeResponse me(Authentication auth) {
        String rol = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
        return new MeResponse(auth.getName(), rol);
    }
}
