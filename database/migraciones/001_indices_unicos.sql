-- Migracion 001 - Entrega 2
-- SOLO para bases creadas con una version de schema.sql anterior a noviembre de 2026.
-- Una base nueva NO la necesita: schema.sql ya incluye todo lo de este archivo.
-- Es idempotente (IF NOT EXISTS): correrla sobre una base nueva no cambia nada.
--
-- 1) Agrega evento.zona_horaria (la entidad JPA Evento ya la usa, pero schema.sql
--    no la tenia). Guarda la zona IANA de la sede (ej: "America/Argentina/Buenos_Aires")
--    para generar archivos .ics con la hora local del circuito.
-- 2) Agrega indices unicos sobre id_externo para que la sincronizacion con APIs
--    externas nunca duplique registros (se busca por id_externo antes de insertar).
--
-- Convencion de fechas: todas las columnas TIMESTAMP WITHOUT TIME ZONE de evento,
-- sesion y etapa se guardan en UTC. La conversion a la hora del usuario la hace el frontend.

ALTER TABLE evento ADD COLUMN IF NOT EXISTS zona_horaria VARCHAR(64) NOT NULL DEFAULT 'UTC';

-- id_externo es unico dentro de su "padre" (una temporada puede repetir numeros de otra)
CREATE UNIQUE INDEX IF NOT EXISTS ux_evento_temporada_externo
    ON evento (id_temporada, id_externo) WHERE id_externo IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_sesion_evento_externo
    ON sesion (id_evento, id_externo) WHERE id_externo IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_etapa_evento_externo
    ON etapa (id_evento, id_externo) WHERE id_externo IS NOT NULL;

-- Pilotos y equipos son globales: el id_externo lleva prefijo de categoria (ej: "F1:max_verstappen")
CREATE UNIQUE INDEX IF NOT EXISTS ux_piloto_externo
    ON piloto (id_externo) WHERE id_externo IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_equipo_externo
    ON equipo (id_externo) WHERE id_externo IS NOT NULL;

-- Una temporada por categoria y anio
CREATE UNIQUE INDEX IF NOT EXISTS ux_temporada_categoria_anio
    ON temporada (id_categoria, anio);

-- Un solo resultado por piloto en cada sesion, y por tripulacion en cada etapa
CREATE UNIQUE INDEX IF NOT EXISTS ux_resultado_sesion_piloto
    ON resultado_sesion (id_sesion, id_piloto);

CREATE UNIQUE INDEX IF NOT EXISTS ux_resultado_etapa_tripulacion
    ON resultado_etapa (id_etapa, id_tripulacion);

-- Email de usuario unico (login)
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuario_email
    ON usuario (lower(email));

-- Indices de apoyo para el calendario (consultas por rango de fechas)
CREATE INDEX IF NOT EXISTS idx_evento_fecha_inicio ON evento (fecha_inicio);
CREATE INDEX IF NOT EXISTS idx_sesion_fecha_inicio ON sesion (fecha_inicio);
