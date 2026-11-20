package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.equipo.EquipoResumenDto;
import com.tfi.motorsport.backend.dtos.equipo.EstadisticasEquipoDto;
import com.tfi.motorsport.backend.dtos.equipo.PerfilEquipoDto;
import com.tfi.motorsport.backend.dtos.perfil.ParticipacionDto;
import com.tfi.motorsport.backend.dtos.perfil.ResultadoRecienteDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Equipo;
import com.tfi.motorsport.backend.model.EquipoPiloto;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoPilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoEtapaJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import com.tfi.motorsport.backend.repositories.unificacion.FilaResultado;
import com.tfi.motorsport.backend.services.IEquipoService;
import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico;
import com.tfi.motorsport.backend.services.estadisticas.MotorEstadistico.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipoServiceImpl implements IEquipoService {

    private static final int ULTIMOS_RESULTADOS = 10;

    private final CategoriaJPARepository categoriaRepo;
    private final EquipoJPARepository equipoRepo;
    private final EquipoPilotoJPARepository equipoPilotoRepo;
    private final ResultadoSesionJPARepository resultadoSesionRepo;
    private final ResultadoEtapaJPARepository resultadoEtapaRepo;

    @Override
    public List<EquipoResumenDto> equiposDeCategoria(String siglas, Integer anio) {
        Categoria categoria = buscarCategoria(siglas);
        return equipoRepo.deCategoria(categoria.getId(), anio).stream()
                .map(EquipoServiceImpl::resumen)
                .toList();
    }

    @Override
    public PerfilEquipoDto perfil(Long equipoId) {
        Equipo equipo = equipoRepo.findById(equipoId).filter(x -> !x.isEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("el equipo", equipoId));
        List<EquipoPiloto> plantel = equipoPilotoRepo.plantelDeEquipo(equipoId);
        Set<String> categorias = plantel.stream().map(ep -> ep.getCategoria().getSiglas())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ParticipacionDto> plantelDto = plantel.stream()
                .map(ep -> new ParticipacionDto(ep.getTemporada(), ep.getCategoria().getSiglas(), equipoId, equipo.getNombre(),
                        ep.getPiloto().getId(), EventoServiceImpl.nombre(ep.getPiloto()), ep.getNumero(), ep.getRol()))
                .toList();

        List<FilaResultado> filas = new ArrayList<>(resultadoSesionRepo.filasDeEquipo(equipoId));
        filas.addAll(resultadoEtapaRepo.filasDeEquipo(equipoId));
        List<MotorEstadistico.Participacion> participaciones = MotorEstadistico.consolidar(filas).stream().filter(Participacion::disputo).toList();
        EstadisticasEquipoDto estadisticasEquipoDto = new EstadisticasEquipoDto(
                (int) participaciones.stream().map(MotorEstadistico.Participacion::eventoId).distinct().count(),
                (int) participaciones.stream().filter(Participacion::gano).count(),
                (int) participaciones.stream().filter(Participacion::podio).count(),
                participaciones.stream().map(Participacion::puntos).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));


        return new PerfilEquipoDto(resumen(equipo), equipo.getSede(), equipo.getFechaFundacion(), List.copyOf(categorias),
                plantelDto,estadisticasEquipoDto, ResultadosRecientes.de(filas, false));
    }

    /**
     * Estadísticas calculadas con los resultados cargados (no con un palmarés guardado).
     * Victorias y podios cuentan solo resultados finales de evento; los puntos suman también los sprints.
     */
    static EstadisticasEquipoDto estadisticas(List<FilaResultado> filas) {
        List<FilaResultado> finales = filas.stream().filter(ResultadosRecientes::esResultadoDeEvento).toList();

        int eventos = (int) finales.stream().map(FilaResultado::eventoId).distinct().count();
        int victorias = (int) finales.stream().filter(f -> Objects.equals(f.posicion(), 1)).count();
        int podios = (int) finales.stream()
                .filter(f -> f.posicion() != null && f.posicion() >= 1 && f.posicion() <= 3)
                .count();
        BigDecimal puntos = filas.stream()
                .filter(f -> ResultadosRecientes.esResultadoDeEvento(f) || Catalogo.TipoSesion.SPRINT.equals(f.tipo()))
                .map(FilaResultado::puntos)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new EstadisticasEquipoDto(eventos, victorias, podios, puntos);
    }

    private static EquipoResumenDto resumen(Equipo e) {
        return new EquipoResumenDto(e.getId(), e.getNombre(), e.getNombreCorto(), e.getNacionalidad(), e.getLogoUrl());
    }

    private Categoria buscarCategoria(String siglas) {
        return categoriaRepo.findBySiglasIgnoreCase(siglas.trim())
                .filter(c -> !c.isEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la categoría '" + siglas + "'"));
    }

    /** Últimos resultados principales (carrera o clasificación general), del más reciente al más antiguo. */
    private List<ResultadoRecienteDto> ultimos(List<FilaResultado> filas) {
        Map<Long, String> siglasPorTemporada = siglasPorTemporada(filas);
        return MotorEstadistico.consolidar(filas).stream()
                .filter(MotorEstadistico.Participacion::disputo)
                .sorted(Comparator.comparing(MotorEstadistico.Participacion::fecha, Comparator.nullsLast(Comparator.<Instant>reverseOrder()))
                        .thenComparing(MotorEstadistico.Participacion::posicionFinal, Comparator.nullsLast(Comparator.<Integer>naturalOrder())))
                .limit(ULTIMOS_RESULTADOS)
                .map(participacion -> new ResultadoRecienteDto(
                        participacion.eventoId(), participacion.evento(),
                        siglasPorTemporada.get(participacion.temporadaId()), participacion.anio(),
                        participacion.fecha(), participacion.posicionFinal(), participacion.estadoFinal(),
                        participacion.puntos(), participacion.equipo(), participacion.piloto()))
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
