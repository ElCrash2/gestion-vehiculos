package com.ejemplo.vehiculos.repository;

import com.ejemplo.vehiculos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Acceso JPA a {@code usuarios} y consultas derivadas usadas por autenticación y administración. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busca la cuenta para autenticar por su nombre de acceso. */
    Optional<Usuario> findByUsername(String username);

    /** Detecta duplicados antes de intentar insertar una cuenta. */
    boolean existsByUsername(String username);

    /** Encuentra las cuentas enlazadas al conductor antes de eliminar su ficha. */
    List<Usuario> findByConductorId(Long conductorId);
}
