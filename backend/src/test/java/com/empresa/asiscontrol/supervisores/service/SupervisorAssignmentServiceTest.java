package com.empresa.asiscontrol.supervisores.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.empresa.asiscontrol.auditoria.service.AuditService;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.empleados.service.EmployeeAccess;
import com.empresa.asiscontrol.shared.exception.ConflictException;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.supervisores.dto.AsignarSupervisorRequest;
import com.empresa.asiscontrol.supervisores.repository.SupervisorEmpleadoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SupervisorAssignmentServiceTest {
    private final SupervisorEmpleadoRepository repository = mock(SupervisorEmpleadoRepository.class);
    private final EmployeeAccess employees = mock(EmployeeAccess.class);
    private final CrewMembershipCoordinator crew = mock(CrewMembershipCoordinator.class);
    private final AuditService audit = mock(AuditService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC);
    private final SupervisorAssignmentService service = new SupervisorAssignmentService(
            repository, employees, crew, audit, clock);
    private final AsisUserPrincipal actor = mock(AsisUserPrincipal.class);
    private final RequestMetadata metadata = new RequestMetadata("127.0.0.1", "test", "test");

    @Test
    void impideAutoAsignacionAntesDePersistir() {
        assertThatThrownBy(() -> service.assign(9L,
                new AsignarSupervisorRequest(9L, LocalDate.of(2026, 9, 28), null), actor, metadata))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(repository, employees);
    }

    @Test
    void impideVigenciaFutura() {
        assertThatThrownBy(() -> service.assign(9L,
                new AsignarSupervisorRequest(10L, LocalDate.of(2026, 9, 29), null), actor, metadata))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(repository, employees);
    }
}
