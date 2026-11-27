# Motorsport Analytics

Plataforma web dedicada a la integración, consulta y análisis de información de diversas categorías de automovilismo deportivo (Motorsport).

> **Trabajo Final Integrador (TFI)** — Tecnicatura Universitaria en Programación  
> **Universidad Tecnológica Nacional (UTN)**  
> **Grupo:** 190  
> **Integrantes:** Salas Mateo Roman · Luna Genaro Nahuel  
> **Estado actual:** versión final. Los siete MVP están implementados y el sistema está publicado en la nube (frontend en Vercel, backend en Render y base de datos en Neon).

## Accesos rápidos

Todo lo que se entrega está referenciado desde esta tabla.

| Qué | Dónde |
|---|---|
| **Sistema desplegado (frontend)** | <https://motorsport-analytics.vercel.app/> |
| **API desplegada (backend)** | <https://motorsport-analytics-api.onrender.com/api> · estado: [`/api/salud`](https://motorsport-analytics-api.onrender.com/api/salud) |
| **Informe Final (PDF)** | [`docs/Informe_Final_Grupo190.pdf`](docs/Informe_Final_Grupo190.pdf) |
| **Video de presentación final** | **COMPLETAR:** enlace al video |
| **Repositorio** | <https://github.com/MRSalas07/trabajo-final-utn> |
| **Integrantes** | [Integrantes del grupo](#integrantes-del-grupo) |
| **Tecnologías** | [Stack tecnológico](#stack-tecnológico) |
| **Cómo ejecutarlo** | [Cómo levantar el proyecto](#cómo-levantar-el-proyecto) |
| **Cómo se desplegó** | [Despliegue en la nube](#despliegue-en-la-nube) |
| **Limitaciones** | [Limitaciones y pendientes conocidos](#limitaciones-y-pendientes-conocidos) |

## Integrantes del grupo

**Grupo 190** — Tecnicatura Universitaria en Programación, UTN.

- Salas, Mateo Roman
- Luna, Genaro Nahuel

---

## Descripción del proyecto

**Motorsport Analytics** es una plataforma web dedicada a la integración, consulta y análisis de información de diversas categorías de automovilismo deportivo.

Su propósito principal es **recopilar, normalizar, almacenar y presentar de manera unificada** los datos deportivos que actualmente se encuentran fragmentados y dispersos en la web, ofreciendo una experiencia limpia e intuitiva inspirada en plataformas deportivas de fútbol como Promiedos o 365scores.

## Problemática

En el ecosistema del automovilismo deportivo, tanto nacional como internacional, la información sobre calendarios, horarios de eventos, estadísticas de rendimiento e historiales se encuentra descentralizada y distribuida entre múltiples plataformas oficiales y medios especializados de difícil lectura.

Cada categoría opera bajo su propia estructura de datos, nomenclatura y formatos dispares, frecuentemente archivos PDF de difícil acceso o sitios poco amigables en dispositivos móviles.

Esto genera una ineficiencia para el fanático y el analista deportivo, quienes deben consultar múltiples fuentes desconectadas para obtener un panorama unificado del Motorsport.

## Justificación y valor agregado

El proyecto se justifica técnicamente al resolver esta fragmentación mediante una **capa de integración que centraliza y normaliza datos provenientes de diversas fuentes**.

El factor diferencial respecto a un sistema CRUD tradicional radica en el procesamiento estadístico propio de los datos crudos recopilados.

La plataforma permite visualizar información estadística por la categoría seleccionada, tales como:

- Posiciones en el campeonato.
- Equipos.
- Marcas.
- Fechas disputadas y por disputarse.
- Información estadística y comparativa.

Además, cada evento se puede **agregar al calendario del celular** (Android o iOS) con un archivo `.ics`.

## Alcance

El alcance se delimita a la integración de tres categorías contrastantes de automovilismo:

| Sigla | Categoría | Particularidad |
|---|---|---|
| **F1** | Fórmula 1 | Eventos con sesiones: entrenamientos, clasificación, sprint y carrera. |
| **WRC** | World Rally Championship | Eventos con etapas o tramos cronometrados; cada resultado pertenece a una tripulación (piloto y copiloto). |
| **TC** | Turismo Carretera | Categoría nacional argentina de pista, con series clasificatorias y carrera final. |

## Hoja de ruta por MVPs

El desarrollo se organiza en MVPs: cada uno termina con algo que se puede probar de punta a punta.

| MVP | Alcance | Estado |
|---|---|---|
| **1 — Consulta de eventos y resultados** | Registro e inicio de sesión, zona horaria, categorías, temporadas, fechas y resultados de cada evento. | Implementado |
| **2 — Perfiles, portada y calendario** | Fichas de pilotos y equipos, portada con próximos y últimos eventos, calendario unificado con descarga `.ics`. | Implementado |
| **3 — Datos automáticos** | Panel de administración para sincronizar datos desde APIs externas e importar archivos. | Implementado |
| **4 — Estadísticas y gráficos** | Campeonatos, evolución de puntos, tendencias y comparativas entre pilotos. | Implementado |
| **5 — Administración (ABM)** | Alta, baja lógica y modificación de todas las entidades. | Implementado |
| **6 — Análisis avanzado** | Estadísticas por equipo y marca, histórico de temporadas. | Implementado |
| **7 — Telemetría (funcionalidad plus)** | Telemetría de F1, sujeta a la disponibilidad del proveedor. | Implementado (el modo en vivo requiere la suscripción de OpenF1) |

## Funcionalidades implementadas

### MVP 1 — Consulta de eventos y resultados

- **Cuentas:** registro, inicio de sesión y pantalla "Mi cuenta" para guardar la zona horaria preferida.
- **Zona horaria:** todos los horarios se muestran en la zona elegida por el usuario; al cambiarla, las pantallas se actualizan sin recargar.
- **Categorías:** listado de F1, WRC y TC.
- **Temporadas y fechas:** selección de temporada y tabla de eventos con fecha, circuito y estado.
- **Detalle de evento:** hora local y hora en la sede, diferencia horaria, sesiones o etapas, y tabla de resultados.

### MVP 2 — Perfiles, portada y calendario

- **Pilotos:** listado paginado con búsqueda por nombre, apellido o código y filtros de categoría y año; ficha con datos personales, palmarés por categoría, trayectoria y últimos resultados.
- **Equipos:** listado por categoría y año; ficha con plantel, categorías, estadísticas calculadas con los resultados cargados y últimos resultados.
- **Portada:** próximos eventos, calendario del mes, últimos eventos finalizados y tabla de tiempos de la última carrera.
- **Calendario unificado:** pantalla mensual con los eventos de las categorías elegidas y descarga del `.ics` de una temporada completa ("Agendar una temporada").
- **Agregar al calendario:** el detalle de cada evento descarga un archivo `.ics` que el celular o la computadora ofrecen agregar a su calendario (una entrada por sesión; una sola para todo el rally en WRC).

### MVP 3 — Datos automáticos

- **Panel `/admin`** (rol `ADMIN`), pestaña "Integración de datos": sincroniza una temporada de F1 desde Jolpica (`api.jolpi.ca`, sucesora de Ergast) o de WRC desde la API que usa el sitio wrc.com (no oficial). La sincronización corre en segundo plano y se sigue su estado; las fallas y los trabajos completados se avisan con notificaciones aunque se cambie de pantalla.
- **Importación JSON:** TC no tiene API pública, así que cada temporada se carga con un JSON en el formato normalizado del backend. El mismo formato sirve para los respaldos de F1 y WRC que se guardan en [`database/respaldos/`](database/respaldos/).
- **Sincronización automática** diaria y opcional de la temporada en curso (`SYNC_AUTOMATICA` y `SYNC_CRON`).
- Todo se identifica por `id_externo`: sincronizar o importar dos veces actualiza los datos, no los duplica.

### MVP 4 — Estadísticas y gráficos

- **Estadísticas por categoría y temporada (`/estadisticas`):** campeonato de pilotos y de equipos calculado con los resultados cargados, evolución del campeonato fecha a fecha, tendencia de un piloto (media móvil y regresión lineal, con su interpretación: mejorando, estable o empeorando) y comparativa entre dos pilotos (quién terminó delante en cada carrera que compartieron).
- **Ficha de categoría:** temporadas, líder, último ganador y próximo evento; en la portada, el top 5 del campeonato de cada categoría.
- **Palmarés:** se recalcula a partir de los resultados (acción de `ADMIN`).
- Los gráficos se dibujan con Chart.js; todos los cálculos los hace el backend.

### MVP 5 — Administración (ABM)

- **Gestión de datos en `/admin`:** alta, modificación y baja lógica de categorías, temporadas, eventos, sesiones, etapas, resultados, pilotos, equipos, participaciones (piloto en un equipo) y tripulaciones. El palmarés se consulta, porque es un dato derivado.
- **Auditoría:** cada registro guarda quién lo creó y quién lo modificó por última vez, y las tablas del panel lo muestran.
- Validaciones en el backend con mensajes de error en castellano.

### MVP 6 — Análisis avanzado

- **Equipos y marcas:** ranking de marcas de la temporada, tendencia de un equipo o de una marca y evolución de puntos por equipo o por marca.
- **Histórico:** trayectoria temporada por temporada de un piloto o de un equipo y comparativa histórica entre dos pilotos.
- **Marca en TC:** se guarda en la participación de cada piloto por temporada (`equipo_x_piloto.marca`), porque un mismo equipo puede correr con autos de distintas marcas. En F1 y WRC la marca es la del equipo.

### MVP 7 — Telemetría de F1 (funcionalidad plus)

- **Módulo opcional** (`TELEMETRIA_HABILITADA`) con datos de [OpenF1](https://openf1.org), una API abierta y no oficial con datos desde 2023. No usa la base de datos: consulta la API, guarda los datos un rato en memoria y respeta el cupo de consultas por minuto. Si está apagado, `/api/salud` lo informa: la página de telemetría muestra un aviso y la portada no muestra su panel.
- **Foto de la sesión:** la idea central del módulo. Para un instante T, cada dato es el último que OpenF1 informó hasta T: torre de tiempos (posición, diferencias, vueltas, última y mejor vuelta, neumático y paradas), versus de dos pilotos, dirección de carrera y clima.
- **Análisis de sesiones terminadas (`/telemetria`):** tiempos por vuelta de hasta seis pilotos, evolución de posiciones minuto a minuto y un resumen con la mejor vuelta de cada uno.
- **Repetición (sesiones terminadas):** "Reproducir" pide una foto por segundo y en cada una T avanza según la velocidad elegida (x1 a x30). La sesión se descarga una vez y cada foto se calcula en memoria (unos milisegundos), sin volver a consultar OpenF1. Se puede saltar a un minuto con el deslizador o, en carrera y sprint, de a una vuelta.
- **En vivo:** T es ahora y la página pide una foto nueva cada 5 segundos, el mínimo que permite el cupo de la suscripción de OpenF1 (`OPENF1_USUARIO` y `OPENF1_PASSWORD`).
- **Versus:** dos pilotos a elección, lado a lado: última y mejor vuelta con sus 3 sectores, mejores sectores, velocidad en la trampa y máxima, paradas, tiempo en boxes y neumático. En carrera, además, sus últimas vueltas comparadas una por una (quién fue más rápido y por cuánto).
- **Clasificación:** se cuenta por tandas (Q1, Q2, Q3) y no por vueltas; la mejor vuelta y las diferencias son las de la tanda en curso, como en la TV.

## Stack tecnológico

| Componente | Tecnología | Rol |
|---|---|---|
| **Backend** | Java 21 + Spring Boot 4.1.2 | API REST, lógica de negocio, persistencia con Spring Data JPA, seguridad con Spring Security y JWT, validación, tareas programadas y documentación OpenAPI (Swagger). |
| **Frontend** | Angular 22 + TypeScript | SPA responsiva con componentes standalone y signals (sin zone.js); consume la API con `HttpClient` y dibuja los gráficos con Chart.js. |
| **Base de datos** | PostgreSQL | Almacenamiento relacional de usuarios, categorías, temporadas, eventos, sesiones, etapas, pilotos, equipos, participaciones, tripulaciones, resultados y palmarés. |
| **Fuentes de datos** | Jolpica (F1), API de wrc.com (WRC), JSON (TC), OpenF1 (telemetría F1) | Sincronización e importación de temporadas; telemetría sin pasar por la base. |
| **Pruebas** | JUnit 5, Mockito, H2 en memoria y JaCoCo (backend); Vitest (frontend) | 471 tests del backend (86,4 % de cobertura de líneas) sin necesidad de PostgreSQL, y 36 del frontend. |
| **Despliegue** | Vercel (frontend), Render con Docker (backend) y Neon (PostgreSQL) | Sistema publicado; el detalle está en [Despliegue en la nube](#despliegue-en-la-nube). |

## Arquitectura

Monolito por capas con API REST en el backend y una SPA desacoplada en el frontend. El detalle está en [`docs/arquitectura.md`](docs/arquitectura.md), los módulos en [`docs/modulos.md`](docs/modulos.md) y el modelo de datos en [`docs/diagramaUML.md`](docs/diagramaUML.md).

```text
Navegador (Angular)                      Servidor (Spring Boot)
página → servicio → interceptor  ──HTTP/JSON──►  seguridad (CORS + JWT) → controller → service → repository → PostgreSQL
                                                                                  └─► clientes de APIs externas
```

Convenciones que atraviesan todo el sistema:

- **Horarios en UTC.** La base guarda todo en UTC. El frontend envía `?zona=<zona IANA>` y el backend responde cada horario en UTC y convertido a esa zona.
- **DTOs.** Las entidades JPA no salen del backend; la API responde con DTOs.
- **Errores uniformes.** Todo error responde con el formato `{ timestamp, status, error, detalles[] }`.
- **Baja lógica y auditoría.** Los registros no se borran: se marcan con `eliminado`. Cada tabla guarda además quién y cuándo creó y modificó cada registro.
- **Integración normalizada.** Un adaptador por proveedor traduce sus datos a un modelo común y un único persistidor los guarda, buscando siempre por `id_externo`.

### Modelo de datos

```text
Categoria → Temporada → Evento ─┬─ Sesion → ResultadoSesion (piloto + equipo)        F1 y TC
                                └─ Etapa  → ResultadoEtapa  (tripulación)            WRC

EquipoXPiloto   participación de un piloto en un equipo por temporada (con la marca, en TC)
Tripulacion     piloto + copiloto de un equipo por temporada (WRC)
Palmares        totales de cada piloto por categoría, recalculados con los resultados
```

### Seguridad

- Sin secretos en el repositorio: la base, el JWT y las claves de OpenF1 se leen de variables de entorno (ver [`backend/.env.example`](backend/.env.example), que solo trae valores vacíos o de ejemplo). El `.env` real no se sube a git.
- Contraseñas almacenadas con hash BCrypt.
- Autenticación sin estado con JWT (HS256). El token incluye id, email, zona horaria y roles, y vence según `JWT_EXPIRACION_MINUTOS`.
- Las consultas (`GET /api/**`) son públicas. `/api/usuarios/**` requiere iniciar sesión. `/api/admin/**`, crear, modificar o dar de baja categorías y recalcular el palmarés requieren rol `ADMIN`.
- El rol `ADMIN` se asigna a los emails configurados en `ADMIN_EMAILS`.
- Las credenciales de OpenF1 quedan en el servidor: el navegador solo hace GET públicos a `/api/telemetria`.
- CORS: la API solo acepta peticiones del navegador desde los orígenes de `CORS_ALLOWED_ORIGINS`.

## API disponible

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

## Cómo levantar el proyecto

**Requisitos:** Java 21, Node.js con npm y PostgreSQL.

### 1. Base de datos

```sh
createdb motorsport
psql -d motorsport < database/schema.sql
psql -d motorsport < database/datos_iniciales.sql
psql -d motorsport < database/datos_ejemplo/seed_tc_2025.sql   # opcional: TC 2025
```

El backend valida el esquema al iniciar (`ddl-auto=validate`), por lo que las tablas deben existir antes de levantarlo. `schema.sql` se ejecuta una vez sobre una base vacía; los otros dos se pueden repetir sin duplicar datos. F1 y WRC se cargan después desde `/admin` (sincronización o respaldo JSON): el paso a paso, el usuario administrador local, las migraciones para bases viejas y el formato de importación están en [`database/README.md`](database/README.md).

`datos_iniciales.sql` crea además un usuario administrador **solo para el entorno local** (`admin@motorsport.local`, con la contraseña indicada en el propio script). En una base publicada hay que cambiar esa contraseña o borrar el usuario, y definir en `ADMIN_EMAILS` únicamente los emails reales de administración.

Los archivos se pasan con `<` para que los lea tu usuario: así funciona también cuando `psql` corre como otro usuario (por ejemplo `sudo -u postgres psql -d motorsport < database/schema.sql`).

### 2. Backend

```sh
cd backend
cp .env.example .env      # completar DB_USERNAME, DB_PASSWORD y JWT_SECRET (mínimo 32 caracteres)
./gradlew bootRun         # en Windows: gradlew.bat bootRun
```

La API queda en `http://localhost:8080/api`.

| Variable | Uso |
|---|---|
| `SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a PostgreSQL (`.env.example` apunta a la base `motorsport`). |
| `JWT_SECRET` | Clave para firmar los tokens (mínimo 32 caracteres; se genera con `openssl rand -base64 48`). |
| `JWT_EXPIRACION_MINUTOS` | Duración del token (240 por defecto). |
| `ADMIN_EMAILS` | Emails con rol `ADMIN`, separados por coma. |
| `CORS_ALLOWED_ORIGINS` | URLs del frontend autorizadas (`http://localhost:4200` por defecto). |
| `SYNC_AUTOMATICA`, `SYNC_CRON` | Sincronización automática diaria de la temporada en curso (`false` y todos los días a las 6:00 por defecto). |
| `WRC_HABILITADO` | Activa la fuente de WRC (`true` por defecto). |
| `TELEMETRIA_HABILITADA` | Activa el módulo de telemetría F1 (`false` por defecto). |
| `OPENF1_USUARIO`, `OPENF1_PASSWORD` | Suscripción de OpenF1 para el modo en vivo. Sin ellas, las sesiones se ven media hora después de terminar. |

Las demás variables (URLs de las APIs, tiempos de espera, reintentos y cupos de consultas) tienen valores por defecto y están comentadas en `.env.example`.

### 3. Frontend

```sh
cd frontend
npm install
npm start
```

La aplicación queda en `http://localhost:4200` y consume la API definida en `src/environments/`. Hoy ambos archivos (`environment.ts` y `environment.development.ts`) apuntan al backend publicado en Render; para trabajar contra el backend local hay que cambiar `apiUrl` en `environment.development.ts` a `http://localhost:8080/api`. Más detalles en [`frontend/README.md`](frontend/README.md).

### 4. Pruebas

```sh
# Backend (JUnit + H2 en memoria: no hace falta PostgreSQL)
cd backend
./gradlew test             # en Windows: gradlew.bat test

# Frontend (Vitest)
cd frontend
npm test -- --watch=false
```

Resultado en `develop` al cerrar el MVP 7: **471 tests del backend y 36 del frontend, todos aprobados**. `./gradlew test` genera además el reporte de cobertura de JaCoCo en `backend/build/reports/jacoco/test/html/index.html` (86 % de las líneas y 70,6 % de las ramas).

Los tests del backend cubren las funciones puras (motor estadístico, foto de la telemetría, `.ics`, zonas horarias), los servicios con dependencias simuladas, los controladores con `MockMvc` (seguridad, validaciones y errores) y los adaptadores de F1 y WRC contra un servidor HTTP local. Los del frontend cubren la seguridad (interceptor, guards y sesión), los formatos, el seguimiento de sincronizaciones y las reglas de la telemetría, incluido el versus. H2 no replica las reglas propias de PostgreSQL (valores por defecto de auditoría, disparadores e índices únicos parciales): esas se verifican a mano sobre PostgreSQL.

## Despliegue en la nube

| Pieza | Servicio | Configuración en el repositorio |
|---|---|---|
| Base de datos | **Neon** (PostgreSQL administrado, `sslmode=require`) | Se crea con [`database/schema.sql`](database/schema.sql) y [`database/datos_iniciales.sql`](database/datos_iniciales.sql); los datos se cargan desde `/admin` (sincronización o respaldos JSON). |
| Backend | **Render** (servicio web con Docker, directorio raíz `backend`, health check `/api/salud`) | [`backend/Dockerfile`](backend/Dockerfile) y [`backend/.dockerignore`](backend/.dockerignore). |
| Frontend | **Vercel** (directorio raíz `frontend`, build `npm run build`, salida `dist/motorsport-frontend/browser`) | [`frontend/vercel.json`](frontend/vercel.json): comando de build, carpeta de salida y *rewrite* de todas las rutas a `index.html` (la SPA usa rutas del navegador). |

Las variables de entorno del backend se cargan en el panel de Render (los nombres y su uso están en la tabla de [Backend](#2-backend)). Las que **hay que** definir: `SPRING_DATASOURCE_URL` (URL JDBC de Neon con `sslmode=require`), `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAILS` y `CORS_ALLOWED_ORIGINS`, que debe ser **exactamente** la URL del frontend en Vercel (sin barra final). El frontend no usa variables de entorno: la URL del backend está en `frontend/src/environments/environment.ts`.

Para actualizar el sistema publicado alcanza con hacer *push* a la rama configurada en cada servicio; Render y Vercel vuelven a compilar solos.

Nota: en el plan gratuito de Render el backend se duerme tras un rato sin tráfico, así que la primera consulta puede tardar cerca de un minuto en responder.

## Estructura del repositorio

```text
database/
	README.md                  cómo crear la base, cargar datos y respaldarlos
	schema.sql                 esquema relacional completo (DDL + índices únicos)
	migraciones/               solo para actualizar bases creadas con un esquema anterior
	datos_iniciales.sql        categorías F1, WRC y TC + administrador local
	datos_ejemplo/             temporada 2025 de TC (opcional)
	respaldos/                 exportación a JSON y temporadas respaldadas
docs/
	arquitectura.md
	diagramaUML.md
	modulos.md
	Informe_Final_Grupo190.pdf informe final del trabajo
backend/
	build.gradle
	Dockerfile                 imagen del backend para Render
	.env.example
	src/main/java/com/tfi/motorsport/backend/
		config/                seguridad, CORS, OpenAPI, tareas programadas y propiedades
		controllers/           endpoints REST (gestion/ para el ABM, telemetria/ para OpenF1)
		services/              lógica de negocio (interfaces e Impl/; estadisticas/, gestion/ y telemetria/)
		clients/               clientes HTTP de las APIs externas, adaptadores y normalización
		repositories/          acceso a datos con Spring Data JPA
		model/                 entidades JPA
		dtos/                  objetos de entrada y salida de la API
		handlers/              manejo global de errores
		security/              generación de tokens JWT
	src/test/                  tests con JUnit, Mockito y MockMvc (espejan la estructura de main/)
frontend/
	src/app/
		app.config.ts          configuración global (router, HttpClient, idioma)
		app.routes.ts          rutas con carga diferida
		app.ts / app.html      marco de la aplicación (cabecera y navegación)
		core/                  servicios HTTP, sesión, interceptor, guards y modelos
		shared/                componentes y pipes reutilizables (gráfico, íconos, estados de carga)
		pages/                 una carpeta por pantalla
	src/environments/          URL del backend por entorno
	vercel.json                configuración de despliegue en Vercel
README.md
```

## Flujo de trabajo en Git

- `main`: versión estable; recibe una rama `release/x.y.0` al cerrar cada hito.
- `develop`: rama de integración.
- `feature/<tema>`: funcionalidades nuevas; se integran a `develop` mediante pull request.
- `fix/<tema>`: correcciones sobre código ya integrado; también salen de `develop` y vuelven por pull request.
- `docs/<tema>` y `chore/<tema>`: documentación y tareas de mantenimiento (datos, tests), con el mismo circuito.

Las ramas de trabajo son cortas: se crean desde `develop` actualizado, se integran por pull request y se borran después del merge.

## Limitaciones y pendientes conocidos

- **Plan gratuito:** el backend en Render se duerme sin tráfico (primera respuesta lenta) y Neon tiene límites de almacenamiento y de horas de cómputo.
- **Datos:** los respaldos JSON no traen el campeón oficial (`idExternoCampeon`), así que el palmarés usa el campeonato calculado; falta el respaldo de F1 2026 y las marcas de TC 2025.
- **Fuentes externas:** la API de WRC no es oficial y puede dejar de responder (los respaldos JSON permiten reconstruir la base). El modo en vivo de la telemetría requiere la suscripción paga de OpenF1.
- **Pruebas:** los tests del backend corren sobre H2, así que las reglas propias de PostgreSQL se verifican a mano. Los del frontend cubren la lógica (seguridad, formatos, telemetría y seguimiento de sincronizaciones), no todas las pantallas.

## Historial de entregas

### **Hito Nro1:** Entrega 1 — Propuesta de idea y alcance

Se definió **Motorsport Analytics** como una plataforma web para centralizar y analizar información de automovilismo. La propuesta inicial delimitó el alcance a Fórmula 1 (F1), World Rally Championship (WRC) y Turismo Carretera (TC), e identificó como funcionalidades principales la consulta de categorías, el calendario unificado, los resultados y las estadísticas.

### **Hito Nro2:** Entrega 2 — Diseño técnico y estructura inicial

En esta etapa se avanzó desde la propuesta hacia el diseño de la solución y la organización inicial del repositorio:

- Se definió **PostgreSQL** como motor de base de datos, en reemplazo de las alternativas MySQL / SQL Server consideradas en la Entrega 1. La decisión se basa en su licencia open source, integridad transaccional, soporte de fechas y zonas horarias, e integración con Spring Data JPA e Hibernate; además, es compatible con los servicios PaaS evaluados para el despliegue (finalmente se usó Neon).
- Se documentaron el esquema relacional y el DDL con las entidades deportivas, relaciones, índices principales y campos de auditoría; se agregó un DML con categorías iniciales para F1, WRC y TC.
- Se definieron la arquitectura monolítica por capas con API REST y SPA, y los módulos funcionales del sistema.
- Se especificó la integración de datos mediante consumo de APIs externas y se incorporó como funcionalidad plus la telemetría en vivo, limitada a F1 y sujeta a la disponibilidad del proveedor.
- Se organizaron los documentos en `docs/`, el esquema y los datos iniciales en `database/`, y se crearon las estructuras iniciales de `backend/` y `frontend/`.

### **Hito:** MVP 1 — Consulta de eventos y resultados

- **Backend:** API REST con registro e inicio de sesión mediante JWT, preferencias de zona horaria, categorías, temporadas, eventos y tablas de resultados de sesiones y etapas; manejo global de errores, configuración de CORS y documentación Swagger.
- **Frontend:** aplicación Angular con las pantallas de inicio, categorías, temporadas y fechas, detalle de evento con resultados, ingreso y registro, y "Mi cuenta"; selector de zona horaria en la cabecera.
- **Pruebas:** tests de backend para categorías, autenticación, seguridad y zona horaria.

### **Hito:** MVP 2 — Perfiles, portada y calendario (versión 1.3.0)

- **Base de datos:** migración con índices únicos y la zona horaria de cada evento; datos de ejemplo de la temporada 2025 de F1, WRC y TC.
- **Backend:** listado y fichas de pilotos y equipos; calendario por rango de fechas, próximos y recientes; generación de archivos `.ics` por evento y por temporada. Además, errores de validación y de tipo de parámetro responden 400 (antes 500), el error genérico ya no expone detalles internos y se desactivó `open-in-view`.
- **Frontend:** pantallas de pilotos y equipos con sus fichas, portada con próximos y últimos eventos, calendario mensual y botón "Agregar al calendario" en el detalle de evento.
- **Pruebas:** 89 tests de backend; se sumaron los de perfiles, calendario, generación de `.ics`, manejo de errores y del servicio de eventos.

### **Hito:** MVP 3 — Datos automáticos (versión 1.4.0)

- **Backend:** clientes de Jolpica (F1) y de la API de wrc.com (WRC) con reintentos y límite de consultas, adaptadores que normalizan cada proveedor a un modelo común, sincronización en segundo plano con seguimiento de estado, importación de temporadas en JSON y sincronización automática diaria opcional.
- **Frontend:** panel `/admin` para sincronizar e importar. Las notificaciones de los trabajos en curso llegaron en la versión 1.5.0.
- **Base de datos:** script de exportación a JSON y respaldos de las temporadas de F1 y WRC.

### **Hito:** MVP 4 — Estadísticas y gráficos (versión 1.5.0)

- **Backend:** campeonatos de pilotos y equipos, evolución, tendencia (media móvil y regresión lineal), comparativa entre pilotos, resumen de la temporada, ficha de categoría y recálculo del palmarés.
- **Frontend:** página de estadísticas con gráficos (Chart.js), ficha de categoría y panel de campeonatos en la portada.

### **Hito:** MVP 5 — Administración (versión 1.6.0)

- **Backend:** ABM de todas las entidades con baja lógica y validaciones.
- **Frontend:** gestión de datos en `/admin` con tablas reutilizables por entidad.

### **Hito:** MVP 6 — Análisis avanzado (versión 1.7.0)

- **Backend:** ranking y tendencias de marcas y equipos, evolución por grupo, historial de pilotos y equipos y comparativa histórica; el campeón oficial de cada temporada (`temporada.id_piloto_campeon`).
- **Frontend:** pestañas "Equipos y marcas" e "Histórico" en estadísticas.

### **Hito:** Despliegue en la nube

- **Backend:** `Dockerfile` en dos etapas (compila con JDK 21 y ejecuta con JRE, usuario sin privilegios) publicado en Render; la configuración sensible va en variables de entorno.
- **Frontend:** `vercel.json` con el *rewrite* de la SPA; publicado en Vercel apuntando al backend de Render.
- **Base de datos:** PostgreSQL en Neon, creada con `schema.sql` y `datos_iniciales.sql`.

### **Hito:** MVP 7 — Telemetría F1 (versión 1.8.0)

- **Backend:** módulo de telemetría con OpenF1 (cliente con cupo por minuto y token para el vivo), análisis de sesiones y foto de la sesión en un instante con funciones puras (`CalculadoraFoto`) y caché en memoria. Un único `GET /api/telemetria/sesiones/{sessionKey}/foto` (`vuelta`, `instante`, `pilotos`) resuelve el vivo, la repetición y el resultado final, y arma torre, versus de dos pilotos (sectores, velocidades, paradas), vuelta en curso, tandas de clasificación, mensajes y clima. Además: marca por participación en TC (`equipo_x_piloto.marca`), auditoría visible en el panel de administración y `ddl-auto=validate`.
- **Frontend:** página de telemetría con las pestañas Sesión (torre, versus, dirección de carrera y clima; repetición segundo a segundo de x1 a x30, vivo cada 5 s y línea de tiempo con segundos) y Análisis. Las dos pestañas comparten la misma descarga de la sesión (8 consultas a OpenF1).
- **Rediseño final:** la primera versión transmitía los datos por SSE con hilos propios y dibujaba un mapa del circuito que se cortaba (el contorno se armaba con 1500 posiciones, unos 60 s de sesión). Se reemplazó por la "foto en el instante T" con consultas comunes: sin hilos propios, sin conexiones abiertas y sin `PUT` público. Se eliminaron `Transmision`, `Vivo`, `Repeticion`, `EstadoCarrera` y el mapa.
- **Pruebas:** 471 tests de backend (86,4 % de cobertura de líneas) y 36 de frontend, todos aprobados; en este ciclo se sumaron los de gestión (ABM), auditoría, telemetría, controladores y el frontend (seguridad, formatos, seguimiento de sincronizaciones y versus).
