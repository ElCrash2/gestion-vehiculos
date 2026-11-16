package com.ejemplo.vehiculos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de Spring Boot. La anotación activa la autoconfiguración y
 * el escaneo de componentes bajo el paquete {@code com.ejemplo.vehiculos}.
 */
@SpringBootApplication
public class VehiculosApplication {

    /** Arranca el contexto de Spring y el servidor web integrado. */
    public static void main(String[] args) {
        SpringApplication.run(VehiculosApplication.class, args);
    }
}
