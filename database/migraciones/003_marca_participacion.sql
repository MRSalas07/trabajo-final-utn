-- Migracion 003 - Marca de cada piloto en su participacion (equipo_x_piloto)
-- SOLO para bases creadas antes de esta migracion. Una base nueva NO la necesita: schema.sql
-- ya incluye la columna. Es idempotente: correrla de mas no cambia nada.
--
-- En TC la marca (el motor: Ford, Chevrolet, Dodge, Torino, Toyota y desde 2026 Mercedes-Benz y BMW)
-- no es del equipo: un mismo equipo puede correr autos de distintas marcas y un piloto puede cambiar
-- de marca entre temporadas. Por eso se guarda en la participacion (piloto + equipo + temporada).
-- NULL = la marca es la del equipo (equipo.nombre_corto), que es lo que pasa en F1 y WRC.

ALTER TABLE equipo_x_piloto ADD COLUMN IF NOT EXISTS marca VARCHAR(40);
