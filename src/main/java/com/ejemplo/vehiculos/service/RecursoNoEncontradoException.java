package com.ejemplo.vehiculos.service;

/**
 * Excepción de dominio para búsquedas por id sin resultado.
 * {@code GlobalExceptionHandler} la transforma en una respuesta HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    /** Construye un mensaje identificando el tipo de recurso y el id ausente. */
    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " con id " + id + " no encontrado");
    }
}
