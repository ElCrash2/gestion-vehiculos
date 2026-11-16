package com.ejemplo.vehiculos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Motivo de una asignación. Las etiquetas, no los nombres Java, se guardan en
 * {@code asignaciones.motivo} y se exponen en JSON. Añadir valores requiere
 * actualizar {@code MotivoConverter} (si cambia el formato) y el CHECK del SQL.
 */
public enum Motivo {
    RUTA("Ruta"),
    TALLER("Taller");

    /** Etiqueta pública y persistida del motivo. */
    private final String etiqueta;

    Motivo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Representación emitida al serializar el enum a JSON. */
    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Acepta etiqueta o constante Java ignorando mayúsculas y espacios externos. */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Motivo deTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String buscado = texto.trim();
        for (Motivo m : values()) {
            if (m.etiqueta.equalsIgnoreCase(buscado) || m.name().equalsIgnoreCase(buscado)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Motivo no válido: " + texto + ". Valores admitidos: Ruta, Taller");
    }
}
