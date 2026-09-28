CREATE TABLE areas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500) NULL,
    estado VARCHAR(10) NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    actualizado_en DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_areas PRIMARY KEY (id),
    CONSTRAINT uk_areas_codigo UNIQUE (codigo),
    CONSTRAINT ck_areas_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cargos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500) NULL,
    estado VARCHAR(10) NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    actualizado_en DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cargos PRIMARY KEY (id),
    CONSTRAINT uk_cargos_codigo UNIQUE (codigo),
    CONSTRAINT ck_cargos_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE empleados (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo_documento VARCHAR(20) NOT NULL,
    numero_documento_normalizado VARCHAR(30) NOT NULL,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    codigo_empleado VARCHAR(40) NOT NULL,
    area_id BIGINT NOT NULL,
    cargo_id BIGINT NOT NULL,
    tipo_empleado VARCHAR(20) NOT NULL,
    estado VARCHAR(10) NOT NULL,
    fecha_ingreso DATE NOT NULL,
    fecha_inactivacion DATE NULL,
    motivo_inactivacion VARCHAR(500) NULL,
    creado_en DATETIME(6) NOT NULL,
    actualizado_en DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_empleados PRIMARY KEY (id),
    CONSTRAINT uk_empleados_documento UNIQUE (tipo_documento, numero_documento_normalizado),
    CONSTRAINT uk_empleados_codigo UNIQUE (codigo_empleado),
    CONSTRAINT fk_empleados_area FOREIGN KEY (area_id) REFERENCES areas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_empleados_cargo FOREIGN KEY (cargo_id) REFERENCES cargos (id) ON DELETE RESTRICT,
    CONSTRAINT ck_empleados_tipo CHECK (tipo_empleado IN ('OPERATIVO', 'SUPERVISOR', 'CONDUCTOR')),
    CONSTRAINT ck_empleados_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_empleados_inactivacion CHECK (
        (estado = 'ACTIVO' AND fecha_inactivacion IS NULL AND motivo_inactivacion IS NULL)
        OR (estado = 'INACTIVO' AND fecha_inactivacion IS NOT NULL AND motivo_inactivacion IS NOT NULL)
    ),
    INDEX ix_empleados_area (area_id),
    INDEX ix_empleados_cargo (cargo_id),
    INDEX ix_empleados_tipo_estado (tipo_empleado, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE usuarios
    ADD COLUMN empleado_id BIGINT NULL,
    ADD CONSTRAINT uk_usuarios_empleado UNIQUE (empleado_id),
    ADD CONSTRAINT fk_usuarios_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id) ON DELETE RESTRICT;
