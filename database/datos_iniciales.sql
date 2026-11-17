-- Datos iniciales para las categorias contempladas en el MVP.
-- Puede ejecutarse mas de una vez sin duplicar siglas existentes.

INSERT INTO categoria (nombre, siglas, descripcion, pais_origen, activo)
SELECT datos.nombre, datos.siglas, datos.descripcion, datos.pais_origen, TRUE
FROM (
    VALUES
        ('Formula 1', 'F1', 'Campeonato internacional de monoplazas.', 'Internacional'),
        ('World Rally Championship', 'WRC', 'Campeonato mundial de rally.', 'Internacional'),
        ('Turismo Carretera', 'TC', 'Categoria de automovilismo de velocidad argentina.', 'Argentina')
) AS datos(nombre, siglas, descripcion, pais_origen)
WHERE NOT EXISTS (
    SELECT 1
    FROM categoria existente
    WHERE existente.siglas = datos.siglas
);