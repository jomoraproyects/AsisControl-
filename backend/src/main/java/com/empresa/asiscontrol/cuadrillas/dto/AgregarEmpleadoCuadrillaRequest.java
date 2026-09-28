package com.empresa.asiscontrol.cuadrillas.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AgregarEmpleadoCuadrillaRequest(@NotNull Long empleadoId,
        @NotNull LocalDate vigenteDesde) {}
