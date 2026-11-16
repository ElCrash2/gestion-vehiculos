package com.ejemplo.vehiculos.service;

import com.ejemplo.vehiculos.model.Conductor;
import com.ejemplo.vehiculos.model.Rol;
import com.ejemplo.vehiculos.model.Usuario;
import com.ejemplo.vehiculos.repository.ConductorRepository;
import com.ejemplo.vehiculos.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de administración de usuarios.
 *
 * La autenticación se resuelve en seguridad; este servicio administra
 * usuarios, codifica contraseñas y aplica las reglas que dependen del
 * administrador autenticado. La protección general de endpoints está en el
 * controlador y la unicidad/relaciones en el modelo y el esquema SQL.
 */
@Service
public class UsuarioService {

    /** Persistencia de cuentas y verificación de username único. */
    private final UsuarioRepository usuarioRepository;
    /** Resuelve el enlace opcional entre cuenta y conductor. */
    private final ConductorRepository conductorRepository;
    /** Nunca se guarda ni se compara una contraseña en texto plano. */
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ConductorRepository conductorRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.conductorRepository = conductorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Lista entidades para que el controlador las convierta a DTO públicos. */
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    /** Crea una cuenta, comprueba el username y codifica el password antes de persistir. */
    @Transactional
    public Usuario crear(String username, String passwordPlano, Rol rol, Long conductorId) {
        if (usuarioRepository.existsByUsername(username)) {
            throw ReglaNegocioException.conflicto("Ya existe el usuario " + username);
        }
        Conductor conductor = null;
        if (conductorId != null) {
            conductor = conductorRepository.findById(conductorId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Conductor", conductorId));
        }
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(passwordPlano));
        u.setRol(rol);
        u.setConductor(conductor);
        return usuarioRepository.save(u);
    }

    /**
     * Cambia rol, conductor enlazado y (opcionalmente) la contraseña.
     * El username no se modifica y el usuario actual no puede quitarse ADMIN.
     */
    /** Elimina otra cuenta; impide que el administrador elimine su propia cuenta. */
    @Transactional
    public Usuario actualizar(Long id, String passwordPlano, Rol rol, Long conductorId, String usernameActual) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        if (u.getUsername().equals(usernameActual) && rol != Rol.ADMIN) {
            throw ReglaNegocioException.conflicto("No puedes quitarte a ti mismo el rol ADMIN");
        }
        Conductor conductor = null;
        if (conductorId != null) {
            conductor = conductorRepository.findById(conductorId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Conductor", conductorId));
        }
        if (passwordPlano != null && !passwordPlano.isBlank()) {
            u.setPassword(passwordEncoder.encode(passwordPlano));
        }
        u.setRol(rol);
        u.setConductor(conductor);
        return usuarioRepository.save(u);
    }

    @Transactional
    public void eliminar(Long id, String usernameActual) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        if (u.getUsername().equals(usernameActual)) {
            throw ReglaNegocioException.conflicto("No puedes eliminar tu propio usuario");
        }
        usuarioRepository.delete(u);
    }
}
