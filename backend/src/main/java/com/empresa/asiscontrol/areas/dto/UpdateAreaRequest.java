package com.empresa.asiscontrol.areas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAreaRequest(@NotBlank @Size(max = 120) String nombre,
                                @Size(max = 500) String descripcion) {}
