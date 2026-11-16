package com.ejemplo.vehiculos.repository;

import com.ejemplo.vehiculos.model.Asignacion;
import com.ejemplo.vehiculos.model.EstadoAsignacion;
import com.ejemplo.vehiculos.model.Motivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso JPA a {@code asignaciones}. Spring Data deriva las consultas del
 * nombre del método; los nombres de propiedades deben corresponder al modelo
 * Java, no a los nombres físicos de columnas.
 */
public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    /** Busca por estado; admite ACTIVA o FINALIZADA. */
    List<Asignacion> findByEstadoAsignacion(EstadoAsignacion estado);

    /** Carga el histórico asociado al id del conductor, incluido el histórico que aún conserva FK. */
    List<Asignacion> findByConductorId(Long conductorId);

    /** Carga el histórico asociado al id del vehículo. */
    List<Asignacion> findByVehiculoId(Long vehiculoId);

    /** Busca asignaciones de un estado cuya fecha real de regreso sea anterior al límite. */
    List<Asignacion> findByEstadoAsignacionAndFechaRegresoRealBefore(EstadoAsignacion estado, LocalDateTime limite);

    /** Comprueba si el conductor ya tiene asignación con la combinación indicada. */
    boolean existsByConductorIdAndEstadoAsignacionAndMotivo(Long conductorId, EstadoAsignacion estado, Motivo motivo);

    /** Comprueba si existe una asignación del vehículo en el estado indicado. */
    boolean existsByVehiculoIdAndEstadoAsignacion(Long vehiculoId, EstadoAsignacion estado);
}
