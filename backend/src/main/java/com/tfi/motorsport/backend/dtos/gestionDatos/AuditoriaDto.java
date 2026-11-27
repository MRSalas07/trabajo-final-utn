package com.tfi.motorsport.backend.dtos.gestionDatos;

import com.tfi.motorsport.backend.model.AuditableEntity;
import com.tfi.motorsport.backend.model.Usuario;

import java.time.Instant;

/**
 * Auditoría de un registro, para mostrarla en el panel de administración: quién lo creó, quién lo
 * modificó por última vez y cuándo (columnas creadopor, actualizadopor, "fechaCreacion" y
 * "fechaActualizacion" de {@link AuditableEntity}).
 *
 * Un usuario en null significa que el registro lo cargó el sistema (sincronización con las APIs,
 * importación de JSON o scripts SQL), no una persona desde el ABM.
 * Las fechas las completa la base (valor por defecto y trigger), así que en un alta recién hecha
 * pueden venir vacías hasta que se vuelve a consultar el registro.
 */
public record AuditoriaDto(String creadoPor, Instant fechaCreacion, String actualizadoPor, Instant fechaActualizacion) {

    /** Debe llamarse dentro de una transacción: los usuarios se cargan de forma perezosa. */
    public static AuditoriaDto de(AuditableEntity entidad) {
        return new AuditoriaDto(email(entidad.getCreadoPor()), entidad.getFechaCreacion(),
                email(entidad.getActualizadoPor()), entidad.getFechaActualizacion());
    }

    private static String email(Usuario usuario) {
        return usuario != null ? usuario.getEmail() : null;
    }
}
