package com.ejemplo.vehiculos.controller;

import com.ejemplo.vehiculos.model.Asignacion;
import com.ejemplo.vehiculos.model.EstadoAsignacion;
import com.ejemplo.vehiculos.model.Motivo;
import com.ejemplo.vehiculos.service.AsignacionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Adaptador REST para el ciclo de vida de asignaciones.
 *
 * Define rutas y validación de transporte; las reglas de negocio y las
 * transacciones pertenecen a {@link AsignacionService}. ADMIN y CONDUCTOR
 * pueden consultar/crear/finalizar; solo ADMIN puede borrar o depurar.
 */
@RestController
@RequestMapping("/api/asignaciones")
public class AsignacionController {

    /** Cuerpo de alta; la fecha de salida es opcional y por defecto es ahora. */
    public record AsignacionRequest(
            @NotNull Long conductorId,
            @NotNull Long vehiculoId,
            LocalDateTime fechaSalida,                 // opcional: por defecto, ahora
            @NotNull LocalDateTime fechaPrevistaRegreso,
            @NotNull Motivo motivo) { }

    /** Cuerpo de cierre; la fecha real es opcional y por defecto es ahora. */
    public record FinalizarRequest(
            @NotNull @Min(0) Integer kmRegreso,
            LocalDateTime fechaRegresoReal) { }        // opcional: por defecto, ahora

    private final AsignacionService asignacionService;

    public AsignacionController(AsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    /** Lista todas las asignaciones o filtra por estado cuando se proporciona. */
    @GetMapping
    public List<Asignacion> listar(@RequestParam(required = false) EstadoAsignacion estado) {
        return asignacionService.listar(estado);
    }

    /** Devuelve los días de retención configurados antes de permitir un borrado. */
    @GetMapping("/retencion")
    public Map<String, Integer> retencion() {
        return Map.of("dias", asignacionService.getRetencionDias());
    }

    /** Consulta una asignación por su identificador o produce 404 si no existe. */
    @GetMapping("/{id}")
    public Asignacion obtener(@PathVariable Long id) {
        return asignacionService.obtener(id);
    }

    /** Registra una asignación nueva y responde con HTTP 201. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Asignacion crear(@Valid @RequestBody AsignacionRequest req) {
        return asignacionService.crear(req.conductorId(), req.vehiculoId(), req.fechaSalida(),
                req.fechaPrevistaRegreso(), req.motivo());
    }

    /** Cierra una asignación activa con kilometraje y fecha efectiva de regreso. */
    @PutMapping("/{id}/finalizar")
    public Asignacion finalizar(@PathVariable Long id, @Valid @RequestBody FinalizarRequest req) {
        return asignacionService.finalizar(id, req.kmRegreso(), req.fechaRegresoReal());
    }

    /** Solo ADMIN; el servicio verifica que la asignación esté fuera de retención. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(@PathVariable Long id) {
        asignacionService.eliminar(id);
    }

    /** Solo ADMIN; elimina en lote todas las asignaciones finalizadas que ya vencieron. */
    @DeleteMapping("/antiguas")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> depurarAntiguas() {
        return Map.of("eliminadas", asignacionService.depurarAntiguas());
    }
}
