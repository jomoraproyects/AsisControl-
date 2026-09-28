package com.empresa.asiscontrol.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.empresa.asiscontrol.areas.dto.AreaRequest;
import com.empresa.asiscontrol.areas.service.AreaService;
import com.empresa.asiscontrol.auditoria.entity.AuditAction;
import com.empresa.asiscontrol.auditoria.repository.AuditEventRepository;
import com.empresa.asiscontrol.auth.security.AsisUserDetailsService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.cargos.dto.CargoRequest;
import com.empresa.asiscontrol.cargos.service.CargoService;
import com.empresa.asiscontrol.cuadrillas.dto.*;
import com.empresa.asiscontrol.cuadrillas.repository.*;
import com.empresa.asiscontrol.cuadrillas.service.CrewService;
import com.empresa.asiscontrol.empleados.dto.*;
import com.empresa.asiscontrol.empleados.entity.*;
import com.empresa.asiscontrol.empleados.service.EmpleadoService;
import com.empresa.asiscontrol.roles.Roles;
import com.empresa.asiscontrol.shared.exception.ConflictException;
import com.empresa.asiscontrol.shared.exception.NotFoundException;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.supervisores.dto.AsignarSupervisorRequest;
import com.empresa.asiscontrol.supervisores.repository.SupervisorEmpleadoRepository;
import com.empresa.asiscontrol.supervisores.service.SupervisorAssignmentService;
import com.empresa.asiscontrol.usuarios.dto.CreateUserRequest;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
import com.empresa.asiscontrol.usuarios.service.UserService;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Phase2IntegrationTest extends MySqlIntegrationTest {
    private static final RequestMetadata META = new RequestMetadata("127.0.0.1", "test", "phase2-test");
    private static final String PASSWORD = "FaseDos!2026#Clave";
    @Autowired AreaService areas;
    @Autowired CargoService cargos;
    @Autowired EmpleadoService empleados;
    @Autowired SupervisorAssignmentService supervisores;
    @Autowired SupervisorEmpleadoRepository supervisorRows;
    @Autowired CrewService cuadrillas;
    @Autowired CuadrillaEmpleadoRepository crewRows;
    @Autowired UserService userService;
    @Autowired AsisUserDetailsService details;
    @Autowired AuditEventRepository audit;

    @Test
    void empleadoUnicoEditableDesactivableYAuditado() {
        Fixture f = fixture();
        long id = employee(f, TipoEmpleado.OPERATIVO);
        assertThatThrownBy(() -> empleados.create(new CreateEmpleadoRequest(TipoDocumento.CC,
                f.documento(id), "Otro", "Nombre", unique(), f.area, f.cargo,
                TipoEmpleado.OPERATIVO, today().minusDays(100)), f.actor, META))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> empleados.create(new CreateEmpleadoRequest(TipoDocumento.CE,
                unique(), "Otro", "Nombre", f.codigo(id), f.area, f.cargo,
                TipoEmpleado.OPERATIVO, today().minusDays(100)), f.actor, META))
                .isInstanceOf(ConflictException.class);
        assertThat(empleados.update(id, new UpdateEmpleadoRequest("Ana", "Pérez", f.area, f.cargo),
                f.actor, META).nombreCompleto()).isEqualTo("Ana Pérez");
        empleados.deactivate(id, new DesactivarEmpleadoRequest(today(), "Terminación"), f.actor, META);
        assertThat(empleados.get(id, f.actor).estado().name()).isEqualTo("INACTIVO");
        assertThat(audit.countByAccion(AuditAction.CREAR_EMPLEADO)).isPositive();
        assertThat(audit.countByAccion(AuditAction.DESACTIVAR_EMPLEADO)).isPositive();
    }

    @Test
    void asignacionValidaCambioEHistorialConRechazos() {
        Fixture f = fixture();
        long first = employee(f, TipoEmpleado.SUPERVISOR);
        long second = employee(f, TipoEmpleado.SUPERVISOR);
        long worker = employee(f, TipoEmpleado.OPERATIVO);
        long anotherWorker = employee(f, TipoEmpleado.OPERATIVO);
        assertThatThrownBy(() -> supervisores.assign(worker, new AsignarSupervisorRequest(
                999999L, today(), null), f.actor, META)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> supervisores.assign(worker, new AsignarSupervisorRequest(
                worker, today(), null), f.actor, META)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> supervisores.assign(worker, new AsignarSupervisorRequest(
                anotherWorker, today(), null), f.actor, META)).isInstanceOf(ConflictException.class);
        supervisores.assign(worker, new AsignarSupervisorRequest(first, today().minusDays(1), null), f.actor, META);
        supervisores.assign(worker, new AsignarSupervisorRequest(second, today(), "Cambio"), f.actor, META);
        assertThat(supervisorRows.findAll().stream().filter(r -> r.getEmpleadoId().equals(worker))).hasSize(2);
        assertThat(supervisorRows.findByEmpleadoIdAndVigenteHastaIsNull(worker).orElseThrow()
                .getSupervisorId()).isEqualTo(second);
        assertThatThrownBy(() -> supervisores.assign(worker, new AsignarSupervisorRequest(
                first, today(), null), f.actor, META)).isInstanceOf(ConflictException.class);
        assertThat(audit.countByAccion(AuditAction.ASIGNAR_SUPERVISOR)).isPositive();
    }

    @Test
    void cuadrillaExigeSupervisorCompatibleYUnaPertenenciaVigente() {
        Fixture f = fixture();
        long leader = employee(f, TipoEmpleado.SUPERVISOR);
        long otherLeader = employee(f, TipoEmpleado.SUPERVISOR);
        long worker = employee(f, TipoEmpleado.OPERATIVO);
        supervisores.assign(worker, new AsignarSupervisorRequest(leader, today().minusDays(1), null), f.actor, META);
        long crew = cuadrillas.create(new CreateCuadrillaRequest(unique(), "Turno A", null), f.actor, META).id();
        long otherCrew = cuadrillas.create(new CreateCuadrillaRequest(unique(), "Turno B", null), f.actor, META).id();
        cuadrillas.assignSupervisor(crew, new AsignarSupervisorCuadrillaRequest(leader, today().minusDays(1)), f.actor, META);
        cuadrillas.assignSupervisor(otherCrew, new AsignarSupervisorCuadrillaRequest(leader, today().minusDays(1)), f.actor, META);
        cuadrillas.addEmployee(crew, new AgregarEmpleadoCuadrillaRequest(worker, today().minusDays(1)), f.actor, META);
        assertThatThrownBy(() -> cuadrillas.addEmployee(otherCrew,
                new AgregarEmpleadoCuadrillaRequest(worker, today()), f.actor, META))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> cuadrillas.assignSupervisor(crew,
                new AsignarSupervisorCuadrillaRequest(otherLeader, today()), f.actor, META))
                .isInstanceOf(ConflictException.class);
        supervisores.assign(worker, new AsignarSupervisorRequest(otherLeader, today(), "Traslado"), f.actor, META);
        assertThat(crewRows.findByEmpleadoIdAndVigenteHastaIsNull(worker)).isEmpty();
    }

    @Test
    void rolOperativoRequiereEmpleadoDelTipoCorrecto() {
        Fixture f = fixture();
        long worker = employee(f, TipoEmpleado.OPERATIVO);
        long supervisor = employee(f, TipoEmpleado.SUPERVISOR);
        assertThatThrownBy(() -> userService.create(new CreateUserRequest(unique(), null, PASSWORD,
                Set.of(Roles.SUPERVISOR), null), f.actor, META)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> userService.create(new CreateUserRequest(unique(), null, PASSWORD,
                Set.of(Roles.SUPERVISOR), worker), f.actor, META)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> userService.create(new CreateUserRequest(unique(), null, PASSWORD,
                Set.of(Roles.CONDUCTOR), supervisor), f.actor, META)).isInstanceOf(ConflictException.class);
        Usuario unlinked = createReadyUser("unlinked", PASSWORD, Roles.RRHH);
        assertThatThrownBy(() -> userService.assignRole(unlinked.getPublicId(), Roles.SUPERVISOR,
                f.actor, META)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> userService.assignRole(unlinked.getPublicId(), Roles.CONDUCTOR,
                f.actor, META)).isInstanceOf(ConflictException.class);
        assertThat(userService.create(new CreateUserRequest(unique(), null, PASSWORD,
                Set.of(Roles.SUPERVISOR), supervisor), f.actor, META).empleadoId()).isEqualTo(supervisor);
    }

    @Test
    void scopeSupervisorConductorYRrhhNoExponeEmpleadoAjeno() throws Exception {
        Fixture f = fixture();
        long supA = employee(f, TipoEmpleado.SUPERVISOR);
        long supB = employee(f, TipoEmpleado.SUPERVISOR);
        long workerA = employee(f, TipoEmpleado.OPERATIVO);
        long workerB = employee(f, TipoEmpleado.OPERATIVO);
        long driver = employee(f, TipoEmpleado.CONDUCTOR);
        supervisores.assign(workerA, new AsignarSupervisorRequest(supA, today(), null), f.actor, META);
        supervisores.assign(workerB, new AsignarSupervisorRequest(supB, today(), null), f.actor, META);
        AsisUserPrincipal principalA = linkedUser(Roles.SUPERVISOR, supA);
        AsisUserPrincipal principalDriver = linkedUser(Roles.CONDUCTOR, driver);
        AsisUserPrincipal rrhh = principal(createReadyUser("rrhh", PASSWORD, Roles.RRHH));
        mockMvc.perform(get("/api/v1/empleados/{id}", workerA).with(user(principalA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/empleados/{id}", workerB).with(user(principalA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/empleados/me").with(user(principalDriver)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/empleados/{id}", workerA).with(user(principalDriver)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/empleados/{id}", workerB).with(user(rrhh)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/areas").with(user(rrhh)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/areas").with(user(principalA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void concurrentesNoAbrenDosSupervisores() throws Exception {
        Fixture f = fixture();
        long first = employee(f, TipoEmpleado.SUPERVISOR);
        long second = employee(f, TipoEmpleado.SUPERVISOR);
        long worker = employee(f, TipoEmpleado.OPERATIVO);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> a = pool.submit(() -> attemptAssignment(start, worker, first, f.actor));
            Future<Boolean> b = pool.submit(() -> attemptAssignment(start, worker, second, f.actor));
            start.countDown();
            assertThat(a.get(20, TimeUnit.SECONDS) ^ b.get(20, TimeUnit.SECONDS)).isTrue();
            assertThat(supervisorRows.findAll().stream().filter(r -> r.getEmpleadoId().equals(worker)
                    && r.getVigenteHasta() == null)).hasSize(1);
        } finally { pool.shutdownNow(); }
    }

    @Test
    void concurrentesNoAbrenDosCuadrillasParaUnEmpleado() throws Exception {
        Fixture f = fixture();
        long supervisor = employee(f, TipoEmpleado.SUPERVISOR);
        long worker = employee(f, TipoEmpleado.OPERATIVO);
        supervisores.assign(worker, new AsignarSupervisorRequest(supervisor, today(), null), f.actor, META);
        long first = cuadrillas.create(new CreateCuadrillaRequest(unique(), "A", null), f.actor, META).id();
        long second = cuadrillas.create(new CreateCuadrillaRequest(unique(), "B", null), f.actor, META).id();
        cuadrillas.assignSupervisor(first, new AsignarSupervisorCuadrillaRequest(supervisor, today()), f.actor, META);
        cuadrillas.assignSupervisor(second, new AsignarSupervisorCuadrillaRequest(supervisor, today()), f.actor, META);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> a = pool.submit(() -> attemptCrewAdd(start, first, worker, f.actor));
            Future<Boolean> b = pool.submit(() -> attemptCrewAdd(start, second, worker, f.actor));
            start.countDown();
            assertThat(a.get(20, TimeUnit.SECONDS) ^ b.get(20, TimeUnit.SECONDS)).isTrue();
            assertThat(crewRows.findByEmpleadoIdAndVigenteHastaIsNull(worker)).isPresent();
        } finally { pool.shutdownNow(); }
    }

    private boolean attemptCrewAdd(CountDownLatch start, long crew, long worker, AsisUserPrincipal actor)
            throws InterruptedException {
        start.await();
        try {
            cuadrillas.addEmployee(crew, new AgregarEmpleadoCuadrillaRequest(worker, today()), actor, META);
            return true;
        } catch (ConflictException | org.springframework.dao.DataIntegrityViolationException ex) {
            return false;
        }
    }

    private boolean attemptAssignment(CountDownLatch start, long worker, long supervisor, AsisUserPrincipal actor)
            throws InterruptedException {
        start.await();
        try {
            supervisores.assign(worker, new AsignarSupervisorRequest(supervisor, today(), null), actor, META);
            return true;
        } catch (ConflictException | org.springframework.dao.DataIntegrityViolationException ex) {
            return false;
        }
    }
    private Fixture fixture() {
        AsisUserPrincipal actor = principal(createReadyUser("admin_p2", PASSWORD, Roles.SUPER_ADMIN));
        Long area = areas.create(new AreaRequest(unique(), "Operaciones", null), actor, META).id();
        Long cargo = cargos.create(new CargoRequest(unique(), "Operario", null), actor, META).id();
        return new Fixture(actor, area, cargo);
    }
    private long employee(Fixture f, TipoEmpleado type) {
        String document = unique();
        String code = unique();
        var e = empleados.create(new CreateEmpleadoRequest(TipoDocumento.CC, document,
                "Nombre", "Prueba", code, f.area, f.cargo, type, today().minusDays(100)), f.actor, META);
        f.lastDocument = document; f.lastCode = code;
        return e.id();
    }
    private AsisUserPrincipal linkedUser(String role, long employeeId) {
        Usuario user = createReadyUser("linked", PASSWORD, role);
        transactions.executeWithoutResult(tx -> usuarioRepository.findById(user.getId())
                .orElseThrow().vincularEmpleado(employeeId, clock.instant()));
        return principal(user);
    }
    private AsisUserPrincipal principal(Usuario user) {
        return (AsisUserPrincipal) details.loadUserByUsername(user.getNombreUsuario());
    }
    private LocalDate today() { return LocalDate.now(clock); }
    private String unique() { return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12); }
    private static final class Fixture {
        final AsisUserPrincipal actor; final Long area; final Long cargo;
        String lastDocument; String lastCode;
        Fixture(AsisUserPrincipal actor, Long area, Long cargo) {
            this.actor = actor; this.area = area; this.cargo = cargo;
        }
        String documento(long ignored) { return lastDocument; }
        String codigo(long ignored) { return lastCode; }
    }
}
