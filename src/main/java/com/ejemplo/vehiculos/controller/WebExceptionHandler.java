package com.ejemplo.vehiculos.controller;

import com.ejemplo.vehiculos.service.RecursoNoEncontradoException;
import com.ejemplo.vehiculos.service.ReglaNegocioException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Presenta errores de las acciones HTML como mensajes en vez de respuestas JSON. */
@ControllerAdvice(assignableTypes = WebController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebExceptionHandler {

    @ExceptionHandler(ReglaNegocioException.class)
    public String reglaNegocio(ReglaNegocioException exception, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", exception.getMessage());
        return "redirect:/app";
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String recursoNoEncontrado(RecursoNoEncontradoException exception, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", exception.getMessage());
        return "redirect:/app";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String integridad(RedirectAttributes redirect) {
        redirect.addFlashAttribute("error",
                "La operación incumple una restricción de datos o existe un registro relacionado.");
        return "redirect:/app";
    }
}
