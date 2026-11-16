package com.ejemplo.vehiculos.service;

import com.ejemplo.vehiculos.model.Asignacion;
import com.ejemplo.vehiculos.model.EstadoAsignacion;
import com.ejemplo.vehiculos.model.EstadoVehiculo;
import com.ejemplo.vehiculos.model.Vehiculo;
import com.ejemplo.vehiculos.repository.AsignacionRepository;
import com.ejemplo.vehiculos.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso del catálogo y estado operativo de vehículos.
 *
 * Centraliza normalización de matrículas, restricciones de edición durante
 * asignaciones activas y preservación del histórico al eliminar una ficha.
 * Los cambios de columnas deben reflejarse también en {@code db/schema.sql}.
 */
@Service
public class VehiculoService {

    /** CRUD de la tabla vehiculos. */
    private final VehiculoRepository vehiculoRepository;
    /** Comprueba asignaciones activas y actualiza sus referencias históricas. */
    private final AsignacionRepository asignacionRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository, AsignacionRepository asignacionRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.asignacionRepository = asignacionRepository;
    }

    /** Lista todos los vehículos o filtra por estado cuando se indica uno. */
    @Transactional(readOnly = true)
    public List<Vehiculo> listar(EstadoVehiculo estado) {
        return estado == null ? vehiculoRepository.findAll() : vehiculoRepository.findByEstado(estado);
    }

    /** Busca por id y lanza excepción de recurso ausente si no existe. */
    @Transactional(readOnly = true)
    public Vehiculo obtener(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo", id));
    }

    /** Normaliza la matrícula y rechaza duplicados antes de insertar. */
    @Transactional
    public Vehiculo crear(Vehiculo datos) {
        String matricula = datos.getMatricula().trim().toUpperCase();
        if (vehiculoRepository.existsByMatriculaIgnoreCase(matricula)) {
            throw ReglaNegocioException.conflicto("Ya existe un vehículo con la matrícula " + matricula);
        }
        Vehiculo v = new Vehiculo();
        v.setMatricula(matricula);
        v.setModelo(datos.getModelo());
        v.setKilometraje(datos.getKilometraje());
        v.setEstado(datos.getEstado());
        return vehiculoRepository.save(v);
    }

    /** Actualiza datos normalizados y evita alterar estado/kilometraje durante una asignación activa. */
    @Transactional
    public Vehiculo actualizar(Long id, Vehiculo datos) {
        Vehiculo v = obtener(id);
        String matricula = datos.getMatricula().trim().toUpperCase();

        vehiculoRepository.findByMatriculaIgnoreCase(matricula)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw ReglaNegocioException.conflicto("Ya existe un vehículo con la matrícula " + matricula);
                });

        boolean cambiaEstadoOKm = v.getEstado() != datos.getEstado()
                || !v.getKilometraje().equals(datos.getKilometraje());
        if (cambiaEstadoOKm
                && asignacionRepository.existsByVehiculoIdAndEstadoAsignacion(id, EstadoAsignacion.ACTIVA)) {
            throw ReglaNegocioException.conflicto(
                    "El vehículo tiene una asignación activa: su estado y kilometraje se gestionan al finalizarla");
        }

        v.setMatricula(matricula);
        v.setModelo(datos.getModelo());
        v.setKilometraje(datos.getKilometraje());
        v.setEstado(datos.getEstado());
        return vehiculoRepository.save(v);
    }

    /**
     * Elimina un vehículo. Solo se permite si está DISPONIBLE (no EN_RUTA ni TALLER).
     * Sus asignaciones pasadas se conservan con la matrícula copiada y sin enlace al vehículo.
     * Se exige DISPONIBLE y se fuerza el flush antes y después del borrado para respetar FKs.
     */
    @Transactional
    public void eliminar(Long id) {
        Vehiculo v = obtener(id);
        if (v.getEstado() != EstadoVehiculo.DISPONIBLE) {
            throw ReglaNegocioException.conflicto("No se puede eliminar el vehículo " + v.getMatricula()
                    + " mientras está en estado " + v.getEstado()
                    + ": primero hay que finalizar su asignación para que vuelva a DISPONIBLE");
        }
        if (asignacionRepository.existsByVehiculoIdAndEstadoAsignacion(id, EstadoAsignacion.ACTIVA)) {
            throw ReglaNegocioException.conflicto("No se puede eliminar un vehículo con una asignación activa");
        }
        List<Asignacion> historico = asignacionRepository.findByVehiculoId(id);
        for (Asignacion a : historico) {
            a.setVehiculoMatricula(v.getMatricula());
            a.setVehiculo(null);
        }
        asignacionRepository.saveAllAndFlush(historico);
        vehiculoRepository.delete(v);
        vehiculoRepository.flush();
    }
}
