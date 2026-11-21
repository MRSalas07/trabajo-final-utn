-- Migracion 002 - Campeon oficial de cada temporada
-- Se ejecuta DESPUES de schema.sql y 001. Es idempotente: puede correrse varias veces.
--
-- Hasta 1990 la F1 contaba solo los mejores N resultados de cada piloto, asi que el campeon
-- no siempre es el que mas puntos sumo (ej: 1988, Senna campeon con menos puntos totales que Prost).
-- Se guarda el campeon que informa la fuente (tabla oficial) y el palmares lo usa para contar titulos.
-- La clave foranea va aca porque en schema.sql la tabla piloto se crea despues de temporada.

ALTER TABLE temporada ADD COLUMN IF NOT EXISTS id_piloto_campeon BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_temporada_piloto_campeon') THEN
        ALTER TABLE temporada
            ADD CONSTRAINT fk_temporada_piloto_campeon
            FOREIGN KEY (id_piloto_campeon) REFERENCES piloto(id_piloto) ON DELETE SET NULL;
    END IF;
END $$;
