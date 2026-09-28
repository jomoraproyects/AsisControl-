package com.empresa.asiscontrol.supervisores.service;

import com.empresa.asiscontrol.supervisores.repository.SupervisorEmpleadoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupervisorScopeService {
    private final SupervisorEmpleadoRepository repository;
    private final Clock clock;
    public SupervisorScopeService(SupervisorEmpleadoRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public boolean canSee(Long supervisorId, Long employeeId) {
        return repository.countActive(supervisorId, employeeId, LocalDate.now(clock)) > 0;
    }
    @Transactional(readOnly = true)
    public List<Long> assignedIds(Long supervisorId) {
        return repository.assignedEmployeeIds(supervisorId, LocalDate.now(clock));
    }
    @Transactional(readOnly = true)
    public boolean assignedOn(Long supervisorId, Long employeeId, LocalDate date) {
        return repository.countActive(supervisorId, employeeId, date) > 0;
    }
}
