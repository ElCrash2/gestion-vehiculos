package com.ejemplo.vehiculos.model;

/**
 * Estados operativos persistidos por nombre en {@code vehiculos.estado}.
 * Al añadir estados, revisar validaciones y el CHECK de {@code db/schema.sql}.
 */
public enum EstadoVehiculo {
    /** Vehículo sin asignación en curso, disponible para asignar. */
    DISPONIBLE,
    /** Vehículo utilizado actualmente en una ruta. */
    EN_RUTA,
    /** Vehículo asignado a una actividad de taller. */
    TALLER
}
