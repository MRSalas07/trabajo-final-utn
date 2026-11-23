# Base de datos

Motor: PostgreSQL. El backend no crea tablas: las **valida** al iniciar (`ddl-auto=validate`), así que la base tiene que estar creada antes de levantarlo.

## Archivos

| Archivo | Para qué sirve |
|---|---|
| `schema.sql` | Esquema completo en su estado actual: tablas, `evento.zona_horaria` e índices únicos. Se ejecuta una vez, sobre una base vacía. |
| `datos_iniciales.sql` | Las tres categorías (F1, WRC y TC) y el usuario administrador local. Se puede ejecutar más de una vez. |
| `datos_ejemplo/seed_tc_2025.sql` | Opcional: temporada 2025 de Turismo Carretera relevada a mano. Se puede ejecutar más de una vez. |
| `migraciones/001_indices_unicos.sql` | Solo para bases creadas con un `schema.sql` anterior al 20/11/2026. Una base nueva no la necesita. |
| `migraciones/002_campeon_temporada.sql` | Solo para bases creadas antes del 22/11/2026 (agrega `temporada.id_piloto_campeon`). Una base nueva no la necesita. Si tu base es anterior, ejecutala o el backend no arranca. |
| `respaldos/exportar_temporada.sql` | Exporta una temporada a JSON (respaldo de F1/WRC y formato de carga de TC). |
| `respaldos/*.json` | Temporadas exportadas, listas para importar desde `/admin`. |

## Cómo se cargan los datos de cada categoría

- **F1 y WRC:** desde sus APIs, con **Sincronizar** en el panel `/admin`. Después de cada sincronización se exporta un respaldo en JSON.
- **TC:** no tiene API pública, así que cada temporada se carga a mano con **Importar JSON** en `/admin`, usando el mismo formato que los respaldos (ver `respaldos/tc_2025.json`). La temporada 2025 también está en `datos_ejemplo/seed_tc_2025.sql` para cargarla directo por SQL.

Todo se identifica por `id_externo`: sincronizar o importar dos veces actualiza los datos, no los duplica.

## Crear una base nueva

```sh
createdb motorsport
psql -d motorsport < database/schema.sql
psql -d motorsport < database/datos_iniciales.sql
psql -d motorsport < database/datos_ejemplo/seed_tc_2025.sql    # opcional: TC 2025
```

Se usa `<` en lugar de `-f` para que el archivo lo lea tu usuario: así no hay errores de permisos cuando `psql` corre como otro usuario (por ejemplo con `sudo -u postgres psql -d motorsport < ...`).

Para recrear una base existente desde cero: `dropdb motorsport` y repetir los pasos (borra **todos** los datos, usuarios incluidos).

Después, en el `.env` del backend:

```properties
ADMIN_EMAILS=admin@motorsport.local
```

El usuario `admin@motorsport.local` (contraseña `Admin2025!`) lo crea `datos_iniciales.sql`. El rol ADMIN no se guarda en la base: se asigna al iniciar sesión si el email está en `ADMIN_EMAILS`. Es solo para el entorno local.

## Cargar F1 y WRC

1. Levantar backend y frontend, e ingresar con el usuario administrador.
2. En `/admin`, sincronizar F1 y la temporada deseada, esperar a que el trabajo quede **COMPLETADO** y revisar los avisos del resumen.
3. Repetir con WRC. La API de WRC no es oficial: si no responde, restaurar desde el respaldo.

## Respaldo (exportar)

```sh
psql -d motorsport -At -v categoria=F1  -v anio=2025 < database/respaldos/exportar_temporada.sql > database/respaldos/f1_2025.json
psql -d motorsport -At -v categoria=WRC -v anio=2025 < database/respaldos/exportar_temporada.sql > database/respaldos/wrc_2025.json
```

- Los flags `-At` son obligatorios: dejan solo el JSON en la salida.
- Si la temporada no existe o tiene registros sin `id_externo`, el script avisa por pantalla y el archivo queda vacío.
- El script solo lee la base y omite los registros dados de baja.
- Los respaldos se suben al repositorio: así cualquiera del equipo reconstruye la base sin depender de las APIs.

## Restaurar (importar)

En `/admin` → **Importar JSON**, elegir el archivo (por ejemplo `respaldos/f1_2025.json`). También se puede hacer con la API:

```sh
curl -X POST http://localhost:8080/api/admin/importaciones \
  -H "Authorization: Bearer <token de un ADMIN>" -H "Content-Type: application/json" \
  --data-binary @database/respaldos/f1_2025.json
```

La importación es idempotente: se puede repetir, o hacer sobre una base que ya tiene esos datos, sin duplicar nada.

## Formato del JSON (TemporadaNorm)

Es el modelo normalizado del backend (`clients/normalization/ModeloNormalizado.java`). Resumen:

```text
temporada: siglasCategoria*, anio*, nombre, fuente, idExternoCampeon (id_externo del piloto campeón), eventos*[]
  evento: idExterno*, nombre*, nombreCorto, numeroFecha, pais, ciudad, circuito, zonaHoraria (IANA),
          inicio* (ISO-8601 UTC, ej. "2025-03-16T04:00:00Z"), fin, estado, sesiones[], etapas[]
    sesion (circuito): idExterno*, nombre*, tipo*, numero, inicio, fin, vueltasProgramadas, distancia, estado, resultados[]
      resultado: piloto*, equipo*, posicion, numero, puntos, tiempo, diferencia, vueltas, mejorVuelta, estado, motivoAbandono
    etapa (rally): idExterno*, numero, nombre*, dia, distancia, inicio, fin, estado, resultados[]
      resultado: piloto*, copiloto*, equipo*, numero, posicion, tiempo, diferencia, puntos, estado, motivoAbandono
  piloto: idExterno*, nombre, apellido*, fechaNacimiento (AAAA-MM-DD), nacionalidad, pais, codigo, numero
  equipo: idExterno*, nombre*, nombreCorto, nacionalidad
(* obligatorio)
```

- **estado** de evento, sesión y etapa: `PROGRAMADO`, `EN_CURSO`, `FINALIZADO` o `CANCELADO`.
- **estado** de un resultado: `FINALIZADO`, `ABANDONO`, `DESCALIFICADO` o `NO_LARGO`.
- **tipo** de sesión: `PRACTICA`, `CLASIFICACION`, `SPRINT_CLASIFICACION`, `SPRINT`, `SERIE` o `CARRERA`.
- **Tiempos y diferencias:** en segundos.
- **`id_externo` de pilotos y equipos:** con prefijo de categoría (`TC:agustin_canapino`), y hay que repetir siempre el mismo. Un id distinto crea otro piloto.
- **Un piloto sin resultados** no queda asociado a su equipo. Esa relación se crea a partir de los resultados.
