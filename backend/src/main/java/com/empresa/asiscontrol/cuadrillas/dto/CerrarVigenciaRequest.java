package com.empresa.asiscontrol.cuadrillas.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CerrarVigenciaRequest(@NotNull LocalDate vigenteHasta) {}
