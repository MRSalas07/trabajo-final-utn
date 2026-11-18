package com.tfi.motorsport.backend.dtos.calendario;

/** Archivo de calendario listo para descargar: nombre sugerido y contenido en formato iCalendar. */
public record ArchivoIcs(
        String nombreArchivo,
        String contenido
) {
}
