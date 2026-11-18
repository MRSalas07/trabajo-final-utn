package com.tfi.motorsport.backend.services;

import com.tfi.motorsport.backend.dtos.calendario.ArchivoIcs;

/** Exportación del calendario en formato iCalendar (.ics). */
public interface ICalendarioService {

    /** Un evento: una entrada por sesión (F1 y TC) o una sola para todo el rally (WRC). */
    ArchivoIcs icsDeEvento(Long eventoId);

    /** Una temporada completa: una entrada por evento (la carrera principal o, si no tiene, el evento completo). */
    ArchivoIcs icsDeTemporada(String siglas, int anio);
}
