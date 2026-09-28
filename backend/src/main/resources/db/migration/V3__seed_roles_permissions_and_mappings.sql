INSERT INTO roles (id, codigo, nombre, protegido, requiere_mfa, creado_en) VALUES
    (1, 'SUPER_ADMIN', 'Super administrador', TRUE, TRUE, UTC_TIMESTAMP(6)),
    (2, 'RRHH', 'Talento humano', TRUE, TRUE, UTC_TIMESTAMP(6)),
    (3, 'SUPERVISOR', 'Supervisor', TRUE, FALSE, UTC_TIMESTAMP(6)),
    (4, 'CONDUCTOR', 'Conductor', TRUE, FALSE, UTC_TIMESTAMP(6));

INSERT INTO permisos (id, codigo, descripcion, creado_en) VALUES
    (1, 'USUARIO_CREAR', 'Crear usuarios', UTC_TIMESTAMP(6)),
    (2, 'USUARIO_MODIFICAR', 'Modificar usuarios y restablecer contraseñas', UTC_TIMESTAMP(6)),
    (3, 'USUARIO_DESACTIVAR', 'Desactivar usuarios', UTC_TIMESTAMP(6)),
    (4, 'ROL_ASIGNAR', 'Asignar o revocar roles base', UTC_TIMESTAMP(6)),
    (5, 'EMPLEADO_CREAR', 'Crear empleados', UTC_TIMESTAMP(6)),
    (6, 'EMPLEADO_MODIFICAR', 'Modificar empleados', UTC_TIMESTAMP(6)),
    (7, 'EMPLEADO_DESACTIVAR', 'Desactivar empleados', UTC_TIMESTAMP(6)),
    (8, 'EMPLEADO_VER_TODOS', 'Consultar todos los empleados', UTC_TIMESTAMP(6)),
    (9, 'EMPLEADO_VER_ASIGNADOS', 'Consultar empleados asignados', UTC_TIMESTAMP(6)),
    (10, 'EMPLEADO_VER_PROPIO', 'Consultar el empleado propio', UTC_TIMESTAMP(6)),
    (11, 'AREA_GESTIONAR', 'Gestionar áreas', UTC_TIMESTAMP(6)),
    (12, 'CARGO_GESTIONAR', 'Gestionar cargos', UTC_TIMESTAMP(6)),
    (13, 'SUPERVISOR_ASIGNAR', 'Asignar supervisores', UTC_TIMESTAMP(6)),
    (14, 'CUADRILLA_GESTIONAR', 'Gestionar cuadrillas', UTC_TIMESTAMP(6)),
    (15, 'CUADRILLA_VER_PROPIA', 'Consultar cuadrilla propia', UTC_TIMESTAMP(6)),
    (16, 'HORARIO_GESTIONAR', 'Gestionar horarios', UTC_TIMESTAMP(6)),
    (17, 'UBICACION_GESTIONAR', 'Gestionar ubicaciones de trabajo', UTC_TIMESTAMP(6)),
    (18, 'BIOMETRIA_ENROLAR', 'Enrolar biometría', UTC_TIMESTAMP(6)),
    (19, 'BIOMETRIA_REENROLAR', 'Re-enrolar biometría', UTC_TIMESTAMP(6)),
    (20, 'BIOMETRIA_ELIMINAR', 'Eliminar perfil biométrico', UTC_TIMESTAMP(6)),
    (21, 'BIOMETRIA_VERIFICAR_ASIGNADOS', 'Verificar biometría de empleados asignados', UTC_TIMESTAMP(6)),
    (22, 'BIOMETRIA_VERIFICAR_PROPIA', 'Verificar biometría propia', UTC_TIMESTAMP(6)),
    (23, 'SESION_MARCACION_GESTIONAR_PROPIA', 'Gestionar sesiones de marcación propias', UTC_TIMESTAMP(6)),
    (24, 'MARCACION_CREAR_ASIGNADOS', 'Crear marcaciones para empleados asignados', UTC_TIMESTAMP(6)),
    (25, 'MARCACION_CREAR_PROPIA', 'Crear marcación propia', UTC_TIMESTAMP(6)),
    (26, 'MARCACION_VER_TODAS', 'Consultar todas las marcaciones', UTC_TIMESTAMP(6)),
    (27, 'MARCACION_VER_ASIGNADAS', 'Consultar marcaciones asignadas', UTC_TIMESTAMP(6)),
    (28, 'MARCACION_VER_PROPIAS', 'Consultar marcaciones propias', UTC_TIMESTAMP(6)),
    (29, 'MARCACION_EXCEPCIONAL', 'Crear marcación excepcional', UTC_TIMESTAMP(6)),
    (30, 'NOVEDAD_CREAR', 'Crear novedades', UTC_TIMESTAMP(6)),
    (31, 'NOVEDAD_CREAR_ASIGNADOS', 'Crear novedades para empleados asignados', UTC_TIMESTAMP(6)),
    (32, 'NOVEDAD_REVISAR', 'Aprobar o rechazar novedades', UTC_TIMESTAMP(6)),
    (33, 'JORNADA_VER_TODAS', 'Consultar todas las jornadas', UTC_TIMESTAMP(6)),
    (34, 'JORNADA_VER_ASIGNADAS', 'Consultar jornadas asignadas', UTC_TIMESTAMP(6)),
    (35, 'JORNADA_VER_PROPIAS', 'Consultar jornadas propias', UTC_TIMESTAMP(6)),
    (36, 'HORA_EXTRA_REVISAR', 'Revisar horas extra', UTC_TIMESTAMP(6)),
    (37, 'PERIODO_VER', 'Consultar períodos de nómina', UTC_TIMESTAMP(6)),
    (38, 'PERIODO_CERRAR', 'Cerrar períodos de nómina', UTC_TIMESTAMP(6)),
    (39, 'REPORTE_EXPORTAR_ORION', 'Exportar reporte Orión', UTC_TIMESTAMP(6)),
    (40, 'AUDITORIA_VER_TOTAL', 'Consultar auditoría completa', UTC_TIMESTAMP(6)),
    (41, 'AUDITORIA_VER_RRHH', 'Consultar auditoría relacionada con RRHH', UTC_TIMESTAMP(6));

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p WHERE r.codigo = 'SUPER_ADMIN';

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo = 'RRHH' AND p.codigo IN (
    'EMPLEADO_CREAR', 'EMPLEADO_MODIFICAR', 'EMPLEADO_DESACTIVAR', 'EMPLEADO_VER_TODOS',
    'SUPERVISOR_ASIGNAR', 'HORARIO_GESTIONAR', 'BIOMETRIA_ENROLAR', 'BIOMETRIA_REENROLAR',
    'MARCACION_VER_TODAS', 'MARCACION_EXCEPCIONAL', 'NOVEDAD_CREAR', 'NOVEDAD_REVISAR',
    'JORNADA_VER_TODAS', 'HORA_EXTRA_REVISAR', 'PERIODO_VER', 'PERIODO_CERRAR',
    'REPORTE_EXPORTAR_ORION', 'AUDITORIA_VER_RRHH'
);

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo = 'SUPERVISOR' AND p.codigo IN (
    'EMPLEADO_VER_ASIGNADOS', 'CUADRILLA_VER_PROPIA', 'BIOMETRIA_VERIFICAR_ASIGNADOS',
    'SESION_MARCACION_GESTIONAR_PROPIA', 'MARCACION_CREAR_ASIGNADOS',
    'MARCACION_VER_ASIGNADAS', 'NOVEDAD_CREAR_ASIGNADOS', 'JORNADA_VER_ASIGNADAS'
);

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.codigo = 'CONDUCTOR' AND p.codigo IN (
    'EMPLEADO_VER_PROPIO', 'BIOMETRIA_VERIFICAR_PROPIA', 'MARCACION_CREAR_PROPIA',
    'MARCACION_VER_PROPIAS', 'JORNADA_VER_PROPIAS'
);

