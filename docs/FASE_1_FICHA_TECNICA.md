# AsisControl — Fase 1: plataforma e identidad

## Alcance

Esta fase contiene exclusivamente plataforma, autenticación, autorización, MFA, usuarios, catálogos protegidos de roles/permisos, sesiones JDBC y auditoría base. No contiene dominios de las fases posteriores.

## Línea base

| Componente | Versión |
|---|---:|
| Java | 21 LTS |
| Maven / Wrapper | 3.9.16 |
| Spring Boot | 3.5.16 |
| MySQL | 8.4.11 LTS |
| JUnit Jupiter | 5.12.2, administrado por Boot |
| Mockito | 5.17.0, administrado por Boot |
| ArchUnit | 1.5.1 |
| Bouncy Castle | 1.86 |
| java-otp | 1.0.0 |

Spring Boot 3.5 se conserva deliberadamente para cumplir JUnit 5 y evitar introducir JUnit 6 durante la primera fase.

## Decisiones

- Package raíz: `com.empresa.asiscontrol`.
- Spring Session JDBC con MySQL; no se emiten JWT.
- La autenticación privilegiada es un flujo de contraseña y segundo factor. Antes de completar MFA no existe un `SecurityContext` autenticado.
- Los secretos TOTP se cifran con AES-256-GCM y una clave externa; los códigos de recuperación se muestran una sola vez y se persisten con Argon2id.
- Los roles son protegidos e inmutables. Las asignaciones se historizan con vigencias.
- `SUPERVISOR` y `CONDUCTOR` se crean en catálogo, pero su asignación queda bloqueada hasta que Fase 2 pueda exigir el vínculo usuario-empleado.
- El último `SUPER_ADMIN` activo no puede ser desactivado ni perder su rol.
- Cualquier cambio de contraseña, estado o rol incrementa `auth_version` y elimina todas las sesiones JDBC del usuario.
- La autorización usa permisos y termina con una política HTTP `denyAll`.
- La auditoría es append-only, reforzada por triggers de MySQL.
- El limitador de intentos es local al proceso. Es suficiente para el único nodo del MVP y deberá sustituirse por almacenamiento compartido antes de escalar horizontalmente.

## Bootstrap inicial

1. Crear un archivo legible únicamente por la cuenta del servicio, con una contraseña temporal robusta.
2. Definir `ASIS_BOOTSTRAP_ENABLED=true`, `ASIS_BOOTSTRAP_USERNAME`, `ASIS_BOOTSTRAP_PASSWORD_FILE` y opcionalmente `ASIS_BOOTSTRAP_EMAIL`.
3. Iniciar una sola vez. La operación solo es válida si no existe ningún usuario.
4. Retirar la variable y el archivo. El usuario deberá cambiar la contraseña y enrolar TOTP antes de obtener acceso completo.

La recuperación administrativa se ejecuta con `ASIS_RECOVERY_ENABLED=true`, usuario y archivo de nueva contraseña, usando `--spring.main.web-application-type=none`. Solo acepta un usuario que ya posea `SUPER_ADMIN`, revoca sus sesiones y puede reiniciar MFA explícitamente.

## Verificación y deuda conocida

- Las tres migraciones y la validación Hibernate arrancaron correctamente contra una instancia aislada MySQL 8.4.9 en un puerto temporal; no se utilizó la instancia local configurada del equipo.
- La suite descubre 20 pruebas. En el entorno de construcción actual pasan 11 pruebas unitarias/arquitectónicas y se omiten 9 pruebas de integración porque Docker no está instalado. Con Docker disponible, `mvnw.cmd verify` ejecuta esas pruebas sobre `mysql:8.4.11` mediante Testcontainers.
- Flyway 11.7.2, administrado por Spring Boot 3.5.16, migró MySQL 8.4 correctamente pero emite una advertencia porque declara 8.1 como última versión probada. Antes de producción debe validarse una actualización compatible de Flyway o documentarse formalmente la matriz soportada.
- El rate limit de autenticación es local al único proceso del MVP. Debe pasar a almacenamiento compartido antes de ejecutar más de una instancia del backend.
