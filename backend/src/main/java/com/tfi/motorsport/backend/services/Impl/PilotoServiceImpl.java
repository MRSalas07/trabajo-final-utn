package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.comun.PaginaDto;
import com.tfi.motorsport.backend.dtos.perfil.ParticipacionDto;
import com.tfi.motorsport.backend.dtos.perfil.ResultadoRecienteDto;
import com.tfi.motorsport.backend.dtos.piloto.PalmaresDto;
import com.tfi.motorsport.backend.dtos.piloto.PerfilPilotoDto;
import com.tfi.motorsport.backend.dtos.piloto.PilotoDto;
import com.tfi.motorsport.backend.dtos.piloto.PilotoResumenDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.EquipoPiloto;
import com.tfi.motorsport.backend.model.Palmares;
import com.tfi.motorsport.backend.model.Piloto;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoPilotoJPARepository;
import com.tfi.motorsport.backend.repositories.PalmaresJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoEtapaJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import com.tfi.motorsport.backend.services.IPilotoService;
import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico;
import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PilotoServiceImpl implements IPilotoService {

    private static final int ULTIMOS_RESULTADOS = 10;

    private static final Sort ORDEN_LISTADO = Sort.by("apellido", "nombre", "id");

    private final CategoriaJPARepository categoriaRepo;
    private final PilotoJPARepository pilotoRepo;
    private final EquipoPilotoJPARepository equipoPilotoRepo;
    private final PalmaresJPARepository palmaresRepo;
    private final ResultadoSesionJPARepository resultadoSesionRepo;
    private final ResultadoEtapaJPARepository resultadoEtapaRepo;

    @Override
    public PaginaDto<PilotoResumenDto> buscarPilotos(String texto, String siglas, Integer anio, int pagina, int tamanio) {
        Long categoriaId = (siglas == null || siglas.isBlank()) ? null : buscarCategoria(siglas).getId();
        String patron = "%" + (texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT)) + "%";
        boolean filtrarParticipacion = categoriaId != null || anio != null;

        Page<Piloto> pilotos = pilotoRepo.buscar(patron, filtrarParticipacion, categoriaId, anio,
                PageRequest.of(pagina, tamanio, ORDEN_LISTADO));

        // Equipo actual de cada piloto de la página, en una sola consulta.
        // La consulta viene ordenada de la participación más reciente a la más antigua: se queda con la primera.
        Map<Long, EquipoPiloto> equipoActual = pilotos.isEmpty() ? Map.of()
                : equipoPilotoRepo.participacionesDePilotos(
                                pilotos.getContent().stream().map(Piloto::getId).toList(), categoriaId, anio)
                        .stream()
                        .collect(Collectors.toMap(ep -> ep.getPiloto().getId(), ep -> ep, (primera, otra) -> primera));

        return PaginaDto.de(pilotos.map(p -> resumen(p, equipoActual.get(p.getId()))));
    }

    @Override
    public PerfilPilotoDto perfil(Long pilotoId) {
        Piloto piloto = pilotoRepo.findById(pilotoId)
                .filter(p -> !p.isEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("el piloto", pilotoId));

        List<PalmaresDto> palmares = palmaresRepo.dePiloto(pilotoId).stream()
                .map(PilotoServiceImpl::palmares)
                .toList();

        List<ParticipacionDto> trayectoria = equipoPilotoRepo.trayectoriaDePiloto(pilotoId).stream()
                .map(Participaciones::de)
                .toList();

        List<FilaResultado> filas = new ArrayList<>(resultadoSesionRepo.filasDePiloto(pilotoId));
        filas.addAll(resultadoEtapaRepo.filasDePiloto(pilotoId));

        return new PerfilPilotoDto(datos(piloto), palmares, trayectoria, ultimos(filas, siglasPorTemporada(filas)));
    }



    private PilotoResumenDto resumen(Piloto p, EquipoPiloto actual) {
        Integer numero = p.getNumeroActual() != null ? p.getNumeroActual()
                : (actual != null ? actual.getNumero() : null);
        return new PilotoResumenDto(p.getId(), p.getNombre(), p.getApellido(), p.getCodigoPiloto(), numero,
                p.getNacionalidad(), p.getPais(), p.getFotoUrl(),
                actual != null ? actual.getEquipo().getId() : null,
                actual != null ? actual.getEquipo().getNombre() : null);
    }

    private PilotoDto datos(Piloto p) {
        Integer edad = p.getFechaNacimiento() == null ? null
                : Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears();
        return new PilotoDto(p.getId(), p.getNombre(), p.getApellido(), p.getCodigoPiloto(), p.getNumeroActual(),
                p.getNacionalidad(), p.getPais(), p.getFechaNacimiento(), edad, p.getFotoUrl());
    }

    private static PalmaresDto palmares(Palmares pa) {
        return new PalmaresDto(pa.getCategoria().getSiglas(), cero(pa.getCarreras()), cero(pa.getVictorias()),
                cero(pa.getPodios()), cero(pa.getPoles()), cero(pa.getCampeonatos()),
                pa.getPuntosTotales() != null ? pa.getPuntosTotales() : BigDecimal.ZERO, cero(pa.getAbandonos()));
    }

    private static int cero(Integer valor) {
        return valor != null ? valor : 0;
    }

    private Categoria buscarCategoria(String siglas) {
        return categoriaRepo.findBySiglasIgnoreCase(siglas.trim())
                .filter(c -> !c.isEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la categoría '" + siglas + "'"));
    }

    /** Últimos resultados principales (carrera o clasificación general), del más reciente al más antiguo. */
    private List<ResultadoRecienteDto> ultimos(List<FilaResultado> filas, Map<Long, String> siglas) {
        return MotorEstadistico.consolidar(filas).stream()
                .filter(Participacion::disputo)
                .sorted(Comparator.comparing(Participacion::fecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(ULTIMOS_RESULTADOS)
                .map(p -> new ResultadoRecienteDto(p.eventoId(), p.evento(), siglas.get(p.temporadaId()), p.anio(),
                        p.fecha(), p.posicionFinal(), p.estadoFinal(), p.puntos(), p.equipo(), p.piloto()))
                .toList();
    }

    /** Siglas de la categoría de cada temporada presente en las filas. */
    private Map<Long, String> siglasPorTemporada(List<FilaResultado> filas) {
        Map<Long, String> mapa = new HashMap<>();
        Set<Long> temporadas = filas.stream().map(FilaResultado::temporadaId).collect(Collectors.toSet());
        if (temporadas.isEmpty()) {
            return mapa;
        }
        for (Categoria c : categoriaRepo.findAll()) {
            c.getTemporadas().stream().filter(t -> temporadas.contains(t.getId()))
                    .forEach(t -> mapa.put(t.getId(), c.getSiglas()));
        }
        return mapa;
    }
}
