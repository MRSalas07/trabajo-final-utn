package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.AuditableEntity;
import com.tfi.motorsport.backend.model.Catalogo.EstadoEvento;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Equipo;
import com.tfi.motorsport.backend.model.EquipoPiloto;
import com.tfi.motorsport.backend.model.Etapa;
import com.tfi.motorsport.backend.model.Evento;
import com.tfi.motorsport.backend.model.Piloto;
import com.tfi.motorsport.backend.model.Sesion;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.model.Tripulacion;
import com.tfi.motorsport.backend.model.Usuario;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Datos de prueba de los servicios de gestión (MVP 5): entidades armadas a mano, sin base de datos.
 *
 * Los tests usan un SoporteGestion real (con el repositorio de usuarios simulado) para verificar
 * la auditoría de verdad: quién creó, modificó o dio de baja cada registro.
 */
final class GestionDePrueba {

    /** Administrador que hace las operaciones en los tests. */
    static final Long ADMIN_ID = 7L;
    static final String ADMIN_EMAIL = "admin@motorsport.local";

    private GestionDePrueba() {
    }

    static Usuario admin() {
        return usuario(ADMIN_ID, ADMIN_EMAIL);
    }

    static Usuario usuario(Long id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    static Categoria categoria(Long id, String siglas) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setSiglas(siglas);
        c.setNombre(siglas);
        c.setActivo(true);
        return c;
    }

    static Temporada temporada(Long id, Categoria categoria, int anio) {
        Temporada t = new Temporada();
        t.setId(id);
        t.setCategoria(categoria);
        t.setAnio(anio);
        t.setNombre(categoria.getSiglas() + " " + anio);
        return t;
    }

    static Evento evento(Long id, Temporada temporada) {
        Evento e = new Evento();
        e.setId(id);
        e.setTemporada(temporada);
        e.setNombre("Evento " + id);
        e.setZonaHoraria("UTC");
        e.setFechaInicio(Instant.parse("2026-03-08T04:00:00Z"));
        e.setFechaFin(Instant.parse("2026-03-08T04:00:00Z"));
        e.setEstado(EstadoEvento.PROGRAMADO);
        return e;
    }

    static Sesion sesion(Long id, Evento evento) {
        Sesion s = new Sesion();
        s.setId(id);
        s.setEvento(evento);
        s.setNombre("Carrera");
        s.setEstado(EstadoEvento.PROGRAMADO);
        return s;
    }

    static Etapa etapa(Long id, Evento evento) {
        Etapa et = new Etapa();
        et.setId(id);
        et.setEvento(evento);
        et.setNombre("Etapa " + id);
        et.setEstado(EstadoEvento.PROGRAMADO);
        return et;
    }

    static Piloto piloto(Long id, String nombre, String apellido) {
        Piloto p = new Piloto();
        p.setId(id);
        p.setNombre(nombre);
        p.setApellido(apellido);
        return p;
    }

    static Equipo equipo(Long id, String nombre) {
        Equipo e = new Equipo();
        e.setId(id);
        e.setNombre(nombre);
        return e;
    }

    static EquipoPiloto participacion(Long id, Equipo equipo, Piloto piloto, Categoria categoria, int temporada,
                                      String rol) {
        EquipoPiloto ep = new EquipoPiloto();
        ep.setId(id);
        ep.setEquipo(equipo);
        ep.setPiloto(piloto);
        ep.setCategoria(categoria);
        ep.setTemporada(temporada);
        ep.setRol(rol);
        return ep;
    }

    static Tripulacion tripulacion(Long id, Categoria categoria, int temporada, Piloto piloto, Piloto copiloto,
                                   Equipo equipo) {
        Tripulacion t = new Tripulacion();
        t.setId(id);
        t.setCategoria(categoria);
        t.setTemporada(temporada);
        t.setPiloto(piloto);
        t.setCopiloto(copiloto);
        t.setEquipo(equipo);
        return t;
    }

    /** Marca el registro como dado de baja (baja lógica). */
    static <T extends AuditableEntity> T dadoDeBaja(T entidad) {
        entidad.setEliminado(true);
        return entidad;
    }

    /** Para isInstanceOfSatisfying: el error de negocio responde con ese estado HTTP. */
    static Consumer<RuleBusinessException> conEstado(HttpStatus estado) {
        return error -> assertThat(error.getEstado()).isEqualTo(estado);
    }
}
