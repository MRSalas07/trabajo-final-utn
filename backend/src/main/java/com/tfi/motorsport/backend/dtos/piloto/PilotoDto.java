package com.tfi.motorsport.backend.dtos.piloto;

import java.time.LocalDate;

/** Datos personales de un piloto (interfaz Piloto del frontend). */
public record PilotoDto(
        Long id,
        String nombre,
        String apellido,
        String codigo,
        Integer numero,
        String nacionalidad,
        String pais,
        LocalDate fechaNacimiento,
        Integer edad,
        String fotoUrl
) {
}
