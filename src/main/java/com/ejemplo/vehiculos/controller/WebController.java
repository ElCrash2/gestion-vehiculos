package com.ejemplo.vehiculos.controller;

import com.ejemplo.vehiculos.model.Asignacion;
import com.ejemplo.vehiculos.model.Conductor;
import com.ejemplo.vehiculos.model.Departamento;
import com.ejemplo.vehiculos.model.EstadoAsignacion;
import com.ejemplo.vehiculos.model.EstadoVehiculo;
import com.ejemplo.vehiculos.model.Motivo;
import com.ejemplo.vehiculos.model.Rol;
import com.ejemplo.vehiculos.model.Usuario;
import com.ejemplo.vehiculos.model.Vehiculo;
import com.ejemplo.vehiculos.service.AsignacionService;
import com.ejemplo.vehiculos.service.ConductorService;
import com.ejemplo.vehiculos.service.UsuarioService;
import com.ejemplo.vehiculos.service.VehiculoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Locale;
/** Rutas HTML del cliente Thymeleaf. La API JSON permanece en los controllers REST. */
@Controller
public class WebController {

    private final VehiculoService vehiculoService;
    private final ConductorService conductorService;
    private final AsignacionService asignacionService;
    private final UsuarioService usuarioService;

    public WebController(VehiculoService vehiculoService, ConductorService conductorService,
                         AsignacionService asignacionService, UsuarioService usuarioService) {
        this.vehiculoService = vehiculoService;
        this.conductorService = conductorService;
        this.asignacionService = asignacionService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/app";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/app")
    public String inicio(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("admin", esAdmin(authentication));
        model.addAttribute("totalVehiculos", vehiculoService.listar(null).size());
        model.addAttribute("totalConductores", conductorService.listar().size());
        model.addAttribute("asignacionesActivas",
                asignacionService.listar(EstadoAsignacion.ACTIVA).size());
        return "index";
    }

    @GetMapping("/vehiculos")
    public String vehiculos(@RequestParam(required = false) String q,
                            @RequestParam(required = false) EstadoVehiculo estado, Model model) {
        String consulta = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        model.addAttribute("vehiculos", vehiculoService.listar(estado).stream()
                .filter(vehiculo -> consulta.isEmpty()
                        || vehiculo.getMatricula().toLowerCase(Locale.ROOT).contains(consulta)
                        || (vehiculo.getModelo() != null
                        && vehiculo.getModelo().toLowerCase(Locale.ROOT).contains(consulta)))
                .toList());
        model.addAttribute("consulta", q);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("estadosVehiculo", EstadoVehiculo.values());
        return "vehiculos/lista";
    }

    @GetMapping("/vehiculos/nuevo")
    @PreAuthorize("hasRole('ADMIN')")
    public String nuevoVehiculo(Model model) {
        model.addAttribute("vehiculo", new Vehiculo());
        prepararFormularioVehiculo(model);
        return "vehiculos/formulario";
    }

    @GetMapping("/vehiculos/{id}/editar")
    @PreAuthorize("hasRole('ADMIN')")
    public String editarVehiculo(@PathVariable Long id, Model model) {
        model.addAttribute("vehiculo", vehiculoService.obtener(id));
        prepararFormularioVehiculo(model);
        return "vehiculos/formulario";
    }

    @PostMapping("/vehiculos")
    @PreAuthorize("hasRole('ADMIN')")
    public String crearVehiculo(@Valid @ModelAttribute("vehiculo") Vehiculo vehiculo,
                                BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararFormularioVehiculo(model);
            return "vehiculos/formulario";
        }
        vehiculoService.crear(vehiculo);
        redirect.addFlashAttribute("mensaje", "Vehículo creado.");
        return "redirect:/vehiculos";
    }

    @PostMapping("/vehiculos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String actualizarVehiculo(@PathVariable Long id,
                                    @Valid @ModelAttribute("vehiculo") Vehiculo vehiculo,
                                    BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            vehiculo.setId(id);
            prepararFormularioVehiculo(model);
            return "vehiculos/formulario";
        }
        vehiculoService.actualizar(id, vehiculo);
        redirect.addFlashAttribute("mensaje", "Vehículo actualizado.");
        return "redirect:/vehiculos";
    }

    @PostMapping("/vehiculos/{id}/eliminar")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminarVehiculo(@PathVariable Long id, RedirectAttributes redirect) {
        vehiculoService.eliminar(id);
        redirect.addFlashAttribute("mensaje", "Vehículo eliminado.");
        return "redirect:/vehiculos";
    }

    @GetMapping("/conductores")
    public String conductores(@RequestParam(required = false) String q,
                              @RequestParam(required = false) Departamento departamento, Model model) {
        String consulta = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        model.addAttribute("conductores", conductorService.listar().stream()
                .filter(conductor -> consulta.isEmpty()
                        || conductor.getNombre().toLowerCase(Locale.ROOT).contains(consulta)
                        || (conductor.getTelefono() != null
                        && conductor.getTelefono().toLowerCase(Locale.ROOT).contains(consulta)))
                .filter(conductor -> departamento == null || conductor.getDepartamento() == departamento)
                .toList());
        model.addAttribute("consulta", q);
        model.addAttribute("departamentoSeleccionado", departamento);
        model.addAttribute("departamentos", Departamento.values());
        return "conductores/lista";
    }

    @GetMapping("/conductores/nuevo")
    @PreAuthorize("hasRole('ADMIN')")
    public String nuevoConductor(Model model) {
        model.addAttribute("conductor", new Conductor());
        prepararFormularioConductor(model);
        return "conductores/formulario";
    }

    @GetMapping("/conductores/{id}/editar")
    @PreAuthorize("hasRole('ADMIN')")
    public String editarConductor(@PathVariable Long id, Model model) {
        model.addAttribute("conductor", conductorService.obtener(id));
        prepararFormularioConductor(model);
        return "conductores/formulario";
    }

    @PostMapping("/conductores")
    @PreAuthorize("hasRole('ADMIN')")
    public String crearConductor(@Valid @ModelAttribute("conductor") Conductor conductor,
                                 BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararFormularioConductor(model);
            return "conductores/formulario";
        }
        conductorService.crear(conductor);
        redirect.addFlashAttribute("mensaje", "Conductor creado.");
        return "redirect:/conductores";
    }

    @PostMapping("/conductores/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String actualizarConductor(@PathVariable Long id,
                                      @Valid @ModelAttribute("conductor") Conductor conductor,
                                      BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            conductor.setId(id);
            prepararFormularioConductor(model);
            return "conductores/formulario";
        }
        conductorService.actualizar(id, conductor);
        redirect.addFlashAttribute("mensaje", "Conductor actualizado.");
        return "redirect:/conductores";
    }

    @PostMapping("/conductores/{id}/eliminar")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminarConductor(@PathVariable Long id, RedirectAttributes redirect) {
        conductorService.eliminar(id);
        redirect.addFlashAttribute("mensaje", "Conductor eliminado.");
        return "redirect:/conductores";
    }

    @GetMapping("/asignaciones")
    public String asignaciones(@RequestParam(required = false) EstadoAsignacion estado, Model model) {
        model.addAttribute("asignaciones", asignacionService.listar(estado));
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("estadosAsignacion", EstadoAsignacion.values());
        model.addAttribute("conductores", conductorService.listar());
        model.addAttribute("vehiculosDisponibles", vehiculoService.listar(EstadoVehiculo.DISPONIBLE));
        model.addAttribute("motivos", Motivo.values());
        model.addAttribute("retencionDias", asignacionService.getRetencionDias());
        model.addAttribute("asignacionForm", new AsignacionForm());
        model.addAttribute("ahora", LocalDateTime.now().withSecond(0).withNano(0));
        return "asignaciones/lista";
    }

    @PostMapping("/asignaciones")
    public String crearAsignacion(@Valid @ModelAttribute("asignacionForm") AsignacionForm form,
                                  BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargarFormularioAsignaciones(model, null);
            return "asignaciones/lista";
        }
        asignacionService.crear(form.getConductorId(), form.getVehiculoId(), form.getFechaSalida(),
                form.getFechaPrevistaRegreso(), form.getMotivo());
        redirect.addFlashAttribute("mensaje", "Asignación creada.");
        return "redirect:/asignaciones";
    }

    @PostMapping("/asignaciones/{id}/finalizar")
    public String finalizarAsignacion(@PathVariable Long id,
                                      @RequestParam Integer kmRegreso,
                                      @RequestParam(required = false) LocalDateTime fechaRegresoReal,
                                      RedirectAttributes redirect) {
        asignacionService.finalizar(id, kmRegreso, fechaRegresoReal);
        redirect.addFlashAttribute("mensaje", "Asignación finalizada.");
        return "redirect:/asignaciones";
    }

    @PostMapping("/asignaciones/{id}/eliminar")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminarAsignacion(@PathVariable Long id, RedirectAttributes redirect) {
        asignacionService.eliminar(id);
        redirect.addFlashAttribute("mensaje", "Asignación eliminada.");
        return "redirect:/asignaciones";
    }

    @PostMapping("/asignaciones/depurar")
    @PreAuthorize("hasRole('ADMIN')")
    public String depurarAsignaciones(RedirectAttributes redirect) {
        int eliminadas = asignacionService.depurarAntiguas();
        redirect.addFlashAttribute("mensaje", "Asignaciones eliminadas: " + eliminadas + ".");
        return "redirect:/asignaciones";
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public String usuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("conductores", conductorService.listar());
        model.addAttribute("roles", Rol.values());
        model.addAttribute("usuarioForm", new UsuarioForm());
        return "usuarios/lista";
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public String crearUsuario(@Valid @ModelAttribute("usuarioForm") UsuarioForm form,
                               BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargarFormularioUsuarios(model);
            return "usuarios/lista";
        }
        usuarioService.crear(form.getUsername(), form.getPassword(), form.getRol(), form.getConductorId());
        redirect.addFlashAttribute("mensaje", "Usuario creado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String actualizarUsuario(@PathVariable Long id, @ModelAttribute UsuarioForm form,
                                    Authentication authentication, RedirectAttributes redirect) {
        usuarioService.actualizar(id, form.getPassword(), form.getRol(), form.getConductorId(),
                authentication.getName());
        redirect.addFlashAttribute("mensaje", "Usuario actualizado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/eliminar")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminarUsuario(@PathVariable Long id, Authentication authentication,
                                  RedirectAttributes redirect) {
        usuarioService.eliminar(id, authentication.getName());
        redirect.addFlashAttribute("mensaje", "Usuario eliminado.");
        return "redirect:/usuarios";
    }

    @ModelAttribute("admin")
    public boolean admin(Authentication authentication) {
        return authentication != null && esAdmin(authentication);
    }

    private void prepararFormularioVehiculo(Model model) {
        model.addAttribute("estadosVehiculo", EstadoVehiculo.values());
    }

    private void prepararFormularioConductor(Model model) {
        model.addAttribute("departamentos", Departamento.values());
    }

    private void cargarFormularioAsignaciones(Model model, EstadoAsignacion estado) {
        model.addAttribute("asignaciones", asignacionService.listar(estado));
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("estadosAsignacion", EstadoAsignacion.values());
        model.addAttribute("conductores", conductorService.listar());
        model.addAttribute("vehiculosDisponibles", vehiculoService.listar(EstadoVehiculo.DISPONIBLE));
        model.addAttribute("motivos", Motivo.values());
        model.addAttribute("retencionDias", asignacionService.getRetencionDias());
        model.addAttribute("ahora", LocalDateTime.now().withSecond(0).withNano(0));
    }

    private void cargarFormularioUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("conductores", conductorService.listar());
        model.addAttribute("roles", Rol.values());
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    /** Campos de asignación recibidos desde el formulario Thymeleaf. */
    public static class AsignacionForm {
        @NotNull
        private Long conductorId;
        @NotNull
        private Long vehiculoId;
        private LocalDateTime fechaSalida;
        @NotNull
        private LocalDateTime fechaPrevistaRegreso;
        @NotNull
        private Motivo motivo;

        public Long getConductorId() { return conductorId; }
        public void setConductorId(Long conductorId) { this.conductorId = conductorId; }
        public Long getVehiculoId() { return vehiculoId; }
        public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }
        public LocalDateTime getFechaSalida() { return fechaSalida; }
        public void setFechaSalida(LocalDateTime fechaSalida) { this.fechaSalida = fechaSalida; }
        public LocalDateTime getFechaPrevistaRegreso() { return fechaPrevistaRegreso; }
        public void setFechaPrevistaRegreso(LocalDateTime fechaPrevistaRegreso) {
            this.fechaPrevistaRegreso = fechaPrevistaRegreso;
        }
        public Motivo getMotivo() { return motivo; }
        public void setMotivo(Motivo motivo) { this.motivo = motivo; }
    }

    /** Campos para alta/edición de usuario; la contraseña vacía no modifica la existente. */
    public static class UsuarioForm {
        @NotBlank
        @Size(max = 50)
        private String username;
        @NotBlank
        @Size(min = 8, max = 72)
        private String password;
        @NotNull
        private Rol rol = Rol.CONDUCTOR;
        private Long conductorId;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Rol getRol() { return rol; }
        public void setRol(Rol rol) { this.rol = rol; }
        public Long getConductorId() { return conductorId; }
        public void setConductorId(Long conductorId) { this.conductorId = conductorId; }
    }
}
