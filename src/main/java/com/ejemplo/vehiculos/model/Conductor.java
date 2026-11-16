package com.ejemplo.vehiculos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ficha de un conductor, persistida en {@code conductores}.
 *
 * Las validaciones anotadas se aplican cuando la entidad se valida, por
 * ejemplo al recibirla en un controlador con {@code @Valid}. Para cambios de
 * esquema, mantener sincronizados los límites/valores con {@code db/schema.sql};
 * {@code departamento} usa etiquetas mediante {@link DepartamentoConverter}.
 */
@Entity
@Table(name = "conductores")
public class Conductor {

    /** Clave primaria autogenerada. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre obligatorio, máximo 100 caracteres ({@code nombre}). */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    /** Departamento obligatorio, almacenado con su etiqueta traducida. */
    @NotNull
    @Convert(converter = DepartamentoConverter.class)
    @Column(nullable = false, length = 30)
    private Departamento departamento;

    /** Teléfono opcional, limitado a 20 caracteres. */
    @Size(max = 20)
    @Column(length = 20)
    private String telefono;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Departamento getDepartamento() { return departamento; }
    public void setDepartamento(Departamento departamento) { this.departamento = departamento; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
