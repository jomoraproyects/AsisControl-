package com.empresa.asiscontrol.supervisores.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AsignarSupervisorRequest(@NotNull Long supervisorId, @NotNull LocalDate vigenteDesde,
        @Size(max = 500) String motivoCambio) {}
