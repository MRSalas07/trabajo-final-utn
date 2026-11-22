package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.AuditableEntity;
import com.tfi.motorsport.backend.model.Piloto;
import com.tfi.motorsport.backend.repositories.UsuarioJPARepository;
import com.tfi.motorsport.backend.services.Impl.Participaciones;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Utilidades que comparten todos los servicios de gestión.
 *
 * Se inyecta en cada servicio (composición) en lugar de usar una clase padre (herencia):
 * cada servicio declara solo lo que usa y no queda atado a una jerarquía.
 *
 *  - activa(): busca por id y exige que no esté dada de baja (404 en ambos casos).
 *  - alta() / cambio() / baja(): auditoría (quién creó / modificó / dio de baja).
 *  - Métodos estáticos: validaciones y normalización de textos que no necesitan la base.
 */
@Component
@RequiredArgsConstructor
public class SoporteGestion {

    /** Tope de los listados de pilotos y equipos (con F1 desde 1950 hay ~900 pilotos). */
    public static final int LIMITE_LISTADO = 500;

    private final UsuarioJPARepository usuarioRepo;

    /** Busca por id y exige que no esté dada de baja (404 en ambos casos). */
    public <T extends AuditableEntity> T activa(JpaRepository<T, Long> repo, Long id, String descripcion) {
        if (id == null) {
            throw new RuleBusinessException("Falta indicar " + descripcion);
        }
        T entidad = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException(descripcion, id));
        if (entidad.isEliminado()) {
            throw new ResourceNotFoundException(descripcion, id);
        }
        return entidad;
    }

    public void alta(AuditableEntity e, Long usuarioId) {
        usuarioRepo.findById(usuarioId == null ? -1L : usuarioId).ifPresent(u -> {
            e.setCreadoPor(u);
            e.setActualizadoPor(u);
        });
    }

    public void cambio(AuditableEntity e, Long usuarioId) {
        usuarioRepo.findById(usuarioId == null ? -1L : usuarioId).ifPresent(e::setActualizadoPor);
    }

    /** Baja lógica: se marca como eliminado y queda registrado quién lo hizo. */
    public void baja(AuditableEntity e, Long usuarioId) {
        e.setEliminado(true);
        cambio(e, usuarioId);
    }

    public static void validarFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new RuleBusinessException("La fecha de fin no puede ser anterior a la de inicio");
        }
    }

    public static void validarInstantes(Instant desde, Instant hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new RuleBusinessException("La fecha y hora de fin no puede ser anterior a la de inicio");
        }
    }

    /** Texto recortado, o null si vino vacío. */
    public static String texto(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Patrón para un LIKE: "%texto%" en minúsculas. */
    public static String patron(String texto) {
        return "%" + (texto == null ? "" : texto.trim().toLowerCase()) + "%";
    }

    public static String nombre(Piloto p) {
        return Participaciones.nombreCompleto(p);
    }
}
