package com.ejemplo.vehiculos.model;

/**
 * Roles de autorización. Spring Security los expone como ROLE_ADMIN y
 * ROLE_CONDUCTOR; la base de datos guarda los nombres de estas constantes.
 * Si se añade un rol, revisar {@code SecurityConfig} y el CHECK SQL de usuarios.
 */
public enum Rol {
    ADMIN,
    CONDUCTOR
}
