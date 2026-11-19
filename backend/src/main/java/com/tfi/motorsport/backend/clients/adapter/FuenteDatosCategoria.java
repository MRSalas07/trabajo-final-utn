package com.tfi.motorsport.backend.clients.adapter;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.*;

/**
 * Contrato de un ADAPTADOR de datos externos (patrón Adapter / Strategy).
 *
 * Cada implementación sabe hablar con un proveedor concreto y traducir su formato
 * al modelo normalizado. El servicio de sincronización elige el adaptador según
 * las siglas de la categoría, sin conocer los detalles de cada API.
 */
public interface FuenteDatosCategoria {

    /** Siglas de la categoría que cubre esta fuente (ej: "F1"). */
    String siglasCategoria();

    /** Nombre del proveedor, para logs y para el resumen de sincronización. */
    String proveedor();

    /** Indica si la fuente está habilitada por configuración. */
    default boolean habilitada() {
        return true;
    }

    /**
     * Descarga y normaliza una temporada completa: calendario, sesiones/etapas y resultados.
     *
     * @param anio  temporada a sincronizar
     * @param avisos lista donde el adaptador agrega advertencias no fatales
     *               (ej: "evento X sin resultados publicados")
     */
    TemporadaNorm obtenerTemporada(int anio, java.util.List<String> avisos);
}
