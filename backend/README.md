# Motorsport Analytics — Backend

API REST en Java 21 + Spring Boot 4.1.2 (Gradle). La visión general del proyecto, la lista completa de endpoints y el despliegue están en el [README principal](../README.md).

## Cómo correrlo

Necesita PostgreSQL con el esquema ya creado (`../database/schema.sql`): el backend valida las tablas al iniciar (`ddl-auto=validate`) y no las crea.

```sh
cp .env.example .env      # completar DB_USERNAME, DB_PASSWORD y JWT_SECRET (mínimo 32 caracteres)
./gradlew bootRun         # en Windows: gradlew.bat bootRun
```

- API: `http://localhost:8080/api` (el puerto se toma de `PORT`, 8080 por defecto).
- Estado del servicio y módulos habilitados: `GET /api/salud`.
- Documentación interactiva (Swagger): `http://localhost:8080/swagger-ui.html`.

## Variables de entorno

Todas se leen de `application.properties`. Los valores reales nunca se suben a git: `.env` está ignorado y [`.env.example`](.env.example) solo trae valores vacíos o de ejemplo.

| Variable | Uso |
|---|---|
| `SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a PostgreSQL. |
| `JWT_SECRET`, `JWT_EXPIRACION_MINUTOS` | Firma y duración de los tokens (mínimo 32 caracteres; 240 minutos por defecto). |
| `ADMIN_EMAILS` | Emails con rol `ADMIN`, separados por coma. |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend autorizados (URL exacta, sin barra final). |
| `SYNC_AUTOMATICA`, `SYNC_CRON` | Sincronización diaria de la temporada en curso. |
| `WRC_HABILITADO` | Activa la fuente de WRC. |
| `TELEMETRIA_HABILITADA`, `OPENF1_USUARIO`, `OPENF1_PASSWORD` | Módulo de telemetría F1; las credenciales son solo para el modo en vivo. |

Las URLs de las APIs externas, los tiempos de espera, los reintentos y los cupos de consultas tienen valores por defecto (ver `.env.example`). La tabla completa con su explicación está en el [README principal](../README.md#2-backend).

## Estructura

```text
src/main/java/com/tfi/motorsport/backend/
  config/          seguridad (CORS + JWT), OpenAPI, tareas programadas y propiedades
  controllers/     endpoints REST; gestion/ (ABM de administración) y telemetria/ (OpenF1)
  services/        lógica de negocio (interfaces e Impl/); estadisticas/, gestion/ y telemetria/
  clients/         clientes HTTP de las APIs externas
    clientHttpExternal/   cliente común con tiempos de espera, reintentos y límite de consultas
    adapter/              un adaptador por proveedor (F1, WRC) hacia el modelo normalizado
    normalization/        validación y guardado idempotente por id_externo
    openf1/               cliente de OpenF1 con cupo por minuto
  repositories/    acceso a datos con Spring Data JPA
  model/           entidades JPA
  dtos/            objetos de entrada y salida de la API
  handlers/        manejo global de errores (formato uniforme)
  security/        generación y validación de tokens JWT
  common/          utilidades compartidas (Horario: UTC + hora local de la sede)
src/main/resources/application.properties
src/test/          tests que espejan la estructura de main/
Dockerfile         imagen para Render (compila con JDK 21 y ejecuta con JRE)
```

## Pruebas

```sh
./gradlew test       # en Windows: gradlew.bat test
```

- Usan JUnit 5, Mockito, AssertJ y MockMvc; los de integración corren sobre H2 en memoria (perfil `test`), por lo que **no hace falta PostgreSQL ni conexión a internet**: los proveedores externos se simulan con un servidor HTTP local.
- Cubren las funciones puras (motor estadístico, foto de la telemetría, `.ics`, zonas horarias), los servicios, los controladores (seguridad, validaciones y errores) y los adaptadores de F1 y WRC.
- JaCoCo genera el reporte de cobertura en `build/reports/jacoco/test/html/index.html`.
- Limitación: H2 no replica las reglas propias de PostgreSQL (disparadores, valores por defecto de auditoría e índices únicos parciales); esas se verifican a mano sobre PostgreSQL.

## Docker

```sh
docker build -t motorsport-backend .
docker run -p 8080:8080 --env-file .env motorsport-backend
```

El mismo `Dockerfile` es el que usa Render. Allí las variables se cargan en el panel del servicio y `PORT` lo inyecta la plataforma.
