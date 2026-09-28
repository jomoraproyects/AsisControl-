# AsisControl

Software de control de asistencia del personal de Serviciudad. Se desarrolla como monolito modular por fases; contiene la **Fase 1: plataforma e identidad** y la **Fase 2: estructura organizacional**.

## Requisitos

- Java 21 LTS
- Docker con Docker Compose
- Maven Wrapper incluido

## Desarrollo local

1. Copiar `.env.example` a `.env` y reemplazar todos los secretos.
2. Generar `MFA_ENCRYPTION_KEY` como 32 bytes aleatorios codificados en Base64.
3. Iniciar MySQL:

   ```powershell
   docker compose --env-file .env -f docker-compose.development.yml up -d
   ```

4. Exportar las variables de `.env` a la sesión y ejecutar:

   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=development
   ```

5. Ejecutar pruebas:

   ```powershell
   cd backend
   .\mvnw.cmd verify
   ```

Las pruebas de integración usan MySQL 8.4 con Testcontainers cuando Docker está disponible. También pueden ejecutarse sobre una instancia MySQL local aislada; consulte [la ficha técnica de Fase 2](docs/FASE_2_FICHA_TECNICA.md).

## Bootstrap seguro

El primer `SUPER_ADMIN` no se incluye en una migración. Consulte [la ficha técnica de Fase 1](docs/FASE_1_FICHA_TECNICA.md) para el procedimiento mediante archivo de contraseña montado en el servidor.
