-- Datos iniciales del sistema. Se ejecuta despues de schema.sql.
-- Es idempotente: puede ejecutarse mas de una vez sin duplicar datos.

-- ---------------------------------------------------------------------
-- Categorias contempladas en el MVP
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- Usuario administrador para el entorno LOCAL.
--   Email: admin@motorsport.local   Password: Admin2025!
-- El rol ADMIN no se guarda en la base: se asigna al iniciar sesion si el email
-- figura en la variable ADMIN_EMAILS del backend (agregar admin@motorsport.local al .env).
-- Hash BCrypt ($2a$, cost 10), compatible con BCryptPasswordEncoder.
-- NO usar este usuario en produccion: alli registrarse desde la app con un email propio.
-- ---------------------------------------------------------------------
INSERT INTO usuario (nombre, apellido, email, password_hash, zona_horaria, fecha_registro, activo)
VALUES ('Admin', 'Sistema', 'admin@motorsport.local',
        '$2a$10$ApF3DUwqWLnhTLCNXJuDNuZueObHMF1jpYJfHOrTpPTd97L.kkyjq',
        'America/Argentina/Buenos_Aires', CURRENT_TIMESTAMP, TRUE)
ON CONFLICT ((lower(email))) DO NOTHING;
