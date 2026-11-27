-- =====================================================================
-- Exporta UNA temporada de la base a un archivo JSON con el formato normalizado
-- del backend (ModeloNormalizado.TemporadaNorm).
--
-- Ese JSON sirve para dos cosas:
--   * RESPALDO de F1/WRC: despues de sincronizar desde las APIs, se guarda el JSON para
--     poder reconstruir la base sin depender de que las APIs respondan.
--   * CARGA MANUAL de TC: es el mismo formato que acepta "Importar JSON" en /admin
--     (POST /api/admin/importaciones).
--
-- Restaurar = importar el archivo en /admin. La importacion busca todo por id_externo,
-- asi que no duplica datos aunque se importe dos veces o sobre una base que ya los tiene.
--
-- Uso (la redireccion con < y > la hace tu usuario, asi evita problemas de permisos
-- cuando psql corre como postgres):
--   psql -d motorsport -At -v categoria=F1 -v anio=2025 < database/respaldos/exportar_temporada.sql > database/respaldos/f1_2025.json
--   sudo -u postgres psql -d motorsport -At -v categoria=WRC -v anio=2025 < database/respaldos/exportar_temporada.sql > database/respaldos/wrc_2025.json
--
-- Solo lee la base: no modifica nada. Ignora los registros dados de baja (eliminado = true).
-- =====================================================================
\set QUIET on
\set ON_ERROR_STOP on

\if :{?categoria}
\else
  \warn 'Falta la variable categoria. Ejemplo: -v categoria=F1 -v anio=2025'
  \quit
\endif
\if :{?anio}
\else
  \warn 'Falta la variable anio. Ejemplo: -v categoria=F1 -v anio=2025'
  \quit
\endif

-- La temporada tiene que existir
SELECT count(*) AS hay_temporada
FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria
WHERE upper(c.siglas) = upper(:'categoria') AND t.anio = :anio AND t.eliminado = FALSE
\gset
\if :hay_temporada
\else
  \warn 'No existe la temporada pedida (revisar categoria y anio).'
  \quit
\endif

-- La importacion exige id_externo en eventos, sesiones, etapas, pilotos y equipos.
-- Si algun registro de esta temporada no lo tiene, se corta antes de generar un JSON invalido.
SELECT count(*) AS sin_id_externo FROM (
    SELECT e.id_evento FROM evento e
    JOIN temporada t ON t.id_temporada = e.id_temporada JOIN categoria c ON c.id_categoria = t.id_categoria
    WHERE upper(c.siglas) = upper(:'categoria') AND t.anio = :anio AND e.eliminado = FALSE
      AND (coalesce(e.id_externo, '') = '' OR e.fecha_inicio IS NULL)
    UNION ALL
    SELECT s.id_sesion FROM sesion s JOIN evento e ON e.id_evento = s.id_evento
    JOIN temporada t ON t.id_temporada = e.id_temporada JOIN categoria c ON c.id_categoria = t.id_categoria
    WHERE upper(c.siglas) = upper(:'categoria') AND t.anio = :anio AND e.eliminado = FALSE AND s.eliminado = FALSE
      AND coalesce(s.id_externo, '') = ''
    UNION ALL
    SELECT et.id_etapa FROM etapa et JOIN evento e ON e.id_evento = et.id_evento
    JOIN temporada t ON t.id_temporada = e.id_temporada JOIN categoria c ON c.id_categoria = t.id_categoria
    WHERE upper(c.siglas) = upper(:'categoria') AND t.anio = :anio AND e.eliminado = FALSE AND et.eliminado = FALSE
      AND coalesce(et.id_externo, '') = ''
    UNION ALL
    SELECT r.id_resultado FROM resultado_sesion r
    JOIN piloto p ON p.id_piloto = r.id_piloto JOIN equipo q ON q.id_equipo = r.id_equipo
    JOIN sesion s ON s.id_sesion = r.id_sesion JOIN evento e ON e.id_evento = s.id_evento
    JOIN temporada t ON t.id_temporada = e.id_temporada JOIN categoria c ON c.id_categoria = t.id_categoria
    WHERE upper(c.siglas) = upper(:'categoria') AND t.anio = :anio AND r.eliminado = FALSE
      AND (coalesce(p.id_externo, '') = '' OR coalesce(q.id_externo, '') = '')
    UNION ALL
    SELECT r.id_resultado_etapa FROM resultado_etapa r
    JOIN tripulacion tr ON tr.id_tripulacion = r.id_tripulacion
    JOIN piloto p ON p.id_piloto = tr.id_piloto JOIN piloto cp ON cp.id_piloto = tr.id_copiloto
    JOIN equipo q ON q.id_equipo = tr.id_equipo
    JOIN etapa et ON et.id_etapa = r.id_etapa JOIN evento e ON e.id_evento = et.id_evento
    JOIN temporada t ON t.id_temporada = e.id_temporada JOIN categoria c ON c.id_categoria = t.id_categoria
    WHERE upper(c.siglas) = upper(:'categoria') AND t.anio = :anio AND r.eliminado = FALSE
      AND (coalesce(p.id_externo, '') = '' OR coalesce(cp.id_externo, '') = '' OR coalesce(q.id_externo, '') = '')
) faltantes
\gset
\if :sin_id_externo
  \warn 'Hay registros sin id_externo (o eventos sin fecha_inicio) en esta temporada; la importacion los rechazaria. Completarlos antes de exportar.'
  \quit
\endif

-- Funciones auxiliares (pg_temp: existen solo durante esta ejecucion)
CREATE FUNCTION pg_temp.iso(ts TIMESTAMP) RETURNS TEXT LANGUAGE sql IMMUTABLE AS
$$ SELECT replace(to_char(ts, 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'), '.000000Z', 'Z') $$;   -- las fechas se guardan en UTC

CREATE FUNCTION pg_temp.piloto_json(id BIGINT) RETURNS JSONB LANGUAGE sql STABLE AS
$$ SELECT jsonb_build_object(
       'idExterno', p.id_externo, 'nombre', p.nombre, 'apellido', p.apellido,
       'fechaNacimiento', p.fecha_nacimiento, 'nacionalidad', p.nacionalidad, 'pais', p.pais,
       'codigo', p.codigo_piloto, 'numero', p.numero_actual)
   FROM piloto p WHERE p.id_piloto = id $$;

CREATE FUNCTION pg_temp.equipo_json(id BIGINT) RETURNS JSONB LANGUAGE sql STABLE AS
$$ SELECT jsonb_build_object(
       'idExterno', q.id_externo, 'nombre', q.nombre, 'nombreCorto', q.nombre_corto,
       'nacionalidad', q.nacionalidad)
   FROM equipo q WHERE q.id_equipo = id $$;

-- Documento JSON (jsonb_strip_nulls quita los campos vacios para achicar el archivo)
SELECT jsonb_pretty(jsonb_strip_nulls(jsonb_build_object(
    'siglasCategoria', c.siglas,
    'anio', t.anio,
    'nombre', t.nombre,
    'fuente', 'RESPALDO_JSON',
    'idExternoCampeon', (SELECT pc.id_externo FROM piloto pc WHERE pc.id_piloto = t.id_piloto_campeon),
    'eventos', coalesce((
        SELECT jsonb_agg(jsonb_build_object(
            'idExterno', e.id_externo, 'nombre', e.nombre, 'nombreCorto', e.nombre_corto,
            'numeroFecha', e.numero_fecha, 'pais', e.pais, 'ciudad', e.ciudad, 'circuito', e.circuito,
            'zonaHoraria', e.zona_horaria, 'inicio', pg_temp.iso(e.fecha_inicio), 'fin', pg_temp.iso(e.fecha_fin),
            'estado', e.estado,
            'sesiones', (
                SELECT jsonb_agg(jsonb_build_object(
                    'idExterno', s.id_externo, 'nombre', s.nombre, 'tipo', s.tipo, 'numero', s.numero,
                    'inicio', pg_temp.iso(s.fecha_inicio), 'fin', pg_temp.iso(s.fecha_fin),
                    'vueltasProgramadas', s.vueltas_programadas, 'distancia', s.distancia, 'estado', s.estado,
                    'resultados', (
                        SELECT jsonb_agg(jsonb_build_object(
                            'piloto', pg_temp.piloto_json(r.id_piloto), 'equipo', pg_temp.equipo_json(r.id_equipo),
                            'posicion', r.posicion, 'numero', r.numero, 'puntos', r.puntos, 'tiempo', r.tiempo,
                            'diferencia', r.diferencia, 'vueltas', r.vueltas, 'mejorVuelta', r.mejor_vuelta,
                            'estado', r.estado, 'motivoAbandono', r.motivo_abandono,
                            -- marca de la participacion del piloto (TC); NULL en F1/WRC y la quita jsonb_strip_nulls
                            'marca', (SELECT max(ep.marca) FROM equipo_x_piloto ep
                                      WHERE ep.id_piloto = r.id_piloto AND ep.id_equipo = r.id_equipo
                                        AND ep.id_categoria = t.id_categoria AND ep.temporada = t.anio
                                        AND ep.eliminado = FALSE))
                            ORDER BY r.posicion NULLS LAST, r.id_resultado)
                        FROM resultado_sesion r WHERE r.id_sesion = s.id_sesion AND r.eliminado = FALSE)
                ) ORDER BY s.fecha_inicio NULLS LAST, s.id_sesion)
                FROM sesion s WHERE s.id_evento = e.id_evento AND s.eliminado = FALSE),
            'etapas', (
                SELECT jsonb_agg(jsonb_build_object(
                    'idExterno', et.id_externo, 'numero', et.numero, 'nombre', et.nombre, 'dia', et.dia,
                    'distancia', et.distancia, 'inicio', pg_temp.iso(et.fecha_inicio),
                    'fin', pg_temp.iso(et.fecha_fin), 'estado', et.estado,
                    'resultados', (
                        SELECT jsonb_agg(jsonb_build_object(
                            'piloto', pg_temp.piloto_json(tr.id_piloto), 'copiloto', pg_temp.piloto_json(tr.id_copiloto),
                            'equipo', pg_temp.equipo_json(tr.id_equipo), 'numero', tr.numero,
                            'posicion', r.posicion, 'tiempo', r.tiempo, 'diferencia', r.diferencia,
                            'puntos', r.puntos, 'estado', r.estado, 'motivoAbandono', r.motivo_abandono)
                            ORDER BY r.posicion NULLS LAST, r.id_resultado_etapa)
                        FROM resultado_etapa r JOIN tripulacion tr ON tr.id_tripulacion = r.id_tripulacion
                        WHERE r.id_etapa = et.id_etapa AND r.eliminado = FALSE)
                ) ORDER BY et.numero NULLS LAST, et.id_etapa)
                FROM etapa et WHERE et.id_evento = e.id_evento AND et.eliminado = FALSE)
        ) ORDER BY e.numero_fecha NULLS LAST, e.fecha_inicio, e.id_evento)
        FROM evento e WHERE e.id_temporada = t.id_temporada AND e.eliminado = FALSE
    ), '[]'::jsonb)
)))
FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria
WHERE upper(c.siglas) = upper(:'categoria') AND t.anio = :anio AND t.eliminado = FALSE;
