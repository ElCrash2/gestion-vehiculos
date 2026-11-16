package com.ejemplo.vehiculos.service;

import com.ejemplo.vehiculos.model.*;
import com.ejemplo.vehiculos.repository.AsignacionRepository;
import com.ejemplo.vehiculos.repository.ConductorRepository;
import com.ejemplo.vehiculos.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de las reglas principales de {@link AsignacionService}.
 * Los repositorios se simulan: estos tests no levantan Spring ni comprueban
 * consultas SQL, transacciones reales o serialización HTTP.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsignacionServiceTest {

    /** Repositorio simulado; cada prueba prepara solo las respuestas que necesita. */
    @Mock AsignacionRepository asignacionRepository;
    @Mock VehiculoRepository vehiculoRepository;
    @Mock ConductorRepository conductorRepository;

    AsignacionService service;
    Conductor conductor;
    Vehiculo vehiculo;

    /** Construye el servicio y datos base para los escenarios independientes. */
    @BeforeEach
    void preparar() {
        service = new AsignacionService(asignacionRepository, vehiculoRepository, conductorRepository);

        conductor = new Conductor();
        conductor.setId(1L);
        conductor.setNombre("Ana");
        conductor.setDepartamento(Departamento.LOGISTICA);

        vehiculo = new Vehiculo();
        vehiculo.setId(10L);
        vehiculo.setMatricula("1234ABC");
        vehiculo.setKilometraje(5000);
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);

        when(conductorRepository.findById(1L)).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(i -> i.getArgument(0));
        when(asignacionRepository.save(any(Asignacion.class))).thenAnswer(i -> i.getArgument(0));
    }

    /** Atajo para crear una asignación con una ventana de fechas válida. */
    private Asignacion crear(Motivo motivo) {
        LocalDateTime salida = LocalDateTime.now();
        return service.crear(1L, 10L, salida, salida.plusDays(1), motivo);
    }

    @Test
    void crearConMotivoRutaPoneVehiculoEnRutaYCopiaKm() {
        Asignacion a = crear(Motivo.RUTA);

        assertEquals(EstadoVehiculo.EN_RUTA, vehiculo.getEstado());
        assertEquals(5000, a.getKmSalida());
        assertEquals(EstadoAsignacion.ACTIVA, a.getEstadoAsignacion());
    }

    @Test
    void crearConMotivoTallerPoneVehiculoEnTaller() {
        crear(Motivo.TALLER);

        assertEquals(EstadoVehiculo.TALLER, vehiculo.getEstado());
    }

    @Test
    void noSePuedeAsignarVehiculoEnRuta() {
        vehiculo.setEstado(EstadoVehiculo.EN_RUTA);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> crear(Motivo.RUTA));
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void noSePuedeAsignarVehiculoEnTaller() {
        vehiculo.setEstado(EstadoVehiculo.TALLER);

        assertThrows(ReglaNegocioException.class, () -> crear(Motivo.RUTA));
    }

    @Test
    void conductorConRutaActivaNoPuedeTenerOtraRuta() {
        when(asignacionRepository.existsByConductorIdAndEstadoAsignacionAndMotivo(
                1L, EstadoAsignacion.ACTIVA, Motivo.RUTA)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> crear(Motivo.RUTA));
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(EstadoVehiculo.DISPONIBLE, vehiculo.getEstado());
    }

    @Test
    void conductorConRutaActivaPuedeLlevarOtroVehiculoAlTaller() {
        when(asignacionRepository.existsByConductorIdAndEstadoAsignacionAndMotivo(
                1L, EstadoAsignacion.ACTIVA, Motivo.RUTA)).thenReturn(true);

        crear(Motivo.TALLER);

        assertEquals(EstadoVehiculo.TALLER, vehiculo.getEstado());
    }

    @Test
    void fechaPrevistaAnteriorALaSalidaSeRechaza() {
        LocalDateTime salida = LocalDateTime.now();

        assertThrows(ReglaNegocioException.class,
                () -> service.crear(1L, 10L, salida, salida.minusHours(1), Motivo.RUTA));
        assertEquals(EstadoVehiculo.DISPONIBLE, vehiculo.getEstado());
    }

    @Test
    void finalizarActualizaKilometrajeYLiberaVehiculo() {
        Asignacion activa = crear(Motivo.RUTA);
        activa.setId(100L);
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(activa));

        Asignacion cerrada = service.finalizar(100L, 5240, null);

        assertEquals(EstadoAsignacion.FINALIZADA, cerrada.getEstadoAsignacion());
        assertEquals(5240, cerrada.getKmRegreso());
        assertNotNull(cerrada.getFechaRegresoReal());
        assertEquals(5240, vehiculo.getKilometraje());
        assertEquals(EstadoVehiculo.DISPONIBLE, vehiculo.getEstado());
    }

    @Test
    void finalizarConKmMenorQueSalidaSeRechaza() {
        Asignacion activa = crear(Motivo.RUTA);
        activa.setId(100L);
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(activa));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.finalizar(100L, 4999, null));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(EstadoVehiculo.EN_RUTA, vehiculo.getEstado());
        assertEquals(5000, vehiculo.getKilometraje());
    }

    @Test
    void finalizarConKmIgualAlDeSalidaEsValido() {
        Asignacion activa = crear(Motivo.TALLER);
        activa.setId(100L);
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(activa));

        Asignacion cerrada = service.finalizar(100L, 5000, null);

        assertEquals(EstadoAsignacion.FINALIZADA, cerrada.getEstadoAsignacion());
        assertEquals(EstadoVehiculo.DISPONIBLE, vehiculo.getEstado());
    }

    @Test
    void noSePuedeFinalizarDosVeces() {
        Asignacion activa = crear(Motivo.RUTA);
        activa.setId(100L);
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(activa));

        service.finalizar(100L, 5100, null);

        assertThrows(ReglaNegocioException.class, () -> service.finalizar(100L, 5200, null));
    }
}
