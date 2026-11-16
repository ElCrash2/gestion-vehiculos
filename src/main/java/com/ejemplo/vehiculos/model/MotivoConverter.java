package com.ejemplo.vehiculos.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Persiste {@link Motivo} usando las etiquetas Ruta/Taller de la base de datos. */
@Converter
public class MotivoConverter implements AttributeConverter<Motivo, String> {

    /** Convierte el valor Java a su etiqueta SQL y conserva valores nulos. */
    @Override
    public String convertToDatabaseColumn(Motivo motivo) {
        return motivo == null ? null : motivo.getEtiqueta();
    }

    /** Convierte la etiqueta SQL al enum correspondiente. */
    @Override
    public Motivo convertToEntityAttribute(String valor) {
        return valor == null ? null : Motivo.deTexto(valor);
    }
}
