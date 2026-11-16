package com.ejemplo.vehiculos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Cuenta de acceso persistida en {@code usuarios}.
 *
 * El password contiene un hash BCrypt, nunca texto plano, y se excluye de
 * la serialización JSON. {@code conductor_id} es opcional: una cuenta ADMIN
 * puede no estar asociada a un conductor. Las reglas de roles y borrado están
 * en los servicios/controladores y en {@code db/schema.sql}.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    /** Clave primaria autogenerada. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de acceso único y obligatorio, máximo 50 caracteres. */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Hash BCrypt obligatorio; {@link JsonIgnore} evita exponerlo por la API. */
    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    /** Rol guardado como texto, restringido en SQL a ADMIN o CONDUCTOR. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Conductor opcional; usa carga diferida y FK {@code conductor_id}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conductor_id")
    private Conductor conductor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public Conductor getConductor() { return conductor; }
    public void setConductor(Conductor conductor) { this.conductor = conductor; }
}
