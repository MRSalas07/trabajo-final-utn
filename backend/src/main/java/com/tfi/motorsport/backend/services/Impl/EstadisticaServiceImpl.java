package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto.*;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.*;
import com.tfi.motorsport.backend.repositories.*;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import com.tfi.motorsport.backend.services.IEstadisticaService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.tfi.motorsport.backend.model.Catalogo.*;
import com.tfi.motorsport.backend.services.estadisticas.AnalisisEquiposYMarcas;
import com.tfi.motorsport.backend.services.estadisticas.AnalisisHistorico;
import com.tfi.motorsport.backend.services.estadisticas.MarcasPorParticipacion;
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
    private final CategoriaJPARepository categoriaRepo;
    private final PalmaresJPARepository palmaresRepo;
    private final EquipoJPARepository equipoRepo;
    private final EquipoPilotoJPARepository participacionRepo;

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

    /**
     * PALMARÉS: estadística histórica por piloto en una categoría, calculada con todas las
     * temporadas almacenadas. Un campeonato se cuenta solo si la temporada terminó
     * (todos sus eventos finalizados o cancelados) y el piloto quedó primero en la tabla calculada.
     *
     * @return cantidad de pilotos cuyo palmarés se actualizó
     */
    @Override
    @Transactional
    public int recalcularPalmares(String siglas) {
        Categoria categoria = buscarCategoria(siglas);
        List<Participacion> participacionesDeLaCategoria = consolidarParticipacionesDeCategoria(categoria);
        Map<Long, Integer> campeonatosPorPiloto = contarCampeonatosPorPiloto(participacionesDeLaCategoria);

        Map<Long, List<Participacion>> participacionesPorPiloto = participacionesDeLaCategoria.stream()
                .collect(Collectors.groupingBy(Participacion::pilotoId));

        for (Map.Entry<Long, List<Participacion>> entrada : participacionesPorPiloto.entrySet()) {
            Long pilotoId = entrada.getKey();
            List<Participacion> participacionesDelPiloto = entrada.getValue();

            Palmares palmares = palmaresRepo.findByPilotoIdAndCategoriaId(pilotoId, categoria.getId())
                    .orElseGet(() -> nuevoPalmares(pilotoId, categoria));

            palmares.setCarreras((int) participacionesDelPiloto.stream().filter(Participacion::disputo).count());
            palmares.setVictorias((int) participacionesDelPiloto.stream().filter(Participacion::gano).count());
            palmares.setPodios((int) participacionesDelPiloto.stream().filter(Participacion::podio).count());
            palmares.setPoles((int) participacionesDelPiloto.stream().filter(Participacion::pole).count());
            palmares.setAbandonos((int) participacionesDelPiloto.stream().filter(Participacion::abandono).count());
            palmares.setPuntosTotales(participacionesDelPiloto.stream()
                    .map(Participacion::puntos).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            palmares.setCampeonatos(campeonatosPorPiloto.getOrDefault(pilotoId, 0));
            palmaresRepo.save(palmares);
        }
        return participacionesPorPiloto.size();
    }

    @Override
    public List<FilaMarca> rankingMarcas(String siglas, int anio) {
        Temporada temporada = buscarTemporada(siglas, anio);
        return AnalisisEquiposYMarcas.rankingMarcas(consolidarParticipacionesDe(temporada));
    }

    @Override
    public TendenciaGrupo tendenciaEquipo(String siglas, int anio, Long equipoId) {
        verificarQueExistaElEquipo(equipoId);
        Temporada temporada = buscarTemporada(siglas, anio);
        TendenciaGrupo tendencia = AnalisisEquiposYMarcas.tendencia(consolidarParticipacionesDe(temporada),
                AnalisisEquiposYMarcas.TIPO_EQUIPO, String.valueOf(equipoId));
        if (tendencia.fechas().isEmpty()) {
            throw new ResourceNotFoundException("El equipo no tiene resultados en " + siglas.toUpperCase() + " " + anio);
        }
        return tendencia;
    }

    @Override
    public TendenciaGrupo tendenciaMarca(String siglas, int anio, String marca) {
        if (marca == null || marca.isBlank()) {
            throw new RuleBusinessException("Indique la marca (ej: Toyota)");
        }
        Temporada temporada = buscarTemporada(siglas, anio);
        TendenciaGrupo tendencia = AnalisisEquiposYMarcas.tendencia(consolidarParticipacionesDe(temporada),
                AnalisisEquiposYMarcas.TIPO_MARCA, marca);
        if (tendencia.fechas().isEmpty()) {
            throw new ResourceNotFoundException("La marca '" + marca + "' no tiene resultados en "
                    + siglas.toUpperCase() + " " + anio);
        }
        return tendencia;
    }

    @Override
    public EvolucionGrupos evolucionGrupos(String siglas, int anio, String tipo, int top) {
        String tipoNormalizado = tipo == null ? AnalisisEquiposYMarcas.TIPO_EQUIPO : tipo.trim().toUpperCase();
        if (!AnalisisEquiposYMarcas.TIPO_EQUIPO.equals(tipoNormalizado)
                && !AnalisisEquiposYMarcas.TIPO_MARCA.equals(tipoNormalizado)) {
            throw new RuleBusinessException("El tipo debe ser EQUIPO o MARCA");
        }
        int topAcotado = Math.max(1, Math.min(top, TOP_MAXIMO));
        Temporada temporada = buscarTemporada(siglas, anio);
        return AnalisisEquiposYMarcas.evolucion(consolidarParticipacionesDe(temporada), tipoNormalizado, topAcotado);
    }

    @Override
    public Historico historicoPiloto(String siglas, Long pilotoId) {
        verificarQueExistaElPiloto(pilotoId);
        Categoria categoria = buscarCategoria(siglas);
        Historico historico = AnalisisHistorico.historicoPiloto(
                consolidarParticipacionesDeCategoria(categoria), pilotoId, categoria.getSiglas());
        if (historico.temporadas().isEmpty()) {
            throw new ResourceNotFoundException("El piloto no tiene resultados en " + categoria.getSiglas());
        }
        return historico;
    }

    @Override
    public Historico historicoEquipo(String siglas, Long equipoId) {
        verificarQueExistaElEquipo(equipoId);
        Categoria categoria = buscarCategoria(siglas);
        Historico historico = AnalisisHistorico.historicoEquipo(
                consolidarParticipacionesDeCategoria(categoria), equipoId, categoria.getSiglas());
        if (historico.temporadas().isEmpty()) {
            throw new ResourceNotFoundException("El equipo no tiene resultados en " + categoria.getSiglas());
        }
        return historico;
    }

    @Override
    public ComparativaHistorica comparativaHistorica(String siglas, Long pilotoA, Long pilotoB) {
        if (Objects.equals(pilotoA, pilotoB)) {
            throw new RuleBusinessException("Seleccione dos pilotos distintos para comparar");
        }
        verificarQueExistaElPiloto(pilotoA);
        verificarQueExistaElPiloto(pilotoB);
        Categoria categoria = buscarCategoria(siglas);
        return AnalisisHistorico.compararPilotos(consolidarParticipacionesDeCategoria(categoria),
                pilotoA, pilotoB, categoria.getSiglas());
    }



    private List<Participacion> consolidarParticipacionesDe(Temporada t) {
        List<FilaResultado> filas = new ArrayList<>(resultadoSesionRepo.filasDeTemporada(t.getId()));
        filas.addAll(resultadoEtapaRepo.filasDeTemporada(t.getId()));
        // En TC la marca es la de la participación de cada piloto, no la del equipo
        List<FilaResultado> conMarcas = MarcasPorParticipacion.aplicar(filas,
                participacionRepo.marcasDeCategoria(t.getCategoria().getId(), t.getAnio()));
        return MotorEstadistico.consolidar(conMarcas);
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


    /** Cuántos campeonatos ganó cada piloto: se mira solo las temporadas ya terminadas. */
    private Map<Long, Integer> contarCampeonatosPorPiloto(List<Participacion> participacionesDeLaCategoria) {
        Map<Long, Integer> campeonatosPorPiloto = new HashMap<>();
        Map<Long, List<Participacion>> participacionesPorTemporada = participacionesDeLaCategoria.stream()
                .collect(Collectors.groupingBy(Participacion::temporadaId));

        participacionesPorTemporada.forEach((temporadaId, participacionesDeLaTemporada) -> {
            if (!temporadaTerminada(temporadaId)) {
                return;
            }
            Long campeonId = campeonOficial(temporadaId);
            if (campeonId == null) {
                List<FilaCampeonatoPiloto> tabla = MotorEstadistico.campeonatoPilotos(participacionesDeLaTemporada);
                campeonId = tabla.isEmpty() ? null : tabla.get(0).pilotoId();
            }
            if (campeonId != null) {
                campeonatosPorPiloto.merge(campeonId, 1, Integer::sum);
            }
        });
        return campeonatosPorPiloto;
    }

    private Palmares nuevoPalmares(Long pilotoId, Categoria categoria) {
        Palmares nuevo = new Palmares();
        nuevo.setPiloto(pilotoRepo.getReferenceById(pilotoId));
        nuevo.setCategoria(categoria);
        return nuevo;
    }

    /** Una temporada terminó cuando tiene eventos y todos están finalizados o cancelados. */
    private boolean temporadaTerminada(Long temporadaId) {
        List<Evento> eventos = eventoRepo.listarPorTemporada(temporadaId);
        return !eventos.isEmpty() && eventos.stream().allMatch(evento -> {
            String estado = aEstadoEfectivo(evento.getEstado(), evento.getFechaInicio(), evento.getFechaFin());
            return EstadoEvento.FINALIZADO.equals(estado) || EstadoEvento.CANCELADO.equals(estado);
        });
    }

    private Categoria buscarCategoria(String siglas) {
        return categoriaRepo.findBySiglasIgnoreCase(siglas)
                .filter(categoria -> !categoria.isEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la categoría '" + siglas + "'"));
    }

    /** Todas las temporadas de una categoría (circuito + rally), consolidadas por piloto y evento. */
    private List<Participacion> consolidarParticipacionesDeCategoria(Categoria categoria) {
        List<FilaResultado> filas = new ArrayList<>(resultadoSesionRepo.filasDeCategoria(categoria.getId()));
        filas.addAll(resultadoEtapaRepo.filasDeCategoria(categoria.getId()));
        List<FilaResultado> conMarcas = MarcasPorParticipacion.aplicar(filas,
                participacionRepo.marcasDeCategoria(categoria.getId(), null));
        return MotorEstadistico.consolidar(conMarcas);
    }

    private  String aEstadoEfectivo(String estadoGuardado, Instant inicio, Instant fin) {
        if (EstadoEvento.CANCELADO.equals(estadoGuardado) || EstadoEvento.FINALIZADO.equals(estadoGuardado)) {
            return estadoGuardado;
        }
        Instant ahora = Instant.now();
        if (inicio == null || ahora.isBefore(inicio)) {
            return EstadoEvento.PROGRAMADO;
        }
        if (fin != null && ahora.isAfter(fin)) {
            return EstadoEvento.FINALIZADO;
        }
        return EstadoEvento.EN_CURSO;
    }

    private Long campeonOficial(Long temporadaId) {
        return temporadaRepo.findById(temporadaId)
                .map(Temporada::getCampeon)
                .map(campeon -> campeon.getId())
                .orElse(null);
    }

    private void verificarQueExistaElEquipo(Long equipoId) {
        if (equipoId == null || !equipoRepo.existsById(equipoId)) {
            throw new ResourceNotFoundException("el equipo", equipoId);
        }
    }
}
