package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.perfil.ParticipacionDto;
import com.tfi.motorsport.backend.model.EquipoPiloto;
import com.tfi.motorsport.backend.model.Piloto;

/** Conversión de equipo_x_piloto a ParticipacionDto, común a la trayectoria del piloto y al plantel del equipo. */
public final class Participaciones {

    private Participaciones() {
    }

    /** Requiere que equipo, piloto y categoría de la participación estén cargados (join fetch o misma transacción). */
    public static ParticipacionDto de(EquipoPiloto ep) {
        Piloto piloto = ep.getPiloto();
        return new ParticipacionDto(ep.getTemporada(), ep.getCategoria().getSiglas(), ep.getEquipo().getId(),
                ep.getEquipo().getNombre(), piloto.getId(), nombreCompleto(piloto), ep.getNumero(), ep.getRol());
    }

    public static String nombreCompleto(Piloto p) {
        return ((p.getNombre() != null ? p.getNombre() + " " : "") + p.getApellido()).trim();
    }
}
