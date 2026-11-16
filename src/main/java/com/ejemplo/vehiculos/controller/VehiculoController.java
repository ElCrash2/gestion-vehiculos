package com.ejemplo.vehiculos.controller;

import com.ejemplo.vehiculos.model.EstadoVehiculo;
import com.ejemplo.vehiculos.model.Vehiculo;
import com.ejemplo.vehiculos.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST para consultar y administrar vehículos.
 * La lectura está disponible para ADMIN y CONDUCTOR; las escrituras solo
 * para ADMIN según las reglas de {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    /** Lista el catálogo, con filtro de estado opcional. */
    @GetMapping
    public List<Vehiculo> listar(@RequestParam(required = false) EstadoVehiculo estado) {
        return vehiculoService.listar(estado);
    }

    /** Obtiene un vehículo por id o devuelve 404 si no existe. */
    @GetMapping("/{id}")
    public Vehiculo obtener(@PathVariable Long id) {
        return vehiculoService.obtener(id);
    }

    /** Crea un vehículo validado y responde HTTP 201; solo ADMIN. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Vehiculo crear(@Valid @RequestBody Vehiculo vehiculo) {
        return vehiculoService.crear(vehiculo);
    }

    /** Modifica un vehículo; el servicio protege estado y kilometraje durante asignaciones activas. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Vehiculo actualizar(@PathVariable Long id, @Valid @RequestBody Vehiculo vehiculo) {
        return vehiculoService.actualizar(id, vehiculo);
    }

    /** Elimina un vehículo disponible preservando su matrícula en el histórico; solo ADMIN. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(@PathVariable Long id) {
        vehiculoService.eliminar(id);
    }
}
