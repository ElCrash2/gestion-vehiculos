package com.ejemplo.vehiculos.service;

import org.springframework.http.HttpStatus;

/**
 * Señala una operación que viola una regla del dominio y conserva el HTTP
 * apropiado para la respuesta REST. El advice global la serializa como JSON.
 */
public class ReglaNegocioException extends RuntimeException {

    /** Estado REST que debe acompañar el error. */
    private final HttpStatus status;

    /** Solo los métodos de fábrica eligen el estado y crean estas excepciones. */
    private ReglaNegocioException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    /** 409: el estado actual de los datos impide la operación. */
    public static ReglaNegocioException conflicto(String mensaje) {
        return new ReglaNegocioException(HttpStatus.CONFLICT, mensaje);
    }

    /** 400: los datos enviados no son válidos. */
    public static ReglaNegocioException invalida(String mensaje) {
        return new ReglaNegocioException(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** Estado HTTP asociado al error. */
    public HttpStatus getStatus() {
        return status;
    }
}
