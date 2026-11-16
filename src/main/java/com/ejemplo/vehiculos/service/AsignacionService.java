package com.ejemplo.vehiculos.service;

import com.ejemplo.vehiculos.model.Asignacion;
import com.ejemplo.vehiculos.model.Conductor;
import com.ejemplo.vehiculos.model.EstadoAsignacion;
import com.ejemplo.vehiculos.model.EstadoVehiculo;
import com.ejemplo.vehiculos.model.Motivo;
import com.ejemplo.vehiculos.model.Vehiculo;
import com.ejemplo.vehiculos.repository.AsignacionRepository;
import com.ejemplo.vehiculos.repository.ConductorRepository;
import com.ejemplo.vehiculos.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Casos de uso y reglas del ciclo de vida de una asignación.
 *
 * Coordina repositorios de asignaciones, conductores y vehículos dentro de
 * transacciones. El estado del vehículo cambia al crear/finalizar una
 * asignación; retención se configura con {@code asignaciones.retencion-dias}.
 * Para cambiar reglas o relaciones, revisar también constraints e índices de
 * {@code db/schema.sql}.
 */
@Service
public class AsignacionService {

    /** Persistencia del histórico y consultas sobre estado/relaciones. */
    private final AsignacionRepository asignacionRepository;
    /** Persistencia del vehículo, incluido el bloqueo pesimista al asignarlo. */
    private final VehiculoRepository vehiculoRepository;
    /** Persistencia necesaria para validar y enlazar el conductor. */
    private final ConductorRepository conductorRepository;

    /**
     * Antigüedad mínima (en días desde el regreso real) para poder eliminar una asignación finalizada.
     * Configurable con la variable de entorno ASIGNACIONES_RETENCION_DIAS (por defecto 365).
     */
    @Value("${asignaciones.retencion-dias:365}")
    private int retencionDias = 365;

    public AsignacionService(AsignacionRepository asignacionRepository,
                             VehiculoRepository vehiculoRepository,
                             ConductorRepository conductorRepository) {
        this.asignacionRepository = asignacionRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.conductorRepository = conductorRepository;
    }

    /** Lista todas las asignaciones o limita la consulta a un estado concreto. */
    @Transactional(readOnly = true)
    public List<Asignacion> listar(EstadoAsignacion estado) {
        return estado == null ? asignacionRepository.findAll() : asignacionRepository.findByEstadoAsignacion(estado);
    }

    /** Obtiene una asignación por id y lanza excepción de recurso ausente si no existe. */
    @Transactional(readOnly = true)
    public Asignacion obtener(Long id) {
        return asignacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación", id));
    }

    /**
     * Crea una asignación.
     * - Solo se puede asignar un vehículo DISPONIBLE (no EN_RUTA ni TALLER).
     * - Un conductor solo puede tener una asignación de Ruta activa a la vez; de Taller, las que haga falta.
     * - El vehículo pasa a EN_RUTA (motivo Ruta) o TALLER (motivo Taller).
     * - km_salida se copia del kilometraje actual del vehículo.
     * La fecha de salida puede omitirse; la prevista debe ser igual o posterior.
     */
    @Transactional
    public Asignacion crear(Long conductorId, Long vehiculoId, LocalDateTime fechaSalida,
                            LocalDateTime fechaPrevistaRegreso, Motivo motivo) {

        Conductor conductor = conductorRepository.findById(conductorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Conductor", conductorId));

        // Un conductor solo puede tener UNA asignación de Ruta activa (las de Taller no tienen límite)
        if (motivo == Motivo.RUTA && asignacionRepository.existsByConductorIdAndEstadoAsignacionAndMotivo(
                conductorId, EstadoAsignacion.ACTIVA, Motivo.RUTA)) {
            throw ReglaNegocioException.conflicto("El conductor " + conductor.getNombre()
                    + " ya tiene una asignación de Ruta activa: debe finalizarla antes de salir con otro vehículo"
                    + " de ruta (las asignaciones de Taller no tienen este límite)");
        }

        // Bloqueo de fila: evita que dos peticiones simultáneas asignen el mismo vehículo
        Vehiculo vehiculo = vehiculoRepository.findByIdForUpdate(vehiculoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo", vehiculoId));

        if (vehiculo.getEstado() != EstadoVehiculo.DISPONIBLE) {
            throw ReglaNegocioException.conflicto("El vehículo " + vehiculo.getMatricula()
                    + " no se puede asignar: su estado actual es " + vehiculo.getEstado());
        }

        LocalDateTime salida = fechaSalida != null ? fechaSalida : LocalDateTime.now();
        if (fechaPrevistaRegreso.isBefore(salida)) {
            throw ReglaNegocioException.invalida(
                    "La fecha prevista de regreso no puede ser anterior a la fecha de salida");
        }

        Asignacion asignacion = new Asignacion();
        asignacion.setConductor(conductor);
        asignacion.setVehiculo(vehiculo);
        asignacion.setConductorNombre(conductor.getNombre());
        asignacion.setVehiculoMatricula(vehiculo.getMatricula());
        asignacion.setFechaSalida(salida);
        asignacion.setFechaPrevistaRegreso(fechaPrevistaRegreso);
        asignacion.setMotivo(motivo);
        asignacion.setKmSalida(vehiculo.getKilometraje());
        asignacion.setEstadoAsignacion(EstadoAsignacion.ACTIVA);

        vehiculo.setEstado(motivo == Motivo.TALLER ? EstadoVehiculo.TALLER : EstadoVehiculo.EN_RUTA);
        vehiculoRepository.save(vehiculo);

        return asignacionRepository.save(asignacion);
    }

    /**
     * Finaliza una asignación activa.
     * - km_regreso debe ser >= km_salida.
     * - Se actualiza el kilometraje global del vehículo y vuelve a DISPONIBLE.
     * El vehículo se vuelve a bloquear para serializar actualizaciones concurrentes.
     */
    @Transactional
    public Asignacion finalizar(Long id, Integer kmRegreso, LocalDateTime fechaRegresoReal) {
        Asignacion asignacion = obtener(id);

        if (asignacion.getEstadoAsignacion() != EstadoAsignacion.ACTIVA) {
            throw ReglaNegocioException.conflicto("La asignación " + id + " ya está finalizada");
        }
        if (kmRegreso < asignacion.getKmSalida()) {
            throw ReglaNegocioException.invalida("El kilometraje de regreso (" + kmRegreso
                    + ") no puede ser menor que el de salida (" + asignacion.getKmSalida() + ")");
        }

        Vehiculo vehiculo = vehiculoRepository.findByIdForUpdate(asignacion.getVehiculo().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo", asignacion.getVehiculo().getId()));

        asignacion.setKmRegreso(kmRegreso);
        asignacion.setFechaRegresoReal(fechaRegresoReal != null ? fechaRegresoReal : LocalDateTime.now());
        asignacion.setEstadoAsignacion(EstadoAsignacion.FINALIZADA);

        vehiculo.setKilometraje(kmRegreso);
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        vehiculoRepository.save(vehiculo);

        return asignacionRepository.save(asignacion);
    }

    /** Expone la política de retención para informar a la interfaz/API. */
    public int getRetencionDias() {
        return retencionDias;
    }

    /**
     * Elimina una asignación. Solo se permite si está FINALIZADA y su regreso real
     * es anterior al límite calculado desde la hora actual. Las comparaciones
     * usan fecha de regreso real y el valor de retención configurado.
     */
    @Transactional
    public void eliminar(Long id) {
        Asignacion a = obtener(id);
        if (a.getEstadoAsignacion() != EstadoAsignacion.FINALIZADA || a.getFechaRegresoReal() == null) {
            throw ReglaNegocioException.conflicto("Solo se pueden eliminar asignaciones finalizadas");
        }
        if (!a.getFechaRegresoReal().isBefore(limiteAntiguedad())) {
            String fecha = a.getFechaRegresoReal().plusDays(retencionDias)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            throw ReglaNegocioException.conflicto("Solo se pueden eliminar asignaciones finalizadas hace más de "
                    + retencionDias + " días. Esta se podrá eliminar a partir del " + fecha);
        }
        asignacionRepository.delete(a);
    }

    /**
     * Elimina todas las asignaciones finalizadas que superan la antigüedad mínima.
     * Devuelve el número de registros seleccionados para borrar.
     */
    @Transactional
    public int depurarAntiguas() {
        List<Asignacion> antiguas = asignacionRepository
                .findByEstadoAsignacionAndFechaRegresoRealBefore(EstadoAsignacion.FINALIZADA, limiteAntiguedad());
        asignacionRepository.deleteAll(antiguas);
        return antiguas.size();
    }

    /** Calcula la fecha/hora anterior a la cual los registros cumplen retención. */
    private LocalDateTime limiteAntiguedad() {
        return LocalDateTime.now().minusDays(retencionDias);
    }
}
