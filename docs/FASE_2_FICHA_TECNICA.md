# Fase 2 — estructura organizacional

## Alcance y modelo

Se añaden al backend los módulos `areas`, `cargos`, `empleados`, `supervisores` y `cuadrillas`. No se añaden biometría, marcaciones, GPS, horarios ni frontend. `nombreCompleto` se deriva de nombres y apellidos.

| Migración | Tablas y cambios |
| --- | --- |
| `V4__organizational_catalogs_and_employees.sql` | `areas`, `cargos`, `empleados` y `usuarios.empleado_id` opcional, único y con FK. |
| `V5__historical_assignments_and_crews.sql` | `supervisor_empleado`, `cuadrillas`, `cuadrilla_supervisor` y `cuadrilla_empleado`. |

V1–V3 conservan su checksum. Las PK son `BIGINT`; los eventos técnicos usan `DATETIME(6)` UTC y las fechas laborales usan `DATE`. Todos los FK nuevos declaran `ON DELETE RESTRICT`. Tipos y estados se guardan como `VARCHAR`, con enums Java. Las relaciones históricas registran autor y fecha de creación. Las columnas generadas permiten un índice único sobre cada fila abierta y múltiples filas históricas cerradas.

## Invariantes y concurrencia

- Documento normalizado por NFKC, mayúsculas y eliminación de espacios, puntos y guiones; luego se exige `[A-Z0-9]{3,30}`. Son únicos `(tipoDocumento, numeroDocumentoNormalizado)` y `codigoEmpleado`.
- `SUPERVISOR` y `CONDUCTOR` requieren usuario vinculado a empleado activo del mismo tipo. El servicio valida el tipo; FK y UNIQUE impiden vínculos inexistentes o compartidos en la BD. MySQL no admite un `CHECK` entre tablas, por lo que la validación de tipo requiere la API y control de escrituras SQL directas.
- Las vigencias son `[vigenteDesde, vigenteHasta)`. El día final ya no pertenece a la relación. Se permiten fechas efectivas hasta hoy; se rechazan fechas futuras y un segundo cambio el mismo día de inicio. Los intervalos se comprueban antes de insertar.
- `supervisor_empleado` es la fuente de autorización. `cuadrilla_empleado` representa organización y no amplía alcance.
- Las transacciones bloquean la fila del empleado o cuadrilla con `PESSIMISTIC_WRITE`. Las consultas de solapamiento y los índices únicos de fila abierta protegen las carreras; conflictos de integridad o versión se expresan como Problem Details `409`.
- Cambiar el supervisor de un empleado cierra en la misma transacción una pertenencia incompatible a cuadrilla y audita ambas acciones. Cambiar líder de cuadrilla requiere que todos los integrantes ya pertenezcan al nuevo supervisor; para cerrar el líder o desactivar una cuadrilla se retiran antes sus integrantes.
- Desactivar empleado cierra su supervisor vigente y desactiva/revoca sesiones de su usuario vinculado. Un supervisor con empleados o cuadrillas debe transferirlos primero. Se protege al último `SUPER_ADMIN` también en esta ruta.
- Áreas y cargos utilizados no se eliminan físicamente. Un registro existente puede referir a un catálogo desactivado; nuevas asignaciones deben seleccionar catálogos activos.

## Endpoints

Todas las escrituras requieren sesión y CSRF. Las entradas usan Bean Validation y las salidas son DTOs.

| Recurso | Contrato |
| --- | --- |
| Áreas | `GET/POST /api/v1/areas`, `PATCH /api/v1/areas/{id}`, `POST /api/v1/areas/{id}/desactivar` |
| Cargos | `GET/POST /api/v1/cargos`, `PATCH /api/v1/cargos/{id}`, `POST /api/v1/cargos/{id}/desactivar` |
| Empleados | `GET/POST /api/v1/empleados`, `GET /api/v1/empleados/me`, `GET/PATCH /api/v1/empleados/{id}`, `POST /api/v1/empleados/{id}/desactivar` |
| Supervisores | `GET /api/v1/supervisores`, `GET /api/v1/supervisores/{id}/empleados`, `POST /api/v1/empleados/{id}/supervisor`, `POST /api/v1/empleados/{id}/supervisor/cerrar` |
| Cuadrillas | `GET/POST /api/v1/cuadrillas`, `GET/PATCH /api/v1/cuadrillas/{id}`, `POST /api/v1/cuadrillas/{id}/desactivar`, `POST /api/v1/cuadrillas/{id}/supervisor`, `POST /api/v1/cuadrillas/{id}/supervisor/cerrar`, `POST /api/v1/cuadrillas/{id}/empleados`, `POST /api/v1/cuadrillas/{id}/empleados/{empleadoId}/retirar` |
| Usuario–empleado | `POST/DELETE /api/v1/usuarios/{id}/empleado` |

Los PATCH modifican nombre y descripción en catálogos/cuadrillas, o nombres, apellidos, área y cargo en empleados. Código, documento y tipo son inmutables desde esta API. Los POST de asignación reciben `vigenteDesde`; los de cierre reciben `vigenteHasta`.

## Permisos y alcance

| Acción | SUPER_ADMIN | RRHH | SUPERVISOR | CONDUCTOR |
| --- | --- | --- | --- | --- |
| Administrar áreas/cargos | Sí | No | No | No |
| Consultar áreas/cargos | Sí | Sí | No | No |
| Gestionar empleados/supervisor | Sí | Sí | No | No |
| Consultar empleados | Todos | Todos | Asignados vigentes | Perfil propio mínimo |
| Gestionar cuadrillas | Sí | No | No | No |
| Consultar cuadrillas | Todas | No | Propias vigentes | No |
| Vincular usuario y roles | Sí | No | No | No |

Las consultas de supervisor derivan su `empleado_id` del usuario autenticado y filtran en backend. Un ID ajeno devuelve 404. Las autoridades son las ya sembradas en V3.

## Auditoría y pruebas

Se agregaron los eventos `CREAR/MODIFICAR_AREA`, `CREAR/MODIFICAR_CARGO`, `CREAR/MODIFICAR/DESACTIVAR_EMPLEADO`, `ASIGNAR_SUPERVISOR`, `CERRAR_ASIGNACION_SUPERVISOR`, `CREAR/MODIFICAR_CUADRILLA`, `ASIGNAR_SUPERVISOR_CUADRILLA`, `AGREGAR_EMPLEADO_CUADRILLA` y `RETIRAR_EMPLEADO_CUADRILLA`. El evento JPA es inmutable y los triggers de V1 rechazan UPDATE/DELETE. Los detalles usan lista blanca, sin documentos ni secretos.

`cd backend; .\mvnw.cmd verify` ejecuta JUnit 5, MockMvc, ArchUnit e integración MySQL. Por defecto, la integración usa Testcontainers 8.4. Para validar en una BD local aislada se admiten `ASIS_TEST_DB_URL`, `ASIS_TEST_DB_USER` y `ASIS_TEST_DB_PASSWORD`; el harness restringe la URL a `127.0.0.1`/`localhost` y a nombres de base `asiscontrol_*test` o `asiscontrol_*validation`.

V1–V5 y `ddl-auto=validate` se probaron en una instancia MySQL 8.4.9 temporal en `127.0.0.1:3308`. La suite completa se ejecutó allí porque este equipo tiene 6 GB de RAM y WSL informa que falta la Plataforma de máquina virtual. Docker Desktop requiere 8 GB; al disponer de un host compatible se repetirá el mismo `verify` con Testcontainers.

## Deuda y decisiones técnicas

- La precisión de día no permite dos cambios de la misma relación en un día. Programar vigencias futuras e importaciones retroactivas complejas requiere un flujo separado.
- MySQL no tiene restricción de exclusión general de intervalos. Bloqueo, consulta de solapamiento e índice de fila abierta protegen las rutas del MVP; deben limitarse escrituras SQL privilegiadas fuera de la aplicación.
- Flyway 11.7.2 avisa que MySQL 8.4 es posterior a su última versión probada oficialmente, aunque aplicó y validó las cinco migraciones en la prueba local.
