CREATE TABLE supervisor_empleado (
    id BIGINT NOT NULL AUTO_INCREMENT,
    supervisor_id BIGINT NOT NULL,
    empleado_id BIGINT NOT NULL,
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE NULL,
    asignado_por BIGINT NOT NULL,
    motivo_cambio VARCHAR(500) NULL,
    creado_en DATETIME(6) NOT NULL,
    empleado_vigente_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN vigente_hasta IS NULL THEN empleado_id ELSE NULL END
    ) STORED,
    CONSTRAINT pk_supervisor_empleado PRIMARY KEY (id),
    CONSTRAINT uk_supervisor_empleado_vigente UNIQUE (empleado_vigente_id),
    CONSTRAINT fk_supervisor_empleado_supervisor FOREIGN KEY (supervisor_id) REFERENCES empleados (id) ON DELETE RESTRICT,
    CONSTRAINT fk_supervisor_empleado_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id) ON DELETE RESTRICT,
    CONSTRAINT fk_supervisor_empleado_actor FOREIGN KEY (asignado_por) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT ck_supervisor_empleado_distinto CHECK (supervisor_id <> empleado_id),
    CONSTRAINT ck_supervisor_empleado_intervalo CHECK (vigente_hasta IS NULL OR vigente_hasta > vigente_desde),
    INDEX ix_supervisor_empleado_alcance (supervisor_id, vigente_desde, vigente_hasta),
    INDEX ix_supervisor_empleado_historia (empleado_id, vigente_desde, vigente_hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cuadrillas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500) NULL,
    estado VARCHAR(10) NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    actualizado_en DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cuadrillas PRIMARY KEY (id),
    CONSTRAINT uk_cuadrillas_codigo UNIQUE (codigo),
    CONSTRAINT ck_cuadrillas_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cuadrilla_supervisor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cuadrilla_id BIGINT NOT NULL,
    supervisor_id BIGINT NOT NULL,
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE NULL,
    asignado_por BIGINT NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    cuadrilla_vigente_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN vigente_hasta IS NULL THEN cuadrilla_id ELSE NULL END
    ) STORED,
    CONSTRAINT pk_cuadrilla_supervisor PRIMARY KEY (id),
    CONSTRAINT uk_cuadrilla_supervisor_vigente UNIQUE (cuadrilla_vigente_id),
    CONSTRAINT fk_cuadrilla_supervisor_cuadrilla FOREIGN KEY (cuadrilla_id) REFERENCES cuadrillas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_cuadrilla_supervisor_supervisor FOREIGN KEY (supervisor_id) REFERENCES empleados (id) ON DELETE RESTRICT,
    CONSTRAINT fk_cuadrilla_supervisor_actor FOREIGN KEY (asignado_por) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT ck_cuadrilla_supervisor_intervalo CHECK (vigente_hasta IS NULL OR vigente_hasta > vigente_desde),
    INDEX ix_cuadrilla_supervisor_historia (cuadrilla_id, vigente_desde, vigente_hasta),
    INDEX ix_cuadrilla_supervisor_supervisor (supervisor_id, vigente_desde, vigente_hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cuadrilla_empleado (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cuadrilla_id BIGINT NOT NULL,
    empleado_id BIGINT NOT NULL,
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE NULL,
    asignado_por BIGINT NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    empleado_vigente_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN vigente_hasta IS NULL THEN empleado_id ELSE NULL END
    ) STORED,
    CONSTRAINT pk_cuadrilla_empleado PRIMARY KEY (id),
    CONSTRAINT uk_cuadrilla_empleado_vigente UNIQUE (empleado_vigente_id),
    CONSTRAINT fk_cuadrilla_empleado_cuadrilla FOREIGN KEY (cuadrilla_id) REFERENCES cuadrillas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_cuadrilla_empleado_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id) ON DELETE RESTRICT,
    CONSTRAINT fk_cuadrilla_empleado_actor FOREIGN KEY (asignado_por) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT ck_cuadrilla_empleado_intervalo CHECK (vigente_hasta IS NULL OR vigente_hasta > vigente_desde),
    INDEX ix_cuadrilla_empleado_historia (empleado_id, vigente_desde, vigente_hasta),
    INDEX ix_cuadrilla_empleado_cuadrilla (cuadrilla_id, vigente_desde, vigente_hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
