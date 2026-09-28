package com.empresa.asiscontrol.cuadrillas.service;

import com.empresa.asiscontrol.cuadrillas.repository.CuadrillaSupervisorRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrewLeadershipService {
    private final CuadrillaSupervisorRepository repository;
    private final Clock clock;
    public CrewLeadershipService(CuadrillaSupervisorRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public boolean leadsAny(Long supervisorId) {
        return !repository.crewsLedOn(supervisorId, LocalDate.now(clock)).isEmpty();
    }
}
