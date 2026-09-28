CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    nombre_usuario VARCHAR(80) NOT NULL,
    nombre_usuario_normalizado VARCHAR(80) NOT NULL,
    correo VARCHAR(254) NULL,
    correo_normalizado VARCHAR(254) NULL,
    password_hash VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    debe_cambiar_password BOOLEAN NOT NULL DEFAULT TRUE,
    auth_version BIGINT NOT NULL DEFAULT 1,
    ultimo_login_en DATETIME(6) NULL,
    creado_en DATETIME(6) NOT NULL,
    actualizado_en DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_public_id UNIQUE (public_id),
    CONSTRAINT uk_usuarios_nombre_normalizado UNIQUE (nombre_usuario_normalizado),
    CONSTRAINT uk_usuarios_correo_normalizado UNIQUE (correo_normalizado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    protegido BOOLEAN NOT NULL DEFAULT TRUE,
    requiere_mfa BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en DATETIME(6) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE permisos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(80) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    CONSTRAINT pk_permisos PRIMARY KEY (id),
    CONSTRAINT uk_permisos_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE usuario_roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    asignado_en DATETIME(6) NOT NULL,
    asignado_por BIGINT NULL,
    revocado_en DATETIME(6) NULL,
    revocado_por BIGINT NULL,
    rol_vigente_id BIGINT GENERATED ALWAYS AS (CASE WHEN revocado_en IS NULL THEN rol_id ELSE NULL END) STORED,
    CONSTRAINT pk_usuario_roles PRIMARY KEY (id),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_usuario_roles_rol FOREIGN KEY (rol_id) REFERENCES roles (id),
    CONSTRAINT fk_usuario_roles_asignador FOREIGN KEY (asignado_por) REFERENCES usuarios (id),
    CONSTRAINT fk_usuario_roles_revocador FOREIGN KEY (revocado_por) REFERENCES usuarios (id),
    CONSTRAINT uk_usuario_roles_vigente UNIQUE (usuario_id, rol_vigente_id),
    INDEX ix_usuario_roles_vigencia (usuario_id, revocado_en),
    INDEX ix_usuario_roles_rol_vigencia (rol_id, revocado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rol_permisos (
    rol_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    CONSTRAINT pk_rol_permisos PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_rol_permisos_rol FOREIGN KEY (rol_id) REFERENCES roles (id),
    CONSTRAINT fk_rol_permisos_permiso FOREIGN KEY (permiso_id) REFERENCES permisos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE credenciales_mfa (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    secreto_cifrado VARCHAR(1024) NOT NULL,
    version_clave VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    enrolado_en DATETIME(6) NOT NULL,
    confirmado_en DATETIME(6) NULL,
    revocado_en DATETIME(6) NULL,
    credencial_vigente_usuario_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN estado IN ('PENDIENTE', 'ACTIVA') THEN usuario_id ELSE NULL END
    ) STORED,
    CONSTRAINT pk_credenciales_mfa PRIMARY KEY (id),
    CONSTRAINT uk_credenciales_mfa_public_id UNIQUE (public_id),
    CONSTRAINT uk_credenciales_mfa_vigente UNIQUE (credencial_vigente_usuario_id),
    CONSTRAINT fk_credenciales_mfa_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_credenciales_mfa_tipo CHECK (tipo IN ('TOTP')),
    CONSTRAINT ck_credenciales_mfa_estado CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'REVOCADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE codigos_recuperacion_mfa (
    id BIGINT NOT NULL AUTO_INCREMENT,
    credencial_mfa_id BIGINT NOT NULL,
    codigo_hash VARCHAR(255) NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    usado_en DATETIME(6) NULL,
    CONSTRAINT pk_codigos_recuperacion_mfa PRIMARY KEY (id),
    CONSTRAINT fk_codigos_recuperacion_credencial FOREIGN KEY (credencial_mfa_id) REFERENCES credenciales_mfa (id),
    INDEX ix_codigos_recuperacion_disponibles (credencial_mfa_id, usado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE auditoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    usuario_id BIGINT NULL,
    actor VARCHAR(100) NULL,
    accion VARCHAR(60) NOT NULL,
    resultado VARCHAR(20) NOT NULL,
    entidad VARCHAR(80) NULL,
    entidad_id VARCHAR(100) NULL,
    datos_antes JSON NULL,
    datos_despues JSON NULL,
    detalles JSON NULL,
    fecha_hora DATETIME(6) NOT NULL,
    ip VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    correlation_id VARCHAR(64) NOT NULL,
    CONSTRAINT pk_auditoria PRIMARY KEY (id),
    CONSTRAINT uk_auditoria_public_id UNIQUE (public_id),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    INDEX ix_auditoria_fecha (fecha_hora),
    INDEX ix_auditoria_usuario_fecha (usuario_id, fecha_hora),
    INDEX ix_auditoria_accion_fecha (accion, fecha_hora),
    INDEX ix_auditoria_entidad (entidad, entidad_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TRIGGER tr_auditoria_bloquear_update
BEFORE UPDATE ON auditoria
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoria es inmutable';

CREATE TRIGGER tr_auditoria_bloquear_delete
BEFORE DELETE ON auditoria
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoria es inmutable';

