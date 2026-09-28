package com.empresa.asiscontrol.supervisores.service;

import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import java.time.LocalDate;

public interface CrewMembershipCoordinator {
    void closeIfIncompatible(Long employeeId, Long newSupervisorId, LocalDate effectiveDate,
                             AsisUserPrincipal actor, RequestMetadata metadata);
}
