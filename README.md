# Motorsport Analytics

Plataforma web dedicada a la integración, consulta y análisis de información de diversas categorías de automovilismo deportivo (Motorsport).

> **Trabajo Final Integrador (TFI)** — Tecnicatura Universitaria en Programación  
> **Universidad Tecnológica Nacional (UTN)**  
> **Grupo:** 190  
> **Integrantes:** Salas Mateo Roman · Luna Genaro Nahuel  
> **Estado actual:** MVP 1 implementado — consulta de eventos y resultados (backend + frontend)

## Repositorio

[GitHub — trabajo-final-utn](https://github.com/MRSalas07/trabajo-final-utn)

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

La plataforma permitirá visualizar información estadística por la categoría seleccionada, tales como:

- Posiciones en el campeonato.
- Equipos.
- Marcas.
- Fechas disputadas y por disputarse.
- Información estadística y comparativa.

También se prevé agregar la opción de **vincular la carrera por fecha al calendario del celular**, ya sea Android o iOS.

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
| **2 — Perfiles, portada y calendario** | Fichas de pilotos y equipos, portada con próximos y últimos eventos, calendario unificado con descarga `.ics`. | Pendiente |
| **3 — Datos automáticos** | Panel de administración para sincronizar datos desde APIs externas e importar archivos. | Pendiente |
| **4 — Estadísticas y gráficos** | Campeonatos, evolución de puntos, tendencias y comparativas entre pilotos. | Pendiente |
| **5 — Administración (ABM)** | Alta, baja lógica y modificación de todas las entidades. | Pendiente |
| **6 — Análisis avanzado** | Estadísticas por equipo y marca, histórico de temporadas. | Pendiente |
| **7 — Telemetría (funcionalidad plus)** | Telemetría de F1, sujeta a la disponibilidad del proveedor. | Pendiente |

## Funcionalidades del MVP 1

- **Cuentas:** registro, inicio de sesión y pantalla "Mi cuenta" para guardar la zona horaria preferida.
- **Zona horaria:** todos los horarios se muestran en la zona elegida por el usuario; al cambiarla, las pantallas se actualizan sin recargar.
- **Categorías:** listado de F1, WRC y TC.
- **Temporadas y fechas:** selección de temporada y tabla de eventos con fecha, circuito y estado.
- **Detalle de evento:** hora local y hora en la sede, diferencia horaria, sesiones o etapas, y tabla de resultados.

Los datos de temporadas, eventos y resultados todavía se cargan manualmente en la base: `database/datos_iniciales.sql` solo crea las tres categorías. La carga automática corresponde al MVP 3.

## Stack tecnológico

| Componente | Tecnología | Rol |
|---|---|---|
| **Backend** | Java 21 + Spring Boot 4.1.2 | API REST, lógica de negocio, persistencia con Spring Data JPA, seguridad con Spring Security y JWT, validación y documentación OpenAPI (Swagger). |
| **Frontend** | Angular 22 + TypeScript | SPA responsiva con componentes standalone y signals; consume la API con `HttpClient`. |
| **Base de datos** | PostgreSQL | Almacenamiento relacional de usuarios, categorías, temporadas, eventos, sesiones, etapas, pilotos, equipos y resultados. |
| **Pruebas** | JUnit + H2 en memoria | Tests del backend sin necesidad de PostgreSQL. |
| **Despliegue** | PaaS a definir (Render / Railway / Vercel) | Pendiente. |

## Arquitectura

Monolito por capas con API REST en el backend y una SPA desacoplada en el frontend. El detalle está en [`docs/arquitectura.md`](docs/arquitectura.md), los módulos en [`docs/modulos.md`](docs/modulos.md) y el modelo de datos en [`docs/diagramaUML.md`](docs/diagramaUML.md).

```text
Navegador (Angular)                      Servidor (Spring Boot)
página → servicio → interceptor  ──HTTP/JSON──►  seguridad (CORS + JWT) → controller → service → repository → PostgreSQL
```

Convenciones que atraviesan todo el sistema:

- **Horarios en UTC.** La base guarda todo en UTC. El frontend envía `?zona=<zona IANA>` y el backend responde cada horario en UTC y convertido a esa zona.
- **DTOs.** Las entidades JPA no salen del backend; la API responde con DTOs.
- **Errores uniformes.** Todo error responde con el formato `{ timestamp, status, error, detalles[] }`.
- **Baja lógica.** Los registros no se borran: se marcan con `eliminado`.

### Modelo de datos

```text
Categoria → Temporada → Evento ─┬─ Sesion → ResultadoSesion (piloto + equipo)        F1 y TC
                                └─ Etapa  → ResultadoEtapa  (tripulación)            WRC
```

### Seguridad

- Contraseñas almacenadas con hash BCrypt.
- Autenticación sin estado con JWT (HS256). El token incluye id, email, zona horaria y roles, y vence según `JWT_EXPIRACION_MINUTOS`.
- Las consultas (`GET /api/**`) son públicas. `/api/usuarios/**` requiere iniciar sesión. Crear o modificar categorías y `/api/admin/**` requieren rol `ADMIN`.
- El rol `ADMIN` se asigna a los emails configurados en `ADMIN_EMAILS`.
- CORS: la API solo acepta peticiones del navegador desde los orígenes de `CORS_ALLOWED_ORIGINS`.

## API disponible

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/salud` | Público | Estado del servicio. |
| POST | `/api/auth/registro` | Público | Crea un usuario y devuelve el token. |
| POST | `/api/auth/login` | Público | Inicia sesión y devuelve el token. |
| GET | `/api/usuarios/me` | Autenticado | Datos del usuario. |
| PUT | `/api/usuarios/me/preferencias` | Autenticado | Guarda la zona horaria y devuelve un token actualizado. |
| GET | `/api/categorias` | Público | Lista de categorías (filtro opcional `?activo=`). |
| GET | `/api/categorias/{id}` | Público | Detalle de una categoría. |
| POST | `/api/categorias` | ADMIN | Crea una categoría. |
| PUT | `/api/categorias/{id}` | ADMIN | Modifica una categoría. |
| GET | `/api/categorias/{siglas}/temporadas` | Público | Temporadas de una categoría. |
| GET | `/api/categorias/{siglas}/temporadas/{anio}/eventos` | Público | Eventos de una temporada con sus sesiones o etapas. |
| GET | `/api/eventos/{id}` | Público | Detalle de un evento. |
| GET | `/api/sesiones/{id}/resultados` | Público | Tabla de posiciones de una sesión. |
| GET | `/api/etapas/{id}/resultados` | Público | Tabla de tiempos de una etapa de rally. |

La documentación interactiva queda disponible en `/swagger-ui.html` con el backend levantado.

## Cómo levantar el proyecto

**Requisitos:** Java 21, Node.js con npm y PostgreSQL.

### 1. Base de datos

```sh
createdb motorsport_test
psql -d motorsport_test -f database/schema.sql
psql -d motorsport_test -f database/datos_iniciales.sql
```

El backend valida el esquema al iniciar (`ddl-auto=validate`), por lo que las tablas deben existir antes de levantarlo.

### 2. Backend

```sh
cd backend
cp .env.example .env      # completar DB_USERNAME, DB_PASSWORD y JWT_SECRET (mínimo 32 caracteres)
./gradlew bootRun         # en Windows: gradlew.bat bootRun
```

La API queda en `http://localhost:8080/api`.

| Variable | Uso |
|---|---|
| `SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a PostgreSQL. |
| `JWT_SECRET` | Clave para firmar los tokens (mínimo 32 caracteres). |
| `JWT_EXPIRACION_MINUTOS` | Duración del token (240 por defecto). |
| `ADMIN_EMAILS` | Emails con rol `ADMIN`, separados por coma. |
| `CORS_ALLOWED_ORIGINS` | URLs del frontend autorizadas (`http://localhost:4200` por defecto). |

### 3. Frontend

```sh
cd frontend
npm install
npm start
```

La aplicación queda en `http://localhost:4200` y consume la API definida en `src/environments/`.

### 4. Pruebas

```sh
cd backend
./gradlew test
```

## Estructura del repositorio

```text
database/
	schema.sql                 esquema relacional (DDL)
	datos_iniciales.sql        categorías F1, WRC y TC
docs/
	arquitectura.md
	diagramaUML.md
	modulos.md
backend/
	build.gradle
	.env.example
	src/main/java/com/tfi/motorsport/backend/
		config/                seguridad, CORS, OpenAPI y propiedades
		controllers/           endpoints REST
		services/              lógica de negocio (interfaces e Impl/)
		repositories/          acceso a datos con Spring Data JPA
		model/                 entidades JPA
		dtos/                  objetos de entrada y salida de la API
		handlers/              manejo global de errores
		security/              generación de tokens JWT
	src/test/                  tests con JUnit
frontend/
	src/app/
		app.config.ts          configuración global (router, HttpClient, idioma)
		app.routes.ts          rutas con carga diferida
		app.ts / app.html      marco de la aplicación (cabecera y navegación)
		core/                  servicios HTTP, sesión, interceptor, guards y modelos
		shared/                componentes y pipes reutilizables
		pages/                 una carpeta por pantalla
	src/environments/          URL del backend por entorno
README.md
```

## Flujo de trabajo en Git

- `main`: versión estable; recibe `develop` al cerrar cada hito.
- `develop`: rama de integración.
- `feature/<tema>`: funcionalidades nuevas; se integran a `develop` mediante pull request.
- `fix/<tema>`: correcciones sobre código ya integrado; también salen de `develop` y vuelven por pull request.

## Pendientes conocidos

- Datos de ejemplo de temporadas, eventos y resultados para probar el MVP 1 sin carga manual.
- Botón "Agregar al calendario" del detalle de evento: se habilita con el módulo de calendario (MVP 2).
- Tests de eventos y resultados en el backend; el frontend aún no tiene tests.
- Definición del servicio de despliegue.

## Historial de entregas

### **Hito Nro1:** Entrega 1 — Propuesta de idea y alcance

Se definió **Motorsport Analytics** como una plataforma web para centralizar y analizar información de automovilismo. La propuesta inicial delimitó el alcance a Fórmula 1 (F1), World Rally Championship (WRC) y Turismo Carretera (TC), e identificó como funcionalidades principales la consulta de categorías, el calendario unificado, los resultados y las estadísticas.

### **Hito Nro2:** Entrega 2 — Diseño técnico y estructura inicial

En esta etapa se avanzó desde la propuesta hacia el diseño de la solución y la organización inicial del repositorio:

- Se definió **PostgreSQL** como motor de base de datos, en reemplazo de las alternativas MySQL / SQL Server consideradas en la Entrega 1. La decisión se basa en su licencia open source, integridad transaccional, soporte de fechas y zonas horarias, e integración con Spring Data JPA e Hibernate; además, es compatible con las opciones PaaS evaluadas para el despliegue.
- Se documentaron el esquema relacional y el DDL con las entidades deportivas, relaciones, índices principales y campos de auditoría; se agregó un DML con categorías iniciales para F1, WRC y TC.
- Se definieron la arquitectura monolítica por capas con API REST y SPA, y los módulos funcionales del sistema.
- Se especificó la integración de datos mediante consumo de APIs externas y se incorporó como funcionalidad plus la telemetría en vivo, limitada a F1 y sujeta a la disponibilidad del proveedor.
- Se organizaron los documentos en `docs/`, el esquema y los datos iniciales en `database/`, y se crearon las estructuras iniciales de `backend/` y `frontend/`.

### **Avance actual:** MVP 1 — Consulta de eventos y resultados

- **Backend:** API REST con registro e inicio de sesión mediante JWT, preferencias de zona horaria, categorías, temporadas, eventos y tablas de resultados de sesiones y etapas; manejo global de errores, configuración de CORS y documentación Swagger.
- **Frontend:** aplicación Angular con las pantallas de inicio, categorías, temporadas y fechas, detalle de evento con resultados, ingreso y registro, y "Mi cuenta"; selector de zona horaria en la cabecera.
- **Pruebas:** tests de backend para categorías, autenticación, seguridad y zona horaria.
