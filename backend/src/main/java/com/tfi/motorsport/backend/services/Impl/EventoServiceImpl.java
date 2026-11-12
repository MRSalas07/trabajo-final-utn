package com.tfi.motorsport.backend.services.Impl;


import com.tfi.motorsport.backend.dtos.categoria.CategoriaResumenDto;
import com.tfi.motorsport.backend.dtos.etapa.EtapaDto;

import com.tfi.motorsport.backend.dtos.evento.EventoResumenDto;
import com.tfi.motorsport.backend.model.Catalogo.*;
import com.tfi.motorsport.backend.dtos.evento.EventoDto;
import com.tfi.motorsport.backend.dtos.evento.TablaEtapaDto;
import com.tfi.motorsport.backend.dtos.sesion.SesionDto;
import com.tfi.motorsport.backend.dtos.temporada.TemporadaDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.model.*;
import com.tfi.motorsport.backend.repositories.*;
import com.tfi.motorsport.backend.services.IEventoService;
import com.tfi.motorsport.backend.services.ZonaHoraria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tfi.motorsport.backend.model.Catalogo.ETAPA_GENERAL;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventoServiceImpl implements IEventoService {
    private final CategoriaJPARepository categoriaRepo;
    private final TemporadaJPARepository temporadaRepo;
    private final EventoJPARepository eventoRepo;
    private final SesionJPARepository sesionRepo;
    private final EtapaJPARepository etapaRepo;


    @Override
    public List<TemporadaDto> temporadasDeCategoria(String siglas) {
        Categoria categoria = buscarCategoria(siglas);
        return temporadaRepo.findByCategoriaIdAndEliminadoFalseOrderByAnioDesc(categoria.getId()).stream()
                .map(t -> new TemporadaDto(t.getId(), categoria.getId(), categoria.getSiglas(), t.getAnio(),
                        t.getNombre(), t.getFechaInicio(), t.getFechaFin()))
                .toList();
    }

    @Override
    public List<EventoDto> eventosDeTemporada(String siglas, int anio, ZoneId zona) {
        Temporada temporada = temporadaRepo.buscarPorSiglasYAnio(siglas, anio)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay datos de la temporada " + anio + " de " + siglas.toUpperCase()));

        List<Evento> eventos = eventoRepo.listarPorTemporada(temporada.getId());
        if (eventos.isEmpty()) {
            return List.of();
        }

        // Sesiones y etapas de TODOS los eventos en una sola consulta cada una (evita N+1)
        List<Long> idsEventos = eventos.stream().map(Evento::getId).toList();
        Map<Long, List<Sesion>> sesionesPorEvento = sesionRepo.listarPorEventos(idsEventos).stream()
                .collect(Collectors.groupingBy(s -> s.getEvento().getId()));
        Map<Long, List<Etapa>> etapasPorEvento = etapaRepo.listarPorEventos(idsEventos).stream()
                .collect(Collectors.groupingBy(e -> e.getEvento().getId()));

        // Cantidad de resultados de cada sesión y etapa
        List<Long> idsSesiones = sesionesPorEvento.values().stream().flatMap(List::stream).map(Sesion::getId).toList();
        List<Long> idsEtapas = etapasPorEvento.values().stream().flatMap(List::stream).map(Etapa::getId).toList();
        Map<Long, Long> resultadosPorSesion = idsSesiones.isEmpty() ? Map.of() : contar(sesionRepo.contarResultados(idsSesiones));
        Map<Long, Long> resultadosPorEtapa = idsEtapas.isEmpty() ? Map.of() : contar(etapaRepo.contarResultados(idsEtapas));

        return eventos.stream()
                .map(e -> eventoDto(e, zona,
                        sesionesPorEvento.getOrDefault(e.getId(), List.of()),
                        etapasPorEvento.getOrDefault(e.getId(), List.of()),
                        resultadosPorSesion, resultadosPorEtapa))
                .toList();
    }

    @Override
    public EventoDto detalle(Long eventoId, ZoneId zona) {
        Evento evento = eventoRepo.buscarConCategoria(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("el evento", eventoId));

        List<Sesion> sesiones = sesionRepo.listarPorEventos(List.of(eventoId));
        List<Etapa> etapas = etapaRepo.listarPorEventos(List.of(eventoId));

        Map<Long, Long> resultadosPorSesion = sesiones.isEmpty() ? Map.of()
                : contar(sesionRepo.contarResultados(sesiones.stream().map(Sesion::getId).toList()));
        Map<Long, Long> resultadosPorEtapa = etapas.isEmpty() ? Map.of()
                : contar(etapaRepo.contarResultados(etapas.stream().map(Etapa::getId).toList()));

        return eventoDto(evento, zona, sesiones, etapas, resultadosPorSesion, resultadosPorEtapa);
    }

    @Override
    public TablaEtapaDto resultadosDeEtapa(Long etapaId, ZoneId zona) {
        throw new UnsupportedOperationException("No implementado aún");
    }


    private EventoDto eventoDto(Evento e, ZoneId zona, List<Sesion> sesiones, List<Etapa> etapas,
                                Map<Long, Long> resultadosPorSesion, Map<Long, Long> resultadosPorEtapa) {
        Temporada t = e.getTemporada();
        Categoria c = t.getCategoria();
        Integer diferencia = diferenciaMinutos(e.getFechaInicio(), e.getZonaHoraria(), zona);
        return new EventoDto(
                e.getId(),
                new CategoriaResumenDto(c.getId(), c.getSiglas(), c.getNombre()),
                t.getId(),
                t.getAnio(),
                e.getNumeroFecha(),
                e.getNombre(),
                e.getNombreCorto(),
                e.getPais(),
                e.getCiudad(),
                e.getCircuito(),
                e.getZonaHoraria(),
                ZonaHoraria.horario(e.getFechaInicio(), zona),
                ZonaHoraria.horario(e.getFechaFin(), zona),
                ZonaHoraria.horaSede(e.getFechaInicio(), e.getZonaHoraria()),
                diferencia,
                textoDiferencia(diferencia),
                estadoEfectivo(e.getEstado(), e.getFechaInicio(), e.getFechaFin()),
                sesiones.stream().map(s -> sesionDto(s, zona, resultadosPorSesion.getOrDefault(s.getId(), 0L))).toList(),
                etapas.stream().map(et -> etapaDto(et, zona, resultadosPorEtapa.getOrDefault(et.getId(), 0L))).toList());
    }

    private SesionDto sesionDto(Sesion s, ZoneId zona, long cantidadResultados) {
        // Si ya tiene resultados, la sesión terminó aunque el estado guardado diga otra cosa
        String estado = cantidadResultados > 0 ? EstadoEvento.FINALIZADO
                : estadoEfectivo(s.getEstado(), s.getFechaInicio(), s.getFechaFin());
        return new SesionDto(s.getId(), s.getNombre(), s.getTipo(), s.getNumero(),
                ZonaHoraria.horario(s.getFechaInicio(), zona), ZonaHoraria.horario(s.getFechaFin(), zona),
                estado, cantidadResultados);
    }

    private EtapaDto etapaDto(Etapa et, ZoneId zona, long cantidadResultados) {
        return new EtapaDto(et.getId(), et.getNumero(), et.getNombre(), et.getDia(), et.getDistancia(),
                ZonaHoraria.horario(et.getFechaInicio(), zona), et.getEstado(), cantidadResultados,
                ETAPA_GENERAL.equals(et.getIdExterno()));
    }

    private EventoResumenDto eventoResumen(Evento e) {
        Temporada t = e.getTemporada();
        Categoria c = t.getCategoria();
        return new EventoResumenDto(e.getId(), e.getNombre(), t.getAnio(),
                new CategoriaResumenDto(c.getId(), c.getSiglas(), c.getNombre()));
    }

    /** El estado guardado puede quedar viejo entre sincronizaciones: se corrige según la hora actual. */
    private static String estadoEfectivo(String guardado, Instant inicio, Instant fin) {
        if (EstadoEvento.CANCELADO.equals(guardado) || EstadoEvento.FINALIZADO.equals(guardado)) {
            return guardado;
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

    // ------------------------------------------------------------------ apoyo

    /** Filas [id, cantidad] de las consultas contarResultados -> mapa id -> cantidad. */
    private static Map<Long, Long> contar(List<Object[]> filas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] f : filas) {
            mapa.put((Long) f[0], (Long) f[1]);
        }
        return mapa;
    }

    private Categoria buscarCategoria(String siglas) {
        return categoriaRepo.findBySiglasIgnoreCase(siglas)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la categoría '" + siglas + "'"));
    }

    static String nombre(Piloto p) {
        return ((p.getNombre() != null ? p.getNombre() + " " : "") + p.getApellido()).trim();
    }

    /**
     * Diferencia horaria sede - usuario en minutos (positiva = la sede va adelantada).
     * Se calcula en la fecha del evento: el horario de verano cambia la diferencia según la época del año.
     */
    private static Integer diferenciaMinutos(Instant instante, String zonaSede, ZoneId zonaUsuario) {
        if (instante == null) {
            return null;
        }
        ZoneId sede = ZonaHoraria.esValida(zonaSede) ? ZoneId.of(zonaSede) : ZoneId.of("UTC");
        int segundos = sede.getRules().getOffset(instante).getTotalSeconds()
                - zonaUsuario.getRules().getOffset(instante).getTotalSeconds();
        return segundos / 60;
    }

    /** 840 -> "La sede está 14 h adelantada respecto de tu hora"; -210 -> "... 3 h 30 min atrasada ...". */
    private static String textoDiferencia(Integer minutos) {
        if (minutos == null) {
            return null;
        }
        if (minutos == 0) {
            return "La sede tiene la misma hora que vos";
        }
        int total = Math.abs(minutos);
        int horas = total / 60;
        int resto = total % 60;
        String cantidad;
        if (horas > 0 && resto > 0) {
            cantidad = horas + " h ";
        } else if (horas > 0) {
            cantidad = horas + " h";
        } else {
            cantidad = resto + " min";
        }
        return "La sede está " + cantidad + (minutos > 0 ? " adelantada" : " atrasada") + " respecto de tu hora";
    }
}
