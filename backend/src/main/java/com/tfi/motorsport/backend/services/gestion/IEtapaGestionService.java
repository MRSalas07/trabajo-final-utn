package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.EtapaDtos;

import java.util.List;

/** Gestión de etapas de rally. Solo puede haber una clasificación general por evento. */
public interface IEtapaGestionService {

    List<EtapaDtos.Registro> listar(Long eventoId);

    EtapaDtos.Registro crear(EtapaDtos.Request r, Long usuarioId);

    EtapaDtos.Registro modificar(Long id, EtapaDtos.Request r, Long usuarioId);

    void eliminar(Long id, Long usuarioId);
}
