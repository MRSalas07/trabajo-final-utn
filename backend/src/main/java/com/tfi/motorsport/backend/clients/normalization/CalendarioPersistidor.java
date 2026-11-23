package com.tfi.motorsport.backend.clients.normalization;


import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EtapaNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EventoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.ResultadoEtapaNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.ResultadoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.SesionNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.TemporadaNorm;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Catalogo.EstadoEvento;
import com.tfi.motorsport.backend.model.Etapa;
import com.tfi.motorsport.backend.model.Evento;
import com.tfi.motorsport.backend.model.ResultadoEtapa;
import com.tfi.motorsport.backend.model.ResultadoSesion;
import com.tfi.motorsport.backend.model.Sesion;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.EtapaJPARepository;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoEtapaJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import com.tfi.motorsport.backend.repositories.SesionJPARepository;
import com.tfi.motorsport.backend.repositories.TemporadaJPARepository;
import com.tfi.motorsport.backend.repositories.TripulacionJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Guarda el CALENDARIO deportivo y sus tablas de resultados:
 * temporada → evento → sesiones (F1, TC) o etapas (WRC) → resultados.
 *
 * Cada registro hace UPSERT por id_externo dentro de su "padre" (si existe se actualiza,
 * si no se crea), por eso la sincronización se puede repetir sin duplicar datos.
 * Los pilotos, equipos y tripulaciones los resuelve {@link ParticipantesPersistidor}.
 *
 * persistirEvento se llama dentro de la transacción del evento que abre NormalizadorService.
 */
@Component
@RequiredArgsConstructor
class CalendarioPersistidor {

    private final ValidacionNormalizada validacion;
    private final ParticipantesPersistidor participantes;
    private final CategoriaJPARepository categoriaRepo;
    private final TemporadaJPARepository temporadaRepo;
    private final EventoJPARepository eventoRepo;
    private final SesionJPARepository sesionRepo;
    private final EtapaJPARepository etapaRepo;
    private final PilotoJPARepository pilotoRepo;
    private final EquipoJPARepository equipoRepo;
    private final TripulacionJPARepository tripulacionRepo;
    private final ResultadoSesionJPARepository resultadoSesionRepo;
    private final ResultadoEtapaJPARepository resultadoEtapaRepo;

    Temporada upsertTemporada(Long categoriaId, TemporadaNorm t) {
        Temporada temporada = temporadaRepo.findByCategoriaIdAndAnio(categoriaId, t.anio()).orElseGet(() -> {
            Temporada nueva = new Temporada();
            nueva.setCategoria(categoriaRepo.getReferenceById(categoriaId));
            nueva.setAnio(t.anio());
            return nueva;
        });
        temporada.setNombre(t.nombre() != null ? t.nombre().trim() : t.siglasCategoria() + " " + t.anio());
        t.eventos().stream().map(EventoNorm::inicio).filter(Objects::nonNull).min(Comparator.naturalOrder())
                .ifPresent(i -> temporada.setFechaInicio(i.atZone(ZoneOffset.UTC).toLocalDate()));
        t.eventos().stream().map(e -> e.fin() != null ? e.fin() : e.inicio()).filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .ifPresent(f -> temporada.setFechaFin(f.atZone(ZoneOffset.UTC).toLocalDate()));
        return temporadaRepo.save(temporada);
    }

    void persistirEvento(Long temporadaId, EventoNorm e, ContextoNormalizacion ctx) {
        Evento evento = eventoRepo.findByTemporadaIdAndIdExterno(temporadaId, e.idExterno()).orElseGet(() -> {
            Evento nuevo = new Evento();
            nuevo.setTemporada(temporadaRepo.getReferenceById(temporadaId));
            nuevo.setIdExterno(e.idExterno());
            return nuevo;
        });
        evento.setNombre(e.nombre().trim());
        evento.setNombreCorto(recortar(e.nombreCorto()));
        evento.setNumeroFecha(e.numeroFecha());
        evento.setPais(recortar(e.pais()));
        evento.setCiudad(recortar(e.ciudad()));
        evento.setCircuito(recortar(e.circuito()));
        evento.setZonaHoraria(zonaValida(e.zonaHoraria(), e.nombre(), ctx));
        evento.setFechaInicio(e.inicio());
        evento.setFechaFin(e.fin() != null && e.fin().isAfter(e.inicio()) ? e.fin() : e.inicio());
        evento.setEstado(valorODefecto(e.estado(), EstadoEvento.PROGRAMADO));
        Evento guardado = eventoRepo.save(evento);

        if (e.sesiones() != null) {
            for (SesionNorm s : e.sesiones()) {
                persistirSesion(guardado, s, ctx);
            }
        }
        if (e.etapas() != null) {
            for (EtapaNorm et : e.etapas()) {
                persistirEtapa(guardado, et, ctx);
            }
        }
    }

    void asignarCampeon(Long temporadaId, String idExternoCampeon, ContextoNormalizacion ctx) {
        if (idExternoCampeon == null || idExternoCampeon.isBlank()) {
            return;
        }
        pilotoRepo.findByIdExterno(idExternoCampeon.trim()).ifPresentOrElse(
                campeon -> temporadaRepo.findById(temporadaId).ifPresent(temporada -> temporada.setCampeon(campeon)),
                () -> ctx.avisos.add("No se encontró al campeón '" + idExternoCampeon
                        + "' entre los pilotos: el palmarés usará la tabla calculada"));
    }

    // Manejo de datos de F1

    private void persistirSesion(Evento evento, SesionNorm s, ContextoNormalizacion ctx) {
        if (!validacion.sesionValida(s, evento.getNombre(), ctx)) {
            return;
        }
        Sesion sesion = sesionRepo.findByEventoIdAndIdExterno(evento.getId(), s.idExterno()).orElseGet(() -> {
            Sesion nueva = new Sesion();
            nueva.setEvento(evento);
            nueva.setIdExterno(s.idExterno());
            return nueva;
        });
        sesion.setNombre(s.nombre().trim());
        sesion.setTipo(s.tipo());
        sesion.setNumero(s.numero());
        sesion.setFechaInicio(s.inicio());
        sesion.setFechaFin(s.fin() != null && s.inicio() != null && s.fin().isBefore(s.inicio()) ? s.inicio() : s.fin());
        sesion.setVueltasProgramadas(s.vueltasProgramadas());
        sesion.setDistancia(s.distancia());
        sesion.setEstado(valorODefecto(s.estado(), EstadoEvento.PROGRAMADO));
        Sesion guardada = sesionRepo.save(sesion);
        ctx.sesiones++;

        if (s.resultados() == null || s.resultados().isEmpty()) {
            return;
        }
        // Una sola consulta con los resultados que ya tenía la sesión (evita una consulta por resultado)
        Map<Long, ResultadoSesion> existentes = resultadoSesionRepo.findBySesionId(guardada.getId()).stream()
                .collect(Collectors.toMap(r -> r.getPiloto().getId(), Function.identity(), (a, b) -> a));
        for (ResultadoNorm r : s.resultados()) {
            if (!validacion.resultadoValido(r, evento.getNombre(), s.nombre(), ctx)) {
                continue;
            }
            Long pilotoId = participantes.upsertPiloto(r.piloto(), ctx);
            Long equipoId = participantes.upsertEquipo(r.equipo(), ctx);
            ResultadoSesion res = existentes.get(pilotoId);
            if (res == null) {
                res = new ResultadoSesion();
                res.setSesion(guardada);
                res.setPiloto(pilotoRepo.getReferenceById(pilotoId));
                existentes.put(pilotoId, res);
            }
            res.setEquipo(equipoRepo.getReferenceById(equipoId));
            res.setPosicion(r.posicion());
            res.setNumero(r.numero());
            res.setPuntos(r.puntos());
            res.setTiempo(r.tiempo());
            res.setDiferencia(r.diferencia());
            res.setVueltas(r.vueltas());
            res.setMejorVuelta(r.mejorVuelta());
            res.setEstado(valorODefecto(r.estado(), Catalogo.EstadoResultado.FINALIZADO));
            res.setMotivoAbandono(recortar(r.motivoAbandono()));
            resultadoSesionRepo.save(res);
            participantes.registrarParticipacion(equipoId, pilotoId, r.numero(), Catalogo.ROL_PILOTO, r.marca(), ctx);
            ctx.resultados++;
        }
    }

    // Manejo de datos de WRC

    private void persistirEtapa(Evento evento, EtapaNorm et, ContextoNormalizacion ctx) {
        if (!validacion.etapaValida(et, evento.getNombre(), ctx)) {
            return;
        }
        Etapa etapa = etapaRepo.findByEventoIdAndIdExterno(evento.getId(), et.idExterno()).orElseGet(() -> {
            Etapa nueva = new Etapa();
            nueva.setEvento(evento);
            nueva.setIdExterno(et.idExterno());
            return nueva;
        });
        etapa.setNumero(et.numero());
        etapa.setNombre(et.nombre().trim());
        etapa.setDia(et.dia());
        etapa.setDistancia(et.distancia());
        etapa.setFechaInicio(et.inicio());
        etapa.setFechaFin(et.fin());
        etapa.setEstado(valorODefecto(et.estado(), EstadoEvento.PROGRAMADO));
        Etapa guardada = etapaRepo.save(etapa);
        ctx.etapas++;

        if (et.resultados() == null || et.resultados().isEmpty()) {
            return;
        }
        Map<Long, ResultadoEtapa> existentes = resultadoEtapaRepo.findByEtapaId(guardada.getId()).stream()
                .collect(Collectors.toMap(r -> r.getTripulacion().getId(), Function.identity(), (a, b) -> a));
        for (ResultadoEtapaNorm r : et.resultados()) {
            if (!validacion.resultadoValido(r, evento.getNombre(), et.nombre(), ctx)) {
                continue;
            }
            Long tripulacionId = participantes.upsertTripulacion(r, ctx);
            ResultadoEtapa res = existentes.get(tripulacionId);
            if (res == null) {
                res = new ResultadoEtapa();
                res.setEtapa(guardada);
                res.setTripulacion(tripulacionRepo.getReferenceById(tripulacionId));
                existentes.put(tripulacionId, res);
            }
            res.setPosicion(r.posicion());
            res.setTiempo(r.tiempo());
            res.setDiferencia(r.diferencia());
            res.setPuntos(r.puntos());
            res.setEstado(valorODefecto(r.estado(), Catalogo.EstadoResultado.FINALIZADO));
            res.setMotivoAbandono(recortar(r.motivoAbandono()));
            resultadoEtapaRepo.save(res);
            ctx.resultados++;
        }
    }

    private String zonaValida(String zona, String evento, ContextoNormalizacion ctx) {
        if (zona == null || zona.isBlank()) {
            return "UTC";
        }
        try {
            return ZoneId.of(zona.trim()).getId();
        } catch (DateTimeException ex) {
            ctx.avisos.add("Evento '" + evento + "': zona horaria inválida '" + zona + "', se usa UTC");
            return "UTC";
        }
    }

    private static String recortar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String valorODefecto(String valor, String defecto) {
        return valor != null ? valor : defecto;
    }
}
