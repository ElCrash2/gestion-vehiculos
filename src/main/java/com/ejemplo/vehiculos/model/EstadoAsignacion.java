package com.ejemplo.vehiculos.model;

/** Estados del ciclo de vida de una asignación, persistidos por nombre en SQL. */
public enum EstadoAsignacion {
    /** Registro abierto; no tiene datos de regreso. */
    ACTIVA,
    /** Registro cerrado; debe incluir fecha y kilometraje de regreso. */
    FINALIZADA
}
