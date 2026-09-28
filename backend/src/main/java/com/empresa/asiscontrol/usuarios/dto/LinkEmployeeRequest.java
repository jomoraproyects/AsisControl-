package com.empresa.asiscontrol.usuarios.dto;

import jakarta.validation.constraints.NotNull;

public record LinkEmployeeRequest(@NotNull Long empleadoId) {}
