package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.repositories.*;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import com.tfi.motorsport.backend.services.IEstadisticaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico;
import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class EstadisticaServiceImpl implements IEstadisticaService {
    private static final int TOP_MAXIMO = 20;

    private final PilotoJPARepository pilotoRepo;
    private final TemporadaJPARepository temporadaRepo;
    private final EventoJPARepository eventoRepo;
    private final ResultadoSesionJPARepository resultadoSesionRepo;
    private final ResultadoEtapaJPARepository resultadoEtapaRepo;


    @Override
    public int recalcularPalmares(String siglas) {
        return 0;
    }

    @Override
    public List<FilaCampeonatoPiloto> campeonatoPilotos(String siglas, int anio) {
        Temporada temporada = buscarTemporada(siglas, anio);
        List<Participacion> participaciones = consolidarParticipacionesDe(temporada);
        return MotorEstadistico.campeonatoPilotos(participaciones);
    }

    @Override
    public List<FilaCampeonatoEquipo> campeonatoEquipos(String siglas, int anio) {
        Temporada temporada = buscarTemporada(siglas, anio);
        List<Participacion> participaciones = consolidarParticipacionesDe(temporada);
        return  MotorEstadistico.campeonatoEquipos(participaciones);
    }

    @Override
    public ResumenTemporada resumen(String siglas, int anio) {
        Temporada temporada = buscarTemporada(siglas, anio);
        // Se consolida una sola vez y se reutiliza: repetirlo duplicaría las consultas a la base
        List<Participacion> participaciones = consolidarParticipacionesDe(temporada);

        List<FilaCampeonatoPiloto> tablaPilotos = MotorEstadistico.campeonatoPilotos(participaciones);
        List<FilaCampeonatoEquipo> tablaEquipos = MotorEstadistico.campeonatoEquipos(participaciones);
        List<GanadorEvento> ganadores = MotorEstadistico.ganadores(participaciones);

        int eventosEnElCalendario = eventoRepo.listarPorTemporada(temporada.getId()).size();
        int rondasDisputadas = MotorEstadistico.rondas(participaciones).size();
        // Las tablas ya vienen ordenadas: el líder es la primera fila (o null si todavía no hay resultados)
        FilaCampeonatoPiloto pilotoLider = tablaPilotos.isEmpty() ? null : tablaPilotos.get(0);
        FilaCampeonatoEquipo equipoLider = tablaEquipos.isEmpty() ? null : tablaEquipos.get(0);
        int pilotosConVictoria = (int) ganadores.stream().map(GanadorEvento::pilotoId).distinct().count();

        return new ResumenTemporada(temporada.getId(), temporada.getCategoria().getSiglas(), temporada.getAnio(),
                eventosEnElCalendario, rondasDisputadas, pilotoLider, equipoLider, pilotosConVictoria, ganadores);
    }

    @Override
    public EvolucionCampeonato evolucionPilotos(String siglas, int anio, List<Long> pilotoIds, int top) {
        if (pilotoIds != null && pilotoIds.size() > TOP_MAXIMO) {
            throw new RuleBusinessException("Se pueden comparar como máximo " + TOP_MAXIMO + " pilotos");
        }
        int topAcotado = Math.max(1, Math.min(top, TOP_MAXIMO));
        Temporada temporada = buscarTemporada(siglas, anio);
        return MotorEstadistico.evolucion(consolidarParticipacionesDe(temporada), pilotoIds, topAcotado);
    }

    @Override
    public TendenciaPiloto tendenciaPiloto(String siglas, int anio, Long pilotoId) {
        verificarQueExistaElPiloto(pilotoId);
        Temporada temporada = buscarTemporada(siglas, anio);
        return MotorEstadistico.tendenciadePiloto(consolidarParticipacionesDe(temporada), pilotoId);
    }

    @Override
    public ComparativaPilotos compararPilotos(String siglas, int anio, Long pilotoA, Long pilotoB) {
        if (Objects.equals(pilotoA, pilotoB)) {
            throw new RuleBusinessException("Seleccione dos pilotos distintos para comparar");
        }
        verificarQueExistaElPiloto(pilotoA);
        verificarQueExistaElPiloto(pilotoB);
        Temporada temporada = buscarTemporada(siglas, anio);
        return MotorEstadistico.comparativaPilotos(consolidarParticipacionesDe(temporada), pilotoA, pilotoB);
    }



    private List<Participacion> consolidarParticipacionesDe(Temporada t) {
        List<FilaResultado> filas = new ArrayList<>(resultadoSesionRepo.filasDeTemporada(t.getId()));
        filas.addAll(resultadoEtapaRepo.filasDeTemporada(t.getId()));
        return MotorEstadistico.consolidar(filas);
    }

    private Temporada buscarTemporada(String siglas, int anio) {
        return temporadaRepo.buscarPorSiglasYAnio(siglas, anio).orElseThrow(() -> new ResourceNotFoundException(
                "No hay datos de la temporada " + anio + " de " + siglas.toUpperCase()));
    }

    private void verificarQueExistaElPiloto(Long pilotoId) {
        if (pilotoId == null || !pilotoRepo.existsById(pilotoId)) {
            throw new ResourceNotFoundException("el piloto", pilotoId);
        }
    }



}
