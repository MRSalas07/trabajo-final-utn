# Motorsport Analytics — Frontend

SPA en Angular 22 que consume la API REST del backend. La visión general del proyecto, la API y cómo levantar todo están en el [README principal](../README.md).

## Cómo correrlo

```sh
npm install
npm start          # ng serve: http://localhost:4200
npm run build      # build de producción en dist/
npm run watch      # build de desarrollo que se regenera al guardar
```

Necesita el backend levantado. La dirección de la API se configura por entorno:

| Archivo | Cuándo se usa | `apiUrl` |
|---|---|---|
| `src/environments/environment.development.ts` | `npm start` (`ng serve`) | `http://localhost:8080/api` |
| `src/environments/environment.ts` | `npm run build` (producción) | Cambiarla por la URL del backend desplegado antes de publicar. |

## Estructura

```text
src/app/
  app.config.ts         router (carga diferida, parámetros de la URL como inputs), HttpClient con interceptor, idioma es-AR
  app.routes.ts         una ruta por pantalla; /cuenta pide sesión y /admin pide rol ADMIN
  app.ts / app.html     cabecera, menú de secciones y selector de zona horaria
  core/
    modelos.ts          interfaces de los DTOs del backend
    services/           un servicio HTTP por área (eventos, pilotos, estadísticas, telemetría…)
    interceptores/      agrega el token JWT y cierra la sesión si vence
    guards/             authGuard y adminGuard
    errores/            mensajeError(): texto legible a partir de un error HTTP
  shared/               gráfico (Chart.js), íconos SVG propios, estados de carga, calendario del mes, pipes de hora y tiempos
  pages/                una carpeta por pantalla
src/styles.css          estilos globales: variables de color, paneles y grilla de 12 columnas
```

| Ruta | Pantalla |
|---|---|
| `/` | Portada: próximos y últimos eventos, calendario del mes, tiempos de la última carrera, campeonatos y telemetría F1 |
| `/calendario` | Calendario mensual y descarga `.ics` de una temporada |
| `/categorias`, `/categorias/:siglas` | Categorías y ficha de cada una (temporadas, campeonato y fechas) |
| `/eventos/:id` | Detalle de un evento con sus resultados |
| `/pilotos`, `/pilotos/:id` | Listado con búsqueda y ficha de piloto |
| `/equipos`, `/equipos/:id` | Listado y ficha de equipo |
| `/estadisticas` | Campeonatos, evolución, tendencias, comparativas, marcas e histórico |
| `/telemetria` | Telemetría F1: foto de la sesión (en vivo o en repetición) con torre y versus de dos pilotos, y análisis |
| `/login`, `/cuenta` | Ingreso, registro y zona horaria del usuario |
| `/admin` | Sincronización, importación y ABM (solo `ADMIN`) |

## Convenciones

- **Componentes standalone y signals.** La app no usa zone.js: el estado de cada pantalla vive en `signal()` y `computed()`, y los pedidos que dependen de otros datos se disparan desde `effect()`.
- **Respuestas viejas.** Cuando un pedido puede quedar desactualizado (por ejemplo, al cambiar rápido de temporada), se numera y solo se aplica la última respuesta.
- **Horarios.** Se muestran en la zona elegida en la cabecera (`ZonaHorariaService` y el pipe `hora`); los servicios mandan `?zona=` cuando el backend convierte horarios.
- **Errores y cargas.** `mensajeError()` arma el texto del error y `<app-estado-carga>` muestra carga, error o vacío.
- **Gráficos.** `<app-grafico>` envuelve Chart.js. El backend hace los cálculos; el frontend solo los dibuja.
- **Telemetría.** Todo son GET comunes: `FotoSesion` pide una foto cada 5 s en vivo o una por segundo al reproducir, con `setInterval` dentro de un `effect()` que lo frena en `onCleanup` (al salir de la pestaña o de la página). La página consulta `/api/salud` y solo muestra el módulo si está habilitado.

## Pruebas

```sh
npm test -- --watch=false      # ng test (Vitest): 36 tests en 7 archivos .spec.ts
```

Cubren la lógica del cliente: seguridad (interceptor, guards y sesión), formatos y pipes, seguimiento de sincronizaciones y reglas de la telemetría, incluido el componente del versus. No cubren todas las pantallas.
