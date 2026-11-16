package com.ejemplo.vehiculos.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convierte {@link Departamento} a la etiqueta almacenada en
 * {@code conductores.departamento}, en vez del nombre interno del enum.
 */
@Converter
public class DepartamentoConverter implements AttributeConverter<Departamento, String> {

    /** Convierte el enum a etiqueta SQL; conserva {@code null}. */
    @Override
    public String convertToDatabaseColumn(Departamento departamento) {
        return departamento == null ? null : departamento.getEtiqueta();
    }

    /** Reconstruye el enum desde la etiqueta SQL; los valores no reconocidos fallan explícitamente. */
    @Override
    public Departamento convertToEntityAttribute(String valor) {
        return valor == null ? null : Departamento.deTexto(valor);
    }
}
