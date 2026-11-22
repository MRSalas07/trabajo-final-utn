package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.EventoDtos;

import java.util.List;

/** Gestión de eventos (un Gran Premio, un rally). La baja oculta también sus sesiones y etapas. */
public interface IEventoGestionService {

    List<EventoDtos.Registro> listar(Long temporadaId);

    EventoDtos.Registro crear(EventoDtos.Request r, Long usuarioId);

    EventoDtos.Registro modificar(Long id, EventoDtos.Request r, Long usuarioId);

    void eliminar(Long id, Long usuarioId);
}
