package com.tfi.motorsport.backend.services;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaResumenDto;
import com.tfi.motorsport.backend.dtos.etapa.EtapaDto;
import com.tfi.motorsport.backend.dtos.evento.EventoDto;
import com.tfi.motorsport.backend.dtos.sesion.SesionDto;
import com.tfi.motorsport.backend.model.*;
import com.tfi.motorsport.backend.repositories.EtapaJPARepository;
import com.tfi.motorsport.backend.repositories.SesionJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.tfi.motorsport.backend.model.Catalogo.*;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.tfi.motorsport.backend.model.Catalogo.EstadoEvento.*;
import static com.tfi.motorsport.backend.model.Catalogo.ETAPA_GENERAL;

/**
 * Arma los DTOs de eventos (con sesiones, etapas y cantidad de resultados) usando pocas
 * consultas: todo se carga en bloque para la lista completa de eventos (evita el problema N+1).
 * Lo usan el servicio de eventos, el calendario y el perfil de categoría.
 */
@Component
@RequiredArgsConstructor
public class EnsambladorEventos {

    private final SesionJPARepository sesionRepo;
    private final EtapaJPARepository etapaRepo;

    public List<EventoDto> armar(List<Evento> eventos, ZoneId zona) {
        if (eventos.isEmpty()) {
            return List.of();
        }
        List<Long> ids = eventos.stream().map(Evento::getId).toList();
        Map<Long, List<Sesion>> sesiones = sesionRepo.listarPorEventos(ids).stream()
                .collect(Collectors.groupingBy(s -> s.getEvento().getId()));
        Map<Long, List<Etapa>> etapas = etapaRepo.listarPorEventos(ids).stream()
                .collect(Collectors.groupingBy(e -> e.getEvento().getId()));

        List<Long> idsSesiones = sesiones.values().stream().flatMap(List::stream).map(Sesion::getId).toList();
        List<Long> idsEtapas = etapas.values().stream().flatMap(List::stream).map(Etapa::getId).toList();
        Map<Long, Long> conteoSesiones = idsSesiones.isEmpty() ? Map.of() : conteo(sesionRepo.contarResultados(idsSesiones));
        Map<Long, Long> conteoEtapas = idsEtapas.isEmpty() ? Map.of() : conteo(etapaRepo.contarResultados(idsEtapas));

        return eventos.stream().map(e -> evento(e, zona,
                sesiones.getOrDefault(e.getId(), List.of()), etapas.getOrDefault(e.getId(), List.of()),
                conteoSesiones, conteoEtapas)).toList();
    }

    public EventoDto armar(Evento evento, ZoneId zona) {
        return armar(List.of(evento), zona).get(0);
    }

    private static Map<Long, Long> conteo(List<Object[]> filas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] f : filas) {
            mapa.put((Long) f[0], (Long) f[1]);
        }
        return mapa;
    }

    public EventoDto evento(Evento e, ZoneId zona, List<Sesion> sesiones, List<Etapa> etapas,
                                           Map<Long, Long> resultadosPorSesion, Map<Long, Long> resultadosPorEtapa) {
        Temporada t = e.getTemporada();
        return new EventoDto(e.getId(), categoria(t.getCategoria()), t.getId(), t.getAnio(), e.getNumeroFecha(),
                e.getNombre(), e.getNombreCorto(), e.getPais(), e.getCiudad(), e.getCircuito(), e.getZonaHoraria(),
                ZonaHoraria.horario(e.getFechaInicio(), zona), ZonaHoraria.horario(e.getFechaFin(), zona),
                ZonaHoraria.horaSede(e.getFechaInicio(), e.getZonaHoraria()),
                ZonaHoraria.diferenciaMinutos(e.getFechaInicio(), e.getZonaHoraria(), zona),
                ZonaHoraria.textoDiferencia(ZonaHoraria.diferenciaMinutos(e.getFechaInicio(), e.getZonaHoraria(), zona)),
                estadoEfectivo(e.getEstado(), e.getFechaInicio(), e.getFechaFin()),
                sesiones.stream().map(s -> sesion(s, zona, resultadosPorSesion.getOrDefault(s.getId(), 0L))).toList(),
                etapas.stream().map(et -> etapa(et, zona, resultadosPorEtapa.getOrDefault(et.getId(), 0L))).toList());
    }

    public CategoriaResumenDto categoria(Categoria c) {
        return new CategoriaResumenDto(c.getId(), c.getSiglas(), c.getNombre());
    }

    public SesionDto sesion(Sesion s, ZoneId zona, long cantidadResultados) {
        String estado = cantidadResultados > 0 ? Catalogo.EstadoEvento.FINALIZADO
                : estadoEfectivo(s.getEstado(), s.getFechaInicio(), s.getFechaFin());
        return new SesionDto(s.getId(), s.getNombre(), s.getTipo(), s.getNumero(),
                ZonaHoraria.horario(s.getFechaInicio(), zona), ZonaHoraria.horario(s.getFechaFin(), zona),
                estado, cantidadResultados);
    }

    public EtapaDto etapa(Etapa et, ZoneId zona, long cantidadResultados) {
        return new EtapaDto(et.getId(), et.getNumero(), et.getNombre(), et.getDia(), et.getDistancia(),
                ZonaHoraria.horario(et.getFechaInicio(), zona), et.getEstado(), cantidadResultados,
                ETAPA_GENERAL.equals(et.getIdExterno()));
    }

    /**
     * El estado guardado puede quedar desactualizado entre sincronizaciones
     * (ej: "PROGRAMADO" para una carrera que ya empezó). Se corrige según la hora actual.
     */
    public String estadoEfectivo(String guardado, Instant inicio, Instant fin) {
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
}
