package com.tfi.motorsport.backend.clients.normalization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Estado de UNA ejecución de la normalización (una temporada que se importa o sincroniza).
 *
 * Se crea uno nuevo en cada llamada a {@link NormalizadorService#persistir}. No puede ser un campo
 * de los servicios porque estos son únicos (singleton): dos importaciones simultáneas mezclarían
 * sus contadores y cachés.
 *
 * Contiene:
 *  - los datos fijos de la ejecución (categoría y año), para no pasarlos por todos los métodos;
 *  - los avisos que verá el usuario;
 *  - los contadores del resumen;
 *  - cachés para no repetir consultas (un mismo piloto aparece en decenas de resultados).
 */
final class ContextoNormalizacion {

    final Long categoriaId;
    final Integer anio;
    final List<String> avisos;

    /** idExterno del piloto → id en la base. */
    final Map<String, Long> pilotos = new HashMap<>();
    /** idExterno del equipo → id en la base. */
    final Map<String, Long> equipos = new HashMap<>();
    /** Participaciones (equipo-piloto-rol) ya registradas en esta ejecución. */
    final Set<String> participaciones = new HashSet<>();
    /** Marca ya aplicada a cada participación (equipo-piloto-rol) en esta ejecución. */
    final Map<String, String> marcas = new HashMap<>();

    int eventos, omitidos, sesiones, etapas, resultados, pilotosNuevos, equiposNuevos;

    ContextoNormalizacion(Long categoriaId, Integer anio, List<String> avisos) {
        this.categoriaId = categoriaId;
        this.anio = anio;
        this.avisos = avisos;
    }
}
