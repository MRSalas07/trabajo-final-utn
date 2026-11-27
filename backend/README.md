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

## Endopoint disponibles

La documentación interactiva queda disponible en `/swagger-ui.html` con el backend levantado. Las consultas aceptan `?zona=<zona IANA>` para devolver los horarios convertidos.

### Autenticación y usuario

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/registro` | Público | Crea un usuario y devuelve el token. |
| POST | `/api/auth/login` | Público | Inicia sesión y devuelve el token. |
| GET | `/api/usuarios/me` | Autenticado | Datos del usuario. |
| PUT | `/api/usuarios/me/preferencias` | Autenticado | Guarda la zona horaria y devuelve un token actualizado. |

### Consulta

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/salud` | Público | Estado del servicio y módulos habilitados (`telemetria`, `wrcAutomatico`). |
| GET | `/api/categorias` | Público | Lista de categorías (filtro opcional `?activo=`). |
| GET | `/api/categorias/{id}` | Público | Detalle de una categoría. |
| GET | `/api/categorias/{siglas}/perfil` | Público | Ficha de la categoría: temporadas, líder, último ganador y próximo evento. |
| GET | `/api/categorias/{siglas}/temporadas` | Público | Temporadas de una categoría. |
| GET | `/api/categorias/{siglas}/temporadas/{anio}/eventos` | Público | Eventos de una temporada con sus sesiones o etapas. |
| GET | `/api/eventos/{id}` | Público | Detalle de un evento. |
| GET | `/api/sesiones/{id}/resultados` | Público | Tabla de posiciones de una sesión. |
| GET | `/api/etapas/{id}/resultados` | Público | Tabla de tiempos de una etapa de rally o su clasificación general. |
| GET | `/api/pilotos` | Público | Listado paginado de pilotos. Filtros: `filPiloto`, `filtroCategoria`, `filtroAnio`; paginado: `paginado` (desde 0) y `cantidad` (1 a 100, 24 por defecto). |
| GET | `/api/pilotos/{id}` | Público | Ficha de un piloto: datos, palmarés, trayectoria y últimos resultados. |
| GET | `/api/equipos` | Público | Equipos de una categoría (`categoria` obligatoria, `anio` opcional). |
| GET | `/api/equipos/{id}` | Público | Ficha de un equipo: plantel, estadísticas y últimos resultados. |
| GET | `/api/calendario` | Público | Eventos entre `desde` y `hasta` (`yyyy-MM-dd`, en la zona del usuario; hasta 366 días), filtro opcional `categorias` separadas por coma. |
| GET | `/api/calendario/proximos` | Público | Próximos eventos, incluidos los que están en curso (`limite` de 1 a 20). |
| GET | `/api/calendario/recientes` | Público | Últimos eventos finalizados (`limite` de 1 a 20). |
| GET | `/api/calendario/eventos/{id}.ics` | Público | Archivo `.ics` de un evento. |
| GET | `/api/calendario/{siglas}/{anio}.ics` | Público | Archivo `.ics` de una temporada completa. |

### Estadísticas (`/api/estadisticas/{siglas}`)

Todo se calcula con los resultados cargados en la base.

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/{anio}/resumen` | Público | Resumen de la temporada: líder, ganadores por fecha y fechas disputadas. |
| GET | `/{anio}/campeonato/pilotos` | Público | Campeonato de pilotos. |
| GET | `/{anio}/campeonato/equipos` | Público | Campeonato de equipos (suma de los puntos de sus pilotos). |
| GET | `/{anio}/evolucion` | Público | Puntos acumulados y posición fecha a fecha (`pilotos` separados por coma o los `top` primeros, 5 por defecto). |
| GET | `/{anio}/tendencia/{pilotoId}` | Público | Tendencia de un piloto: media móvil, regresión lineal e interpretación. |
| GET | `/{anio}/comparativa?a=&b=` | Público | Comparativa entre dos pilotos en la temporada. |
| GET | `/{anio}/marcas` | Público | Ranking de marcas. |
| GET | `/{anio}/tendencia-equipo/{equipoId}` | Público | Tendencia de un equipo (puntos por fecha, media móvil y regresión). |
| GET | `/{anio}/tendencia-marca?marca=` | Público | Tendencia de una marca (ej: Toyota, Ford). |
| GET | `/{anio}/evolucion-grupos?tipo=EQUIPO\|MARCA` | Público | Puntos acumulados por equipo o por marca (`top`, 6 por defecto). |
| GET | `/historico/pilotos/{pilotoId}` | Público | Historial de un piloto temporada por temporada. |
| GET | `/historico/equipos/{equipoId}` | Público | Historial de un equipo temporada por temporada. |
| GET | `/historico/comparativa?a=&b=` | Público | Comparativa histórica entre dos pilotos. |
| POST | `/palmares/recalcular` | ADMIN | Recalcula el palmarés de la categoría. |

### Administración (rol `ADMIN`)

| Método | Ruta | Descripción |
|---|---|---|
| POST · PUT · DELETE | `/api/categorias`, `/api/categorias/{id}` | Alta, modificación y baja lógica de categorías. |
| GET | `/api/admin/categorias` | Categorías no dadas de baja (activas e inactivas), con su auditoría. |
| GET | `/api/admin/fuentes` | Fuentes automáticas disponibles por categoría. |
| POST | `/api/admin/sincronizaciones` | Inicia la sincronización de una temporada desde su API (en segundo plano). |
| GET | `/api/admin/sincronizaciones`, `/api/admin/sincronizaciones/{id}` | Historial y estado de las sincronizaciones. |
| POST | `/api/admin/importaciones` | Importa una temporada en JSON (formato en [`database/README.md`](database/README.md)). |

ABM: cada recurso tiene `GET /api/admin/{recurso}` (con los filtros de la tabla), `POST`, `PUT /{id}` y `DELETE /{id}` (baja lógica).

| Recurso | Filtros del listado |
|---|---|
| `temporadas` | `categoriaId` |
| `eventos` | `temporadaId` |
| `sesiones`, `etapas` | `eventoId` |
| `resultados-sesion` | `sesionId` |
| `resultados-etapa` | `etapaId` |
| `pilotos`, `equipos` | `q` (búsqueda) y `limit` (hasta 500) |
| `participaciones`, `tripulaciones` | `categoriaId` y `anio` opcional |
| `palmares` | `categoriaId` (solo consulta) |

### Telemetría F1 (`/api/telemetria`, con `TELEMETRIA_HABILITADA=true`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/sesiones?anio=` | Público | Sesiones de un año con datos en OpenF1 (desde 2023), de la más reciente a la más vieja. |
| GET | `/sesiones/{sessionKey}` | Público | Análisis de una sesión terminada: tiempos por vuelta (`pilotos` separados por coma, hasta 6) y posiciones. |
| GET | `/sesiones/{sessionKey}/foto` | Público | Foto de la sesión. En curso: ahora (requiere la suscripción). Terminada: al final de la `vuelta` del líder, en el `instante` pedido (ISO 8601) o, sin ninguno, el final. `pilotos`: hasta 2 para el versus. |

Una sesión terminada se descarga una vez (8 consultas a OpenF1) y queda media hora en memoria: después cada foto se calcula sin consultar OpenF1. En vivo, posiciones, intervalos y vueltas se renuevan cada 5 segundos y el resto cada 30 (46 consultas por minuto, debajo del cupo de 55).

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
