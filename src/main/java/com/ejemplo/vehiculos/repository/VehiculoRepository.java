package com.ejemplo.vehiculos.repository;

import com.ejemplo.vehiculos.model.EstadoVehiculo;
import com.ejemplo.vehiculos.model.Vehiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Acceso JPA a {@code vehiculos}. Las búsquedas sin consulta explícita se
 * derivan de propiedades del modelo; el método de bloqueo se define en JPQL.
 */
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    /** Comprueba matrícula ignorando mayúsculas/minúsculas. */
    boolean existsByMatriculaIgnoreCase(String matricula);

    /** Busca vehículo por matrícula ignorando mayúsculas/minúsculas. */
    Optional<Vehiculo> findByMatriculaIgnoreCase(String matricula);

    /** Filtra el catálogo por estado actual. */
    List<Vehiculo> findByEstado(EstadoVehiculo estado);

    /** Bloqueo pesimista para evitar dos asignaciones simultáneas sobre el mismo vehículo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehiculo v where v.id = :id")
    Optional<Vehiculo> findByIdForUpdate(@Param("id") Long id);
}
