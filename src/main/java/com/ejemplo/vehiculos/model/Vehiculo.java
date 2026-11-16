package com.ejemplo.vehiculos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Vehículo persistido en {@code vehiculos}.
 *
 * La matrícula es única y se normaliza a mayúsculas en {@code VehiculoService}.
 * El kilometraje no puede ser negativo y el estado se almacena como nombre de
 * enum. Al cambiar una columna o estado, revisar {@code db/schema.sql} para
 * conservar la coherencia de los CHECK y la restricción UNIQUE.
 */
@Entity
@Table(name = "vehiculos")
public class Vehiculo {

    /** Clave primaria autogenerada. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Matrícula requerida y única, máximo 20 caracteres. */
    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String matricula;

    /** Modelo descriptivo opcional, máximo 100 caracteres. */
    @Size(max = 100)
    @Column(length = 100)
    private String modelo;

    /** Odómetro actual; obligatorio y no negativo. */
    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer kilometraje = 0;

    /** Situación operativa almacenada como cadena (DISPONIBLE, EN_RUTA o TALLER). */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVehiculo estado = EstadoVehiculo.DISPONIBLE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getKilometraje() { return kilometraje; }
    public void setKilometraje(Integer kilometraje) { this.kilometraje = kilometraje; }

    public EstadoVehiculo getEstado() { return estado; }
    public void setEstado(EstadoVehiculo estado) { this.estado = estado; }
}
