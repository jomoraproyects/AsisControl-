package com.empresa.asiscontrol.empleados.dto;

import jakarta.validation.constraints.*;

public record UpdateEmpleadoRequest(@NotBlank @Size(max = 120) String nombres,
        @NotBlank @Size(max = 120) String apellidos,
        @NotNull Long areaId, @NotNull Long cargoId) {}
