package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.calendario.ArchivoIcs;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Evento;
import com.tfi.motorsport.backend.model.Sesion;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.repositories.SesionJPARepository;
import com.tfi.motorsport.backend.repositories.TemporadaJPARepository;
import com.tfi.motorsport.backend.services.ICalendarioService;
import com.tfi.motorsport.backend.services.Impl.GeneradorIcs.EventoIcs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarioServiceImpl implements ICalendarioService {

    private static final String DOMINIO_UID = "@motorsport-analytics";

    private final EventoJPARepository eventoRepo;
    private final SesionJPARepository sesionRepo;
    private final TemporadaJPARepository temporadaRepo;

    @Override
    public ArchivoIcs icsDeEvento(Long eventoId) {
        Evento evento = eventoRepo.buscarConCategoria(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("el evento", eventoId));

        List<EventoIcs> entradas = new ArrayList<>(sesionRepo.listarPorEventos(List.of(eventoId)).stream()
                .filter(s -> s.getFechaInicio() != null)
                .map(s -> deSesion(evento, s))
                .toList());
        // WRC (o un evento sin sesiones con fecha): una sola entrada para todo el evento
        if (entradas.isEmpty()) {
            deEvento(evento).ifPresent(entradas::add);
        }
        if (entradas.isEmpty()) {
            throw new ResourceNotFoundException("El evento " + eventoId + " todavía no tiene fechas cargadas");
        }

        Temporada t = evento.getTemporada();
        String siglas = t.getCategoria().getSiglas();
        String nombre = Objects.requireNonNullElse(evento.getNombreCorto(), evento.getNombre());
        return new ArchivoIcs(
                nombreArchivo(siglas, String.valueOf(t.getAnio()), nombre),
                GeneradorIcs.generar(siglas + " " + t.getAnio() + " · " + nombre, entradas, Instant.now()));
    }

    @Override
    public ArchivoIcs icsDeTemporada(String siglas, int anio) {
        Temporada temporada = temporadaRepo.buscarPorSiglasYAnio(siglas, anio)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay datos de la temporada " + anio + " de " + siglas.toUpperCase(Locale.ROOT)));

        List<Evento> eventos = eventoRepo.listarPorTemporada(temporada.getId());
        Map<Long, List<Sesion>> sesionesPorEvento = eventos.isEmpty() ? Map.of()
                : sesionRepo.listarPorEventos(eventos.stream().map(Evento::getId).toList()).stream()
                        .collect(Collectors.groupingBy(s -> s.getEvento().getId()));

        // Por evento: la carrera principal si tiene fecha; si no (WRC o sin sesiones), el evento completo
        List<EventoIcs> entradas = eventos.stream()
                .flatMap(e -> sesionesPorEvento.getOrDefault(e.getId(), List.of()).stream()
                        .filter(s -> Catalogo.TipoSesion.CARRERA.equals(s.getTipo()) && s.getFechaInicio() != null)
                        .findFirst()
                        .map(s -> Stream.of(deSesion(e, s)))
                        .orElseGet(() -> deEvento(e).stream()))
                .toList();

        String sigla = temporada.getCategoria().getSiglas();
        return new ArchivoIcs(
                nombreArchivo(sigla, String.valueOf(anio)),
                GeneradorIcs.generar(sigla + " " + anio, entradas, Instant.now()));
    }

    // ------------------------------------------------------------------ conversiones

    private static EventoIcs deSesion(Evento e, Sesion s) {
        String siglas = e.getTemporada().getCategoria().getSiglas();
        String nombreEvento = Objects.requireNonNullElse(e.getNombreCorto(), e.getNombre());
        return new EventoIcs(
                "sesion-" + s.getId() + DOMINIO_UID,
                siglas + " · " + nombreEvento + " · " + s.getNombre(),
                s.getFechaInicio(), s.getFechaFin(), lugar(e), e.getNombre(),
                Catalogo.EstadoEvento.CANCELADO.equals(s.getEstado())
                        || Catalogo.EstadoEvento.CANCELADO.equals(e.getEstado()));
    }

    private static Optional<EventoIcs> deEvento(Evento e) {
        if (e.getFechaInicio() == null) {
            return Optional.empty();
        }
        String siglas = e.getTemporada().getCategoria().getSiglas();
        return Optional.of(new EventoIcs(
                "evento-" + e.getId() + DOMINIO_UID,
                siglas + " · " + e.getNombre(),
                e.getFechaInicio(), e.getFechaFin(), lugar(e), null,
                Catalogo.EstadoEvento.CANCELADO.equals(e.getEstado())));
    }

    /** "Albert Park, Melbourne, Australia" (omite los datos que falten). */
    private static String lugar(Evento e) {
        return Stream.of(e.getCircuito(), e.getCiudad(), e.getPais())
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    /** "f1-2025-australia.ics": minúsculas, sin acentos y solo letras, números y guiones. */
    static String nombreArchivo(String... partes) {
        String base = String.join("-", partes);
        // NFKD separa los acentos (é -> e + ´) y además convierte "ª" en "a"
        String sinAcentos = Normalizer.normalize(base, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        String limpio = sinAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return (limpio.isEmpty() ? "calendario" : limpio) + ".ics";
    }
}
