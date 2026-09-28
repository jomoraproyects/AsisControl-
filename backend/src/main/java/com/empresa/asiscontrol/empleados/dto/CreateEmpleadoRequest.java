package com.empresa.asiscontrol.empleados.dto;

import com.empresa.asiscontrol.empleados.entity.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateEmpleadoRequest(@NotNull TipoDocumento tipoDocumento,
        @NotBlank @Size(max = 30) String numeroDocumento,
        @NotBlank @Size(max = 120) String nombres, @NotBlank @Size(max = 120) String apellidos,
        @NotBlank @Size(max = 40) String codigoEmpleado,
        @NotNull Long areaId, @NotNull Long cargoId,
        @NotNull TipoEmpleado tipoEmpleado, @NotNull LocalDate fechaIngreso) {}
