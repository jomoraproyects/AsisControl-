package com.empresa.asiscontrol.supervisores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CerrarSupervisorRequest(@NotNull LocalDate vigenteHasta,
        @NotBlank @Size(max = 500) String motivoCambio) {}
