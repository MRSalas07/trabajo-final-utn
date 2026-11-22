package com.tfi.motorsport.backend.services.gestion;

import com.tfi.motorsport.backend.dtos.gestionDatos.ParticipacionDtos;

import java.util.List;

/** Gestión de participaciones (tabla equipo_x_piloto: un piloto en un equipo en una temporada). */
public interface IParticipacionGestionService {

    List<ParticipacionDtos.Registro> listar(Long categoriaId, Integer anio);

    ParticipacionDtos.Registro crear(ParticipacionDtos.Request r, Long usuarioId);

    ParticipacionDtos.Registro modificar(Long id, ParticipacionDtos.Request r, Long usuarioId);

    void eliminar(Long id, Long usuarioId);

    /**
     * Registra la participación si no existía (o la reactiva si estaba dada de baja).
     * La usan los Gestión de tripulaciones y de resultados de sesión, igual que la sincronización.
     */
    void registrarSiNoExiste(Long equipoId, Long pilotoId, Long categoriaId, Integer anio, Integer numero,
                             String rol, Long usuarioId);
}
