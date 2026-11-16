package com.ejemplo.vehiculos.controller;

import com.ejemplo.vehiculos.model.Rol;
import com.ejemplo.vehiculos.model.Usuario;
import com.ejemplo.vehiculos.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST de administración de cuentas. La seguridad se aplica a toda
 * la clase, de modo que cada ruta requiere rol ADMIN.
 *
 * Los records son DTO de entrada/salida: no se expone directamente la entidad
 * ni su hash de contraseña.
 */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    /** Alta de usuario: contraseña obligatoria y rol explícito. */
    public record UsuarioRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull Rol rol,
            Long conductorId) { }

    /** Datos editables; contraseña nula/vacía significa mantener la existente. */
    public record UsuarioUpdateRequest(
            @Size(min = 8, max = 72) String password,
            @NotNull Rol rol,
            Long conductorId) { }

    /** Respuesta pública de usuario, deliberadamente sin el password. */
    public record UsuarioResponse(Long id, String username, Rol rol, Long conductorId) {
        /** Convierte una entidad a la proyección pública y evita serializar relaciones completas. */
        static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getUsername(), u.getRol(),
                    u.getConductor() != null ? u.getConductor().getId() : null);
        }
    }

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Lista usuarios transformados a DTO públicos. */
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar().stream().map(UsuarioResponse::de).toList();
    }

    /** Crea una cuenta y responde HTTP 201. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest req) {
        return UsuarioResponse.de(
                usuarioService.crear(req.username(), req.password(), req.rol(), req.conductorId()));
    }

    /** Actualiza rol, conductor opcional y contraseña opcional del usuario. */
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest req,
                                      Authentication auth) {
        return UsuarioResponse.de(usuarioService.actualizar(id, req.password(), req.rol(),
                req.conductorId(), auth.getName()));
    }

    /** Elimina otra cuenta; el servicio impide que el administrador se elimine a sí mismo. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, Authentication auth) {
        usuarioService.eliminar(id, auth.getName());
    }
}
