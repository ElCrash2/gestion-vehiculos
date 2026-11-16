package com.ejemplo.vehiculos.service;

import com.ejemplo.vehiculos.model.Asignacion;
import com.ejemplo.vehiculos.model.Conductor;
import com.ejemplo.vehiculos.model.EstadoAsignacion;
import com.ejemplo.vehiculos.repository.AsignacionRepository;
import com.ejemplo.vehiculos.model.Usuario;
import com.ejemplo.vehiculos.repository.ConductorRepository;
import com.ejemplo.vehiculos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de conductores.
 *
 * Al borrar, conserva el nombre en cada asignación histórica, elimina el
 * enlace de esas asignaciones y desvincula las cuentas de usuario relacionadas.
 * La operación completa es transaccional para no dejar referencias a medias.
 */
@Service
public class ConductorService {

    /** CRUD de la tabla conductores. */
    private final ConductorRepository conductorRepository;
    /** Consulta y actualización de las referencias históricas. */
    private final AsignacionRepository asignacionRepository;
    /** Consulta y desvinculación de cuentas asociadas. */
    private final UsuarioRepository usuarioRepository;

    public ConductorService(ConductorRepository conductorRepository, AsignacionRepository asignacionRepository,
                            UsuarioRepository usuarioRepository) {
        this.conductorRepository = conductorRepository;
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Devuelve todas las fichas de conductor. */
    @Transactional(readOnly = true)
    public List<Conductor> listar() {
        return conductorRepository.findAll();
    }

    /** Obtiene conductor por id; traduce ausencia a {@link RecursoNoEncontradoException}. */
    @Transactional(readOnly = true)
    public Conductor obtener(Long id) {
        return conductorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Conductor", id));
    }

    /** Crea una nueva entidad copiando únicamente los campos editables. */
    @Transactional
    public Conductor crear(Conductor datos) {
        Conductor c = new Conductor();
        copiar(datos, c);
        return conductorRepository.save(c);
    }

    /** Actualiza los campos editables de la ficha existente. */
    @Transactional
    public Conductor actualizar(Long id, Conductor datos) {
        Conductor c = obtener(id);
        copiar(datos, c);
        return conductorRepository.save(c);
    }

    /**
     * Elimina un conductor. No se permite si tiene una asignación ACTIVA.
     * Sus asignaciones pasadas se conservan con el nombre copiado y sin enlace al conductor,
     * y los usuarios enlazados a él quedan sin conductor.
     */
    /** Borra solo si no hay asignaciones activas y conserva la información histórica. */
    @Transactional
    public void eliminar(Long id) {
        Conductor c = obtener(id);
        List<Asignacion> historico = asignacionRepository.findByConductorId(id);
        if (historico.stream().anyMatch(a -> a.getEstadoAsignacion() == EstadoAsignacion.ACTIVA)) {
            throw ReglaNegocioException.conflicto("No se puede eliminar a " + c.getNombre()
                    + " porque tiene una asignación activa: primero hay que finalizarla");
        }
        for (Asignacion a : historico) {
            a.setConductorNombre(c.getNombre());
            a.setConductor(null);
        }
        asignacionRepository.saveAllAndFlush(historico);

        List<Usuario> usuarios = usuarioRepository.findByConductorId(id);
        usuarios.forEach(u -> u.setConductor(null));
        usuarioRepository.saveAllAndFlush(usuarios);

        conductorRepository.delete(c);
        conductorRepository.flush();
    }

    /** Copia campos de negocio, evitando reemplazar el id de la entidad existente. */
    private void copiar(Conductor origen, Conductor destino) {
        destino.setNombre(origen.getNombre());
        destino.setDepartamento(origen.getDepartamento());
        destino.setTelefono(origen.getTelefono());
    }
}
