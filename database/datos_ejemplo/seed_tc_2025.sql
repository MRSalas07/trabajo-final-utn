-- =====================================================================
-- Datos de ejemplo - Turismo Carretera 2025 (OPCIONAL)
-- Motor: PostgreSQL
--
-- Orden de ejecucion: schema.sql -> datos_iniciales.sql -> este archivo.
-- F1 y WRC ya NO se cargan con scripts: se traen desde sus APIs con el panel /admin
-- (sincronizacion) y se respaldan en JSON (ver database/README.md).
-- TC no tiene API publica: sus temporadas se cargan a mano (importacion JSON en /admin).
-- Este archivo es la temporada 2025 relevada a mano, lista para cargar por SQL.
--
-- Es idempotente: usa ON CONFLICT sobre los indices unicos de schema.sql y
-- WHERE NOT EXISTS en las tablas sin indice unico. Las FK se resuelven por id_externo/siglas,
-- nunca por ids fijos, asi que no depende del valor de las secuencias.
--
-- Convenciones (las mismas que usa la importacion JSON):
--   * Fechas TIMESTAMP de evento/sesion en UTC; evento.zona_horaria = zona IANA de la sede.
--   * id_externo de piloto/equipo con prefijo de categoria ('TC:agustin_canapino').
--   * id_externo de evento = numero de fecha ('01'..'15'); de sesion = 'FINAL'.
--   * palmares guarda las estadisticas de la TEMPORADA 2025 publicadas por la ACTC.
--     El recalculo del MVP 4 (POST /api/admin/palmares/TC/recalcular) puede reemplazarlas.
--   * Tiempos/diferencias en segundos.
--
-- Fuentes (consultadas nov-2026): ACTC (Copa de Oro 2025), La Nacion / genesis24.net /
-- racingcalendar.net (calendario, ganadores y equipos TC 2025).
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- TEMPORADA 2025
-- ---------------------------------------------------------------------

INSERT INTO temporada (id_categoria, anio, nombre, fecha_inicio, fecha_fin) VALUES ((SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 2025, 'Campeonato Argentino de Turismo Carretera 2025', '2025-02-15', '2025-12-07') ON CONFLICT (id_categoria, anio) DO NOTHING;

-- ---------------------------------------------------------------------
-- EQUIPO
-- ---------------------------------------------------------------------
INSERT INTO equipo (id_externo, nombre, nombre_corto, nacionalidad, sede) VALUES
    ('TC:canning_motorsport', 'Canning Motorsport', 'Canning', 'Argentina', NULL),
    ('TC:pradecon_racing', 'Pradecon Racing', 'Pradecon', 'Argentina', NULL),
    ('TC:fispa_corse', 'Fispa Corse', 'Fispa', 'Argentina', NULL),
    ('TC:maquin_parts', 'Maquin Parts Racing', 'Maquin Parts', 'Argentina', NULL),
    ('TC:fadel_memo_corse', 'Fadel Memo Corse', 'Fadel', 'Argentina', NULL),
    ('TC:tomas_abdala_racing', 'Tomás Abdala Racing', 'TAR', 'Argentina', NULL),
    ('TC:trotta_racing', 'Trotta Racing Team', 'Trotta', 'Argentina', NULL)
ON CONFLICT (id_externo) WHERE id_externo IS NOT NULL DO NOTHING;

-- ---------------------------------------------------------------------
-- PILOTO (fecha_nacimiento NULL = dato no relevado)
-- ---------------------------------------------------------------------
INSERT INTO piloto (id_externo, nombre, apellido, fecha_nacimiento, nacionalidad, pais, codigo_piloto, numero_actual) VALUES
    ('TC:agustin_canapino', 'Agustín', 'Canapino', '1990-01-19', 'Argentina', 'Argentina', NULL, 86),
    ('TC:matias_rossi', 'Matías', 'Rossi', NULL, 'Argentina', 'Argentina', NULL, 117),
    ('TC:santiago_mangoni', 'Santiago', 'Mangoni', NULL, 'Argentina', 'Argentina', NULL, 9),
    ('TC:julian_santero', 'Julián', 'Santero', NULL, 'Argentina', 'Argentina', NULL, 1),
    ('TC:mauricio_lambiris', 'Mauricio', 'Lambiris', NULL, 'Uruguaya', 'Uruguay', NULL, 2),
    ('TC:mariano_werner', 'Mariano', 'Werner', NULL, 'Argentina', 'Argentina', NULL, 4),
    ('TC:christian_ledesma', 'Christian', 'Ledesma', NULL, 'Argentina', 'Argentina', NULL, 24),
    ('TC:otto_fritzler', 'Otto', 'Fritzler', NULL, 'Argentina', 'Argentina', NULL, 22),
    ('TC:jonatan_castellano', 'Jonatan', 'Castellano', NULL, 'Argentina', 'Argentina', NULL, 12),
    ('TC:facundo_chapur', 'Facundo', 'Chapur', NULL, 'Argentina', 'Argentina', NULL, 79)
ON CONFLICT (id_externo) WHERE id_externo IS NOT NULL DO NOTHING;

-- ---------------------------------------------------------------------
-- EQUIPO_X_PILOTO (rol usa los valores del codigo: Catalogo.ROL_PILOTO)
-- La marca del auto en TC queda pendiente para el MVP 6.
-- ---------------------------------------------------------------------
INSERT INTO equipo_x_piloto (id_equipo, id_piloto, id_categoria, temporada, numero, nro_piloto, rol, fecha_desde, fecha_hasta)
SELECT (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 2025, 86, NULL, 'PILOTO', '2025-02-14', '2025-12-07'
WHERE NOT EXISTS (SELECT 1 FROM equipo_x_piloto x WHERE x.id_equipo = (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport') AND x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino') AND x.temporada = 2025);
INSERT INTO equipo_x_piloto (id_equipo, id_piloto, id_categoria, temporada, numero, nro_piloto, rol, fecha_desde, fecha_hasta)
SELECT (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:pradecon_racing'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:matias_rossi'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 2025, 117, NULL, 'PILOTO', '2025-02-14', '2025-12-07'
WHERE NOT EXISTS (SELECT 1 FROM equipo_x_piloto x WHERE x.id_equipo = (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:pradecon_racing') AND x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:matias_rossi') AND x.temporada = 2025);
INSERT INTO equipo_x_piloto (id_equipo, id_piloto, id_categoria, temporada, numero, nro_piloto, rol, fecha_desde, fecha_hasta)
SELECT (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:santiago_mangoni'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 2025, 9, NULL, 'PILOTO', '2025-02-14', '2025-12-07'
WHERE NOT EXISTS (SELECT 1 FROM equipo_x_piloto x WHERE x.id_equipo = (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport') AND x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:santiago_mangoni') AND x.temporada = 2025);
INSERT INTO equipo_x_piloto (id_equipo, id_piloto, id_categoria, temporada, numero, nro_piloto, rol, fecha_desde, fecha_hasta)
SELECT (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:fispa_corse'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:julian_santero'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 2025, 1, NULL, 'PILOTO', '2025-02-14', '2025-12-07'
WHERE NOT EXISTS (SELECT 1 FROM equipo_x_piloto x WHERE x.id_equipo = (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:fispa_corse') AND x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:julian_santero') AND x.temporada = 2025);
INSERT INTO equipo_x_piloto (id_equipo, id_piloto, id_categoria, temporada, numero, nro_piloto, rol, fecha_desde, fecha_hasta)
SELECT (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:maquin_parts'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:mauricio_lambiris'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 2025, 2, NULL, 'PILOTO', '2025-02-14', '2025-12-07'
WHERE NOT EXISTS (SELECT 1 FROM equipo_x_piloto x WHERE x.id_equipo = (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:maquin_parts') AND x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:mauricio_lambiris') AND x.temporada = 2025);
INSERT INTO equipo_x_piloto (id_equipo, id_piloto, id_categoria, temporada, numero, nro_piloto, rol, fecha_desde, fecha_hasta)
SELECT (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:fadel_memo_corse'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:mariano_werner'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 2025, 4, NULL, 'PILOTO', '2025-02-14', '2025-12-07'
WHERE NOT EXISTS (SELECT 1 FROM equipo_x_piloto x WHERE x.id_equipo = (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:fadel_memo_corse') AND x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:mariano_werner') AND x.temporada = 2025);
INSERT INTO equipo_x_piloto (id_equipo, id_piloto, id_categoria, temporada, numero, nro_piloto, rol, fecha_desde, fecha_hasta)
SELECT (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:pradecon_racing'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:christian_ledesma'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 2025, 24, NULL, 'PILOTO', '2025-02-14', '2025-12-07'
WHERE NOT EXISTS (SELECT 1 FROM equipo_x_piloto x WHERE x.id_equipo = (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:pradecon_racing') AND x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:christian_ledesma') AND x.temporada = 2025);
INSERT INTO equipo_x_piloto (id_equipo, id_piloto, id_categoria, temporada, numero, nro_piloto, rol, fecha_desde, fecha_hasta)
SELECT (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:pradecon_racing'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:otto_fritzler'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 2025, 22, NULL, 'PILOTO', '2025-02-14', '2025-12-07'
WHERE NOT EXISTS (SELECT 1 FROM equipo_x_piloto x WHERE x.id_equipo = (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:pradecon_racing') AND x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:otto_fritzler') AND x.temporada = 2025);
INSERT INTO equipo_x_piloto (id_equipo, id_piloto, id_categoria, temporada, numero, nro_piloto, rol, fecha_desde, fecha_hasta)
SELECT (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:tomas_abdala_racing'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:jonatan_castellano'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 2025, 12, NULL, 'PILOTO', '2025-02-14', '2025-12-07'
WHERE NOT EXISTS (SELECT 1 FROM equipo_x_piloto x WHERE x.id_equipo = (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:tomas_abdala_racing') AND x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:jonatan_castellano') AND x.temporada = 2025);
INSERT INTO equipo_x_piloto (id_equipo, id_piloto, id_categoria, temporada, numero, nro_piloto, rol, fecha_desde, fecha_hasta)
SELECT (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:trotta_racing'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:facundo_chapur'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 2025, 79, NULL, 'PILOTO', '2025-02-14', '2025-12-07'
WHERE NOT EXISTS (SELECT 1 FROM equipo_x_piloto x WHERE x.id_equipo = (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:trotta_racing') AND x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:facundo_chapur') AND x.temporada = 2025);

-- =====================================================================
-- TURISMO CARRETERA - 15 fechas (10 pilotos con su equipo)
-- Sesion cargada: la Final de cada fecha. Hora: 13:00 hora argentina (16:00 UTC) es APROXIMADA,
-- salvo Toay (fecha 14), cuya final largo a las 13:25 (16:25 UTC).
-- Fecha 3 (Centenario): la final se suspendio por cuestiones climaticas.
-- =====================================================================

-- EVENTO TC
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '01', '1ª Fecha - Viedma', 'Viedma', 1, 'Argentina', 'Viedma, Río Negro', 'Autódromo Ciudad de Viedma', '2025-02-15 03:00:00', '2025-02-17 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '02', '2ª Fecha - El Calafate', 'El Calafate', 2, 'Argentina', 'El Calafate, Santa Cruz', 'Autódromo Enrique "Quique" Freile', '2025-03-08 03:00:00', '2025-03-10 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '03', '3ª Fecha - Centenario', 'Centenario', 3, 'Argentina', 'Centenario, Neuquén', 'Autódromo Parque Provincia del Neuquén', '2025-03-29 03:00:00', '2025-03-31 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '04', '4ª Fecha - Toay', 'Toay', 4, 'Argentina', 'Toay, La Pampa', 'Autódromo Provincia de La Pampa', '2025-04-19 03:00:00', '2025-04-21 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '05', '5ª Fecha - Termas de Río Hondo', 'Termas de Río Hondo', 5, 'Argentina', 'Termas de Río Hondo, Santiago del Estero', 'Autódromo Termas de Río Hondo', '2025-05-10 03:00:00', '2025-05-12 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '06', '6ª Fecha - Alta Gracia', 'Alta Gracia', 6, 'Argentina', 'Alta Gracia, Córdoba', 'Autódromo Oscar Cabalén', '2025-05-31 03:00:00', '2025-06-02 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '07', '7ª Fecha - Posadas', 'Posadas', 7, 'Argentina', 'Posadas, Misiones', 'Autódromo Rosamonte', '2025-06-21 03:00:00', '2025-06-23 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '08', '8ª Fecha - Concepción del Uruguay', 'Concepción del Uruguay', 8, 'Argentina', 'Concepción del Uruguay, Entre Ríos', 'Autódromo de Concepción del Uruguay', '2025-07-12 03:00:00', '2025-07-14 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '09', '9ª Fecha - Albardón', 'Albardón', 9, 'Argentina', 'Albardón, San Juan', 'Circuito San Juan Villicum', '2025-08-09 03:00:00', '2025-08-11 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '10', '10ª Fecha - Ciudad Autónoma de Buenos Aires', 'Ciudad Autónoma de Buenos Aires', 10, 'Argentina', 'Ciudad Autónoma de Buenos Aires', 'Autódromo Oscar y Juan Gálvez', '2025-08-23 03:00:00', '2025-08-25 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '11', '11ª Fecha - San Luis', 'San Luis', 11, 'Argentina', 'San Luis, San Luis', 'Autódromo Rosendo Hernández', '2025-09-13 03:00:00', '2025-09-15 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '12', '12ª Fecha - San Nicolás', 'San Nicolás', 12, 'Argentina', 'San Nicolás, Buenos Aires', 'Autódromo Juan María Traverso', '2025-10-04 03:00:00', '2025-10-06 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '13', '13ª Fecha - Paraná', 'Paraná', 13, 'Argentina', 'Paraná, Entre Ríos', 'Autódromo Ciudad de Paraná', '2025-11-01 03:00:00', '2025-11-03 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '14', '14ª Fecha - Toay', 'Toay', 14, 'Argentina', 'Toay, La Pampa', 'Autódromo Provincia de La Pampa', '2025-11-15 03:00:00', '2025-11-17 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
INSERT INTO evento (id_temporada, id_externo, nombre, nombre_corto, numero_fecha, pais, ciudad, circuito, fecha_inicio, fecha_fin, estado, zona_horaria) VALUES ((SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025), '15', '15ª Fecha - La Plata', 'La Plata', 15, 'Argentina', 'La Plata, Buenos Aires', 'Autódromo Roberto José Mouras', '2025-12-06 03:00:00', '2025-12-08 02:59:59', 'FINALIZADO', 'America/Argentina/Buenos_Aires')
ON CONFLICT (id_temporada, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;

-- SESION TC (Final)
-- Ganador: Jonatan Castellano (Dodge Challenger)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '01'), 'FINAL', 'Final', 'CARRERA', 1, '2025-02-16 16:00:00', '2025-02-16 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Facundo Chapur (Torino NG)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '02'), 'FINAL', 'Final', 'CARRERA', 1, '2025-03-09 16:00:00', '2025-03-09 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: sin ganador (final suspendida)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '03'), 'FINAL', 'Final', 'CARRERA', 1, '2025-03-30 16:00:00', '2025-03-30 17:00:00', NULL, NULL, 'CANCELADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Juan Bautista De Benedictis (Ford Mustang)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '04'), 'FINAL', 'Final', 'CARRERA', 1, '2025-04-20 16:00:00', '2025-04-20 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Mauricio Lambiris (Ford Mustang)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '05'), 'FINAL', 'Final', 'CARRERA', 1, '2025-05-11 16:00:00', '2025-05-11 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Mariano Werner (Ford Mustang)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '06'), 'FINAL', 'Final', 'CARRERA', 1, '2025-06-01 16:00:00', '2025-06-01 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Otto Fritzler (Toyota Camry)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '07'), 'FINAL', 'Final', 'CARRERA', 1, '2025-06-22 16:00:00', '2025-06-22 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Agustín Canapino (Chevrolet Camaro)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '08'), 'FINAL', 'Final', 'CARRERA', 1, '2025-07-13 16:00:00', '2025-07-13 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Martín Vázquez (Dodge Challenger)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '09'), 'FINAL', 'Final', 'CARRERA', 1, '2025-08-10 16:00:00', '2025-08-10 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Agustín Canapino (Chevrolet Camaro)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '10'), 'FINAL', 'Final', 'CARRERA', 1, '2025-08-24 16:00:00', '2025-08-24 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Agustín Canapino (Chevrolet Camaro)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '11'), 'FINAL', 'Final', 'CARRERA', 1, '2025-09-14 16:00:00', '2025-09-14 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Agustín Canapino (Chevrolet Camaro)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '12'), 'FINAL', 'Final', 'CARRERA', 1, '2025-10-05 16:00:00', '2025-10-05 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Agustín Canapino (Chevrolet Camaro)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '13'), 'FINAL', 'Final', 'CARRERA', 1, '2025-11-02 16:00:00', '2025-11-02 17:00:00', NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Christian Ledesma (Chevrolet Camaro)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '14'), 'FINAL', 'Final', 'CARRERA', 1, '2025-11-16 16:25:00', '2025-11-16 17:25:00', 30, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;
-- Ganador: Agustín Canapino (Chevrolet Camaro)
INSERT INTO sesion (id_evento, id_externo, nombre, tipo, numero, fecha_inicio, fecha_fin, vueltas_programadas, distancia, estado) VALUES ((SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '15'), 'FINAL', 'Final', 'CARRERA', 1, '2025-12-07 16:00:00', '2025-12-07 17:00:00', 25, NULL, 'FINALIZADO')
ON CONFLICT (id_evento, id_externo) WHERE id_externo IS NOT NULL DO NOTHING;

-- RESULTADO_SESION TC (solo posiciones confirmadas de los 10 pilotos cargados)
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '01') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:jonatan_castellano'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:tomas_abdala_racing'), 1, 12, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '01') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport'), 2, 86, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '02') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:facundo_chapur'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:trotta_racing'), 1, 79, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '05') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:mauricio_lambiris'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:maquin_parts'), 1, 2, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '06') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:mariano_werner'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:fadel_memo_corse'), 1, 4, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '07') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:otto_fritzler'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:pradecon_racing'), 1, 22, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '07') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport'), 2, 86, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '08') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport'), 1, 86, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '09') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:facundo_chapur'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:trotta_racing'), 2, 79, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '10') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport'), 1, 86, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '11') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport'), 1, 86, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '11') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:julian_santero'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:fispa_corse'), 2, 1, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '12') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport'), 1, 86, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '13') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport'), 1, 86, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '14') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:christian_ledesma'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:pradecon_racing'), 1, 24, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '14') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:julian_santero'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:fispa_corse'), 7, 1, NULL, NULL, NULL, NULL, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '15') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:canning_motorsport'), 1, 86, NULL, 2153.333, 0, 25, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '15') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:mariano_werner'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:fadel_memo_corse'), 2, 4, NULL, NULL, 3.584, 25, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '15') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:facundo_chapur'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:trotta_racing'), 5, 79, NULL, NULL, 16.384, 25, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '15') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:otto_fritzler'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:pradecon_racing'), 8, 22, NULL, NULL, 34.051, 25, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '15') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:matias_rossi'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:pradecon_racing'), 9, 117, NULL, NULL, 34.486, 25, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;
INSERT INTO resultado_sesion (id_sesion, id_piloto, id_equipo, posicion, numero, puntos, tiempo, diferencia, vueltas, estado) VALUES ((SELECT id_sesion FROM sesion WHERE id_evento = (SELECT id_evento FROM evento WHERE id_temporada = (SELECT t.id_temporada FROM temporada t JOIN categoria c ON c.id_categoria = t.id_categoria WHERE c.siglas = 'TC' AND t.anio = 2025) AND id_externo = '15') AND id_externo = 'FINAL'), (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:julian_santero'), (SELECT id_equipo FROM equipo WHERE id_externo = 'TC:fispa_corse'), 10, 1, NULL, NULL, 39.399, 25, 'FINALIZADO')
ON CONFLICT (id_sesion, id_piloto) DO NOTHING;

-- =====================================================================
-- PALMARES - estadisticas de la temporada 2025 (Copa de Oro ACTC). NULL = dato no relevado.
-- =====================================================================
INSERT INTO palmares (id_piloto, id_categoria, carreras, victorias, podios, poles, campeonatos, puntos_totales, abandonos)
SELECT (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 15, 6, 8, 7, 1, 239.5, NULL
WHERE NOT EXISTS (SELECT 1 FROM palmares x WHERE x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:agustin_canapino') AND x.id_categoria = (SELECT id_categoria FROM categoria WHERE siglas = 'TC'));
INSERT INTO palmares (id_piloto, id_categoria, carreras, victorias, podios, poles, campeonatos, puntos_totales, abandonos)
SELECT (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:matias_rossi'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 15, 0, NULL, NULL, 0, 157.25, NULL
WHERE NOT EXISTS (SELECT 1 FROM palmares x WHERE x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:matias_rossi') AND x.id_categoria = (SELECT id_categoria FROM categoria WHERE siglas = 'TC'));
INSERT INTO palmares (id_piloto, id_categoria, carreras, victorias, podios, poles, campeonatos, puntos_totales, abandonos)
SELECT (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:santiago_mangoni'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 15, 0, NULL, NULL, 0, 154.25, NULL
WHERE NOT EXISTS (SELECT 1 FROM palmares x WHERE x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:santiago_mangoni') AND x.id_categoria = (SELECT id_categoria FROM categoria WHERE siglas = 'TC'));
INSERT INTO palmares (id_piloto, id_categoria, carreras, victorias, podios, poles, campeonatos, puntos_totales, abandonos)
SELECT (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:julian_santero'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 15, 0, NULL, NULL, 0, 147.5, NULL
WHERE NOT EXISTS (SELECT 1 FROM palmares x WHERE x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:julian_santero') AND x.id_categoria = (SELECT id_categoria FROM categoria WHERE siglas = 'TC'));
INSERT INTO palmares (id_piloto, id_categoria, carreras, victorias, podios, poles, campeonatos, puntos_totales, abandonos)
SELECT (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:mauricio_lambiris'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 15, 1, NULL, NULL, 0, 133, NULL
WHERE NOT EXISTS (SELECT 1 FROM palmares x WHERE x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:mauricio_lambiris') AND x.id_categoria = (SELECT id_categoria FROM categoria WHERE siglas = 'TC'));
INSERT INTO palmares (id_piloto, id_categoria, carreras, victorias, podios, poles, campeonatos, puntos_totales, abandonos)
SELECT (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:mariano_werner'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 15, 1, NULL, NULL, 0, 121.5, NULL
WHERE NOT EXISTS (SELECT 1 FROM palmares x WHERE x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:mariano_werner') AND x.id_categoria = (SELECT id_categoria FROM categoria WHERE siglas = 'TC'));
INSERT INTO palmares (id_piloto, id_categoria, carreras, victorias, podios, poles, campeonatos, puntos_totales, abandonos)
SELECT (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:christian_ledesma'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 15, 1, NULL, NULL, 0, 113, NULL
WHERE NOT EXISTS (SELECT 1 FROM palmares x WHERE x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:christian_ledesma') AND x.id_categoria = (SELECT id_categoria FROM categoria WHERE siglas = 'TC'));
INSERT INTO palmares (id_piloto, id_categoria, carreras, victorias, podios, poles, campeonatos, puntos_totales, abandonos)
SELECT (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:otto_fritzler'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 15, 1, NULL, NULL, 0, 98.25, NULL
WHERE NOT EXISTS (SELECT 1 FROM palmares x WHERE x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:otto_fritzler') AND x.id_categoria = (SELECT id_categoria FROM categoria WHERE siglas = 'TC'));
INSERT INTO palmares (id_piloto, id_categoria, carreras, victorias, podios, poles, campeonatos, puntos_totales, abandonos)
SELECT (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:jonatan_castellano'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 15, 1, NULL, NULL, 0, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM palmares x WHERE x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:jonatan_castellano') AND x.id_categoria = (SELECT id_categoria FROM categoria WHERE siglas = 'TC'));
INSERT INTO palmares (id_piloto, id_categoria, carreras, victorias, podios, poles, campeonatos, puntos_totales, abandonos)
SELECT (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:facundo_chapur'), (SELECT id_categoria FROM categoria WHERE siglas = 'TC'), 15, 1, NULL, NULL, 0, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM palmares x WHERE x.id_piloto = (SELECT id_piloto FROM piloto WHERE id_externo = 'TC:facundo_chapur') AND x.id_categoria = (SELECT id_categoria FROM categoria WHERE siglas = 'TC'));

COMMIT;
