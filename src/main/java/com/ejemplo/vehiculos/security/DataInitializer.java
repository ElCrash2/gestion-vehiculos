package com.ejemplo.vehiculos.security;

import com.ejemplo.vehiculos.model.Rol;
import com.ejemplo.vehiculos.model.Usuario;
import com.ejemplo.vehiculos.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer usuario ADMIN si la tabla de usuarios está vacía.
 * La contraseña se lee del secreto Docker "admin_password" (configtree)
 * o de la variable de entorno ADMIN_PASSWORD. Si no hay ninguna, no se crea
 * ningún usuario por defecto (evita contraseñas conocidas).
 *
 * Se ejecuta una vez durante el arranque. No es un mecanismo de migración
 * de esquema: para cambios de tablas/columnas se debe actualizar el esquema SQL.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    /** Logger de los resultados del aprovisionamiento inicial. */
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${ADMIN_USERNAME:admin}") String adminUsername,
                           @Value("${admin_password:${ADMIN_PASSWORD:}}") String adminPassword) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    /** Comprueba si hacen falta credenciales iniciales y crea ADMIN con hash BCrypt. */
    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("No hay usuarios y no se ha definido admin_password / ADMIN_PASSWORD: no se crea ningún ADMIN inicial");
            return;
        }
        Usuario admin = new Usuario();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRol(Rol.ADMIN);
        usuarioRepository.save(admin);
        log.info("Usuario ADMIN inicial '{}' creado", adminUsername);
    }
}
