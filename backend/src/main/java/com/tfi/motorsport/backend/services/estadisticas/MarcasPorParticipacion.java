package com.tfi.motorsport.backend.services.estadisticas;

import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import com.tfi.motorsport.backend.repositories.unificacion.MarcaParticipacion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Completa la marca de cada fila de resultados con la cargada en la participación del piloto
 * (equipo_x_piloto.marca). Hace falta en TC, donde la marca es el motor del auto y un mismo equipo
 * puede correr autos de distintas marcas. En F1 y WRC no se cargan marcas y sigue valiendo la del
 * equipo (equipo.nombre_corto).
 *
 * Regla, temporada por temporada:
 * <ul>
 *   <li>si la participación del piloto con ese equipo tiene marca, se usa esa;</li>
 *   <li>si la temporada tiene marcas cargadas pero esa participación no, queda "Sin marca"
 *       (así el ranking no mezcla marcas con nombres de equipo);</li>
 *   <li>si la temporada no tiene ninguna marca cargada, la fila queda como estaba.</li>
 * </ul>
 */
public final class MarcasPorParticipacion {

    /** Grupo de las participaciones sin marca en una temporada que sí tiene marcas cargadas. */
    public static final String SIN_MARCA = "Sin marca";

    private MarcasPorParticipacion() {
    }

    public static List<FilaResultado> aplicar(List<FilaResultado> filas, List<MarcaParticipacion> marcas) {
        Map<String, String> marcaPorParticipacion = new HashMap<>();
        Set<Integer> aniosConMarcas = new HashSet<>();
        for (MarcaParticipacion m : marcas) {
            if (m.marca() == null || m.marca().isBlank() || m.anio() == null) {
                continue;
            }
            marcaPorParticipacion.putIfAbsent(clave(m.pilotoId(), m.equipoId(), m.anio()), m.marca().trim());
            aniosConMarcas.add(m.anio());
        }
        if (aniosConMarcas.isEmpty()) {
            return filas;
        }
        List<FilaResultado> conMarcas = new ArrayList<>(filas.size());
        for (FilaResultado fila : filas) {
            if (!aniosConMarcas.contains(fila.anio())) {
                conMarcas.add(fila);
                continue;
            }
            String marca = marcaPorParticipacion.getOrDefault(clave(fila.pilotoId(), fila.equipoId(), fila.anio()), SIN_MARCA);
            conMarcas.add(fila.conMarca(marca));
        }
        return conMarcas;
    }

    private static String clave(Long pilotoId, Long equipoId, Integer anio) {
        return pilotoId + "-" + equipoId + "-" + anio;
    }
}
