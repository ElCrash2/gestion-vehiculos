package com.ejemplo.vehiculos.controller;

import com.ejemplo.vehiculos.service.RecursoNoEncontradoException;
import com.ejemplo.vehiculos.service.ReglaNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traductor centralizado de excepciones a respuestas JSON de la API.
 *
 * Los controladores y servicios lanzan excepciones semánticas; este advice
 * asigna sus códigos HTTP y un cuerpo común. Si se incorpora una excepción
 * nueva con respuesta pública específica, añadir aquí su {@code @ExceptionHandler}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Forma común de los errores: instante, código, nombre del estado y detalle. */
    public record ErrorResponse(LocalDateTime fecha, int estado, String error, String mensaje) { }

    /** Traduce ausencia de recurso a 404. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Utiliza el estado HTTP definido por la regla de negocio (400 o 409). */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException ex) {
        return respuesta(ex.getStatus(), ex.getMessage());
    }

    /** Resume errores Bean Validation por nombre de campo y los responde como 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        String detalle = campos.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, "Datos no válidos -> " + detalle);
    }

    /** Convierte JSON inválido o enums/deserialización no reconocidos a 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getMostSpecificCause();
        return respuesta(HttpStatus.BAD_REQUEST, "Cuerpo de la petición no válido: " + causa.getMessage());
    }

    /** Responde 400 cuando un parámetro de ruta o consulta no se puede convertir. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Valor no válido para el parámetro '" + ex.getName() + "'");
    }

    /** Convierte conflictos de claves/relaciones impuestos por la base de datos a 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        return respuesta(HttpStatus.CONFLICT,
                "La operación viola una restricción de integridad (registro relacionado o valor duplicado)");
    }

    /** Construye el cuerpo JSON común para un estado y mensaje dados. */
    private ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje));
    }
}
