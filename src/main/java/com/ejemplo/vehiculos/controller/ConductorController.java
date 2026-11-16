package com.ejemplo.vehiculos.controller;

import com.ejemplo.vehiculos.model.Conductor;
import com.ejemplo.vehiculos.service.ConductorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST del catálogo de conductores.
 * La consulta está disponible para los roles autenticados permitidos por
 * {@code SecurityConfig}; las operaciones de escritura son exclusivas de ADMIN.
 */
@RestController
@RequestMapping("/api/conductores")
public class ConductorController {

    private final ConductorService conductorService;

    public ConductorController(ConductorService conductorService) {
        this.conductorService = conductorService;
    }

    /** Devuelve el catálogo completo para el cliente de la API. */
    @GetMapping
    public List<Conductor> listar() {
        return conductorService.listar();
    }

    /** Obtiene una ficha por id; el servicio convierte la ausencia en HTTP 404. */
    @GetMapping("/{id}")
    public Conductor obtener(@PathVariable Long id) {
        return conductorService.obtener(id);
    }

    /** Crea un conductor validado y responde HTTP 201; solo ADMIN. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Conductor crear(@Valid @RequestBody Conductor conductor) {
        return conductorService.crear(conductor);
    }

    /** Reemplaza los datos editables del conductor; solo ADMIN. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Conductor actualizar(@PathVariable Long id, @Valid @RequestBody Conductor conductor) {
        return conductorService.actualizar(id, conductor);
    }

    /** Elimina el conductor preservando referencias históricas; solo ADMIN. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(@PathVariable Long id) {
        conductorService.eliminar(id);
    }
}
