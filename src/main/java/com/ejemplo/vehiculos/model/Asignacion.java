package com.ejemplo.vehiculos.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entidad del histórico de usos de vehículos ({@code asignaciones}).
 *
 * Las claves foráneas a conductor y vehículo son opcionales para permitir
 * eliminar esas fichas sin perder el histórico. Las columnas de texto
 * {@code conductor_nombre} y {@code vehiculo_matricula} conservan una copia.
 *
 * Al modificar este modelo, revisar también {@code db/schema.sql}: allí se
 * declaran restricciones CHECK e índices parciales que JPA no expresa aquí.
 * Las columnas sin {@code name} explícito usan la estrategia de nombres de
 * Hibernate (por ejemplo, {@code motivo}).
 */
@Entity
@Table(name = "asignaciones")
public class Asignacion {

    /** Identificador generado por la base de datos (BIGSERIAL en PostgreSQL). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relación opcional con {@code conductores}; la columna FK es {@code conductor_id}.
     * Queda nula al eliminar el conductor; el nombre histórico se conserva abajo.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conductor_id")
    private Conductor conductor;

    /**
     * Relación opcional con {@code vehiculos}; la columna FK es {@code vehiculo_id}.
     * Queda nula al eliminar el vehículo; la matrícula histórica se conserva abajo.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    /** Copia histórica del nombre del conductor; corresponde a {@code conductor_nombre}. */
    @Column(name = "conductor_nombre", nullable = false, length = 100)
    private String conductorNombre;

    /** Copia histórica de la matrícula; corresponde a {@code vehiculo_matricula}. */
    @Column(name = "vehiculo_matricula", nullable = false, length = 20)
    private String vehiculoMatricula;

    /** Fecha y hora de inicio; obligatoria en la tabla. */
    @Column(name = "fecha_salida", nullable = false)
    private LocalDateTime fechaSalida;

    /** Fecha/hora prevista de devolución; debe ser igual o posterior a la salida. */
    @Column(name = "fecha_prevista_regreso", nullable = false)
    private LocalDateTime fechaPrevistaRegreso;

    /** Fecha/hora efectiva de devolución; permanece nula mientras está activa. */
    @Column(name = "fecha_regreso_real")
    private LocalDateTime fechaRegresoReal;

    /** Kilometraje del vehículo al iniciar la asignación. */
    @Column(name = "km_salida", nullable = false)
    private Integer kmSalida;

    /** Kilometraje al finalizar; nulo mientras la asignación está activa. */
    @Column(name = "km_regreso")
    private Integer kmRegreso;

    /** Estado persistido como texto, no como ordinal numérico. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_asignacion", nullable = false, length = 20)
    private EstadoAsignacion estadoAsignacion = EstadoAsignacion.ACTIVA;

    /** Motivo persistido con etiquetas legibles mediante {@link MotivoConverter}. */
    @Convert(converter = MotivoConverter.class)
    @Column(nullable = false, length = 10)
    private Motivo motivo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConductorNombre() { return conductorNombre; }
    public void setConductorNombre(String conductorNombre) { this.conductorNombre = conductorNombre; }

    public String getVehiculoMatricula() { return vehiculoMatricula; }
    public void setVehiculoMatricula(String vehiculoMatricula) { this.vehiculoMatricula = vehiculoMatricula; }

    public Conductor getConductor() { return conductor; }
    public void setConductor(Conductor conductor) { this.conductor = conductor; }

    public Vehiculo getVehiculo() { return vehiculo; }
    public void setVehiculo(Vehiculo vehiculo) { this.vehiculo = vehiculo; }

    public LocalDateTime getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDateTime fechaSalida) { this.fechaSalida = fechaSalida; }

    public LocalDateTime getFechaPrevistaRegreso() { return fechaPrevistaRegreso; }
    public void setFechaPrevistaRegreso(LocalDateTime fechaPrevistaRegreso) { this.fechaPrevistaRegreso = fechaPrevistaRegreso; }

    public LocalDateTime getFechaRegresoReal() { return fechaRegresoReal; }
    public void setFechaRegresoReal(LocalDateTime fechaRegresoReal) { this.fechaRegresoReal = fechaRegresoReal; }

    public Integer getKmSalida() { return kmSalida; }
    public void setKmSalida(Integer kmSalida) { this.kmSalida = kmSalida; }

    public Integer getKmRegreso() { return kmRegreso; }
    public void setKmRegreso(Integer kmRegreso) { this.kmRegreso = kmRegreso; }

    public EstadoAsignacion getEstadoAsignacion() { return estadoAsignacion; }
    public void setEstadoAsignacion(EstadoAsignacion estadoAsignacion) { this.estadoAsignacion = estadoAsignacion; }

    public Motivo getMotivo() { return motivo; }
    public void setMotivo(Motivo motivo) { this.motivo = motivo; }
}
