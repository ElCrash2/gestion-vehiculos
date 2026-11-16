package com.ejemplo.vehiculos.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configura dos mecanismos de acceso independientes: formulario y sesión para
 * páginas Thymeleaf, HTTP Basic stateless para clientes REST existentes.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** BCrypt es el codificador usado al guardar contraseñas y al autenticar. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** API REST: Basic stateless, respuestas JSON 401/403 y sin token CSRF. */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/vehiculos/**", "/api/conductores/**")
                    .hasAnyRole("ADMIN", "CONDUCTOR")
                .requestMatchers("/api/vehiculos/**", "/api/conductores/**").hasRole("ADMIN")
                .requestMatchers("/api/asignaciones/**").hasAnyRole("ADMIN", "CONDUCTOR")
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .httpBasic(b -> b.authenticationEntryPoint((req, res, ex) ->
                    escribirError(res, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                            "Credenciales no válidas o ausentes")))
            .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) ->
                    escribirError(res, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                            "No tienes permisos para realizar esta operación")));
        return http.build();
    }

    /**
     * Interfaz HTML: Spring Security procesa el formulario de login, mantiene
     * la sesión y protege los formularios mutables con CSRF.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**", "/favicon.ico")
                    .permitAll()
                .requestMatchers("/usuarios/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/app", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"));
        return http.build();
    }

    /** Escribe una respuesta JSON sencilla para errores de autenticación/autorización de la API. */
    private static void escribirError(HttpServletResponse res, int estado, String error, String mensaje)
            throws java.io.IOException {
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        res.getWriter().write("{\"estado\":" + estado + ",\"error\":\"" + error
                + "\",\"mensaje\":\"" + mensaje + "\"}");
    }
}
