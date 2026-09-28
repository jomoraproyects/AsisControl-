package com.empresa.asiscontrol.cuadrillas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCuadrillaRequest(@NotBlank @Size(max = 120) String nombre,
        @Size(max = 500) String descripcion) {}
