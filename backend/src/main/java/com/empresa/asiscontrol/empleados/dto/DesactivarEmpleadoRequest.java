package com.empresa.asiscontrol.empleados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DesactivarEmpleadoRequest(LocalDate fechaInactivacion,
        @NotBlank @Size(max = 500) String motivo) {}
