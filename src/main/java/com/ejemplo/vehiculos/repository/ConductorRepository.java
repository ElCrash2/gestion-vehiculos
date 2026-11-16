package com.ejemplo.vehiculos.repository;

import com.ejemplo.vehiculos.model.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;

/** Operaciones CRUD JPA sobre la tabla {@code conductores}. */
public interface ConductorRepository extends JpaRepository<Conductor, Long> {
}
