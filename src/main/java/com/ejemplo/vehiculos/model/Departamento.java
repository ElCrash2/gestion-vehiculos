package com.ejemplo.vehiculos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;

/**
 * Departamentos admitidos por el dominio.
 *
 * El nombre constante Java (por ejemplo, {@code LOGISTICA}) no es el valor
 * persistido: SQL y JSON utilizan la etiqueta con acentos. Si se añade o
 * renombra un departamento, actualizar también {@code DepartamentoConverter},
 * {@code db/schema.sql} (CHECK de {@code conductores.departamento}) y los
 * consumidores del API.
 */
public enum Departamento {
    LOGISTICA("Logística"),
    COMERCIAL("Comercial"),
    ADMINISTRACION("Administración"),
    DIRECCION("Dirección");

    /** Texto legible usado como representación externa y valor de base de datos. */
    private final String etiqueta;

    Departamento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Expone la etiqueta legible al serializar este enum a JSON. */
    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Acepta etiqueta o nombre constante, sin distinguir mayúsculas ni acentos. */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Departamento deTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String buscado = sinAcentos(texto.trim());
        for (Departamento d : values()) {
            if (sinAcentos(d.etiqueta).equalsIgnoreCase(buscado) || d.name().equalsIgnoreCase(buscado)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Departamento no válido: " + texto
                + ". Valores admitidos: Logística, Comercial, Administración, Dirección");
    }

    /** Normaliza el texto para comparar entradas con o sin tildes. */
    private static String sinAcentos(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
