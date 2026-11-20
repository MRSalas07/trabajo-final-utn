package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaCreateDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaUpdateDto;
import com.tfi.motorsport.backend.dtos.categoria.PerfilCategoriaDto;
import com.tfi.motorsport.backend.dtos.estadistica.EstadisticaDto;
import com.tfi.motorsport.backend.dtos.evento.EventoDto;
import com.tfi.motorsport.backend.dtos.temporada.TemporadaDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Catalogo.*;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Evento;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.repositories.TemporadaJPARepository;
import com.tfi.motorsport.backend.services.EnsambladorEventos;
import com.tfi.motorsport.backend.services.ICategoriaService;
import com.tfi.motorsport.backend.services.IEstadisticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaServiceImpl implements ICategoriaService {

    private final CategoriaJPARepository categoriaJPARepository;
    private final TemporadaJPARepository temporadaJPARepository;
    private final EventoJPARepository eventoJPARepository;
    private final IEstadisticaService estadisticaService;
    private final EnsambladorEventos ensamblador;

    @Override
    public List<CategoriaDto> obtenerCategorias() {
        // Solo las vigentes: findAll() también devolvía las dadas de baja (baja lógica)
        List<Categoria> categorias = categoriaJPARepository.findAllByEliminadoFalseOrderByIdAsc();
        return categorias.stream()
                .map(CategoriaDto::fromEntity)
                .toList();
    }

    @Override
    public CategoriaDto obtenerCategoria(Long id) {
        Categoria categoria = buscarCategoria(id);
        return CategoriaDto.fromEntity(categoria);
    }

    @Override
    public List<CategoriaDto> obtenerCategoriasPorFiltro(Boolean activo) {
        List<Categoria> categorias = categoriaJPARepository.findAllByActivoAndEliminadoFalseOrderByIdAsc(activo);
        return categorias.stream()
                .map(CategoriaDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public CategoriaDto crearCategoria(CategoriaCreateDto dto) {
        String siglas = dto.siglas().trim().toUpperCase();
        Categoria categoria = categoriaJPARepository.findBySiglasIgnoreCase(siglas).orElse(null);
        if (categoria != null && !categoria.isEliminado()) {
            throw new RuleBusinessException("Ya existe una categoría con siglas '" + siglas + "'", HttpStatus.CONFLICT);
        }
        if (categoria == null) {
            categoria = new Categoria();
        }
        // Si existía dada de baja (baja lógica) con esas siglas, se reactiva en lugar de duplicarla
        categoria.setEliminado(false);
        categoria.setNombre(dto.nombre().trim());
        categoria.setSiglas(siglas); // se guardan normalizadas (sin espacios y en mayúsculas)
        categoria.setDescripcion(dto.descripcion());
        categoria.setPaisOrigen(dto.paisOrigen());
        categoria.setLogoUrl(dto.logoUrl());
        categoria.setActivo(true);
        return CategoriaDto.fromEntity(categoriaJPARepository.save(categoria));
    }

    @Override
    @Transactional // la clase es readOnly: sin esto la modificación no se escribe en la base
    public CategoriaDto modificarCategoria(Long id, CategoriaUpdateDto dto) {
        Categoria categoria = buscarCategoria(id);
        String siglas = dto.siglas().trim().toUpperCase();
        categoriaJPARepository.findBySiglasIgnoreCase(siglas)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new RuleBusinessException("Las siglas '" + siglas + "' ya están en uso", HttpStatus.CONFLICT);
                });
        categoria.setNombre(dto.nombre().trim());
        categoria.setSiglas(siglas);
        categoria.setDescripcion(dto.descripcion());
        categoria.setPaisOrigen(dto.paisOrigen());
        categoria.setLogoUrl(dto.logoUrl());
        if (dto.activo() != null) {
            categoria.setActivo(dto.activo());
        }
        return CategoriaDto.fromEntity(categoriaJPARepository.save(categoria));
    }

    @Override
    public PerfilCategoriaDto obtenerPerfil(String siglas, ZoneId zonaDelUsuario) {
        Categoria categoria = buscarCategoriaActiva(siglas);
        List<TemporadaDto> temporadas = temporadaJPARepository
                .findByCategoriaIdAndEliminadoFalseOrderByAnioDesc(categoria.getId()).stream()
                .map(this::convertirTemporadaADto)
                .toList();
        TemporadaDto temporadaActual = elegirTemporadaActual(temporadas);

        EstadisticaDto.ResumenTemporada resumen = null;
        EventoDto proximoEvento = null;
        EventoDto ultimoEvento = null;
        if (temporadaActual != null) {
            resumen = estadisticaService.resumen(categoria.getSiglas(), temporadaActual.anio());

            List<Evento> eventosDeLaTemporada = eventoJPARepository.listarPorTemporada(temporadaActual.id());
            Evento proximo = buscarProximoEvento(eventosDeLaTemporada);
            Evento ultimoFinalizado = buscarUltimoEventoFinalizado(eventosDeLaTemporada);
            proximoEvento = proximo != null ? ensamblador.armar(proximo, zonaDelUsuario) : null;
            ultimoEvento = ultimoFinalizado != null ? ensamblador.armar(ultimoFinalizado, zonaDelUsuario) : null;
        }

        return new PerfilCategoriaDto(CategoriaDto.fromEntity(categoria), temporadas, temporadaActual,
                resumen, proximoEvento, ultimoEvento);
    }

    /** La temporada del año en curso si existe; si no, la más reciente cargada (la lista viene de más nueva a más vieja). */
    private TemporadaDto elegirTemporadaActual(List<TemporadaDto> temporadasDeMasNuevaAMasVieja) {
        int anioEnCurso = LocalDate.now().getYear();
        return temporadasDeMasNuevaAMasVieja.stream()
                .filter(temporada -> temporada.anio() == anioEnCurso)
                .findFirst()
                .orElse(temporadasDeMasNuevaAMasVieja.isEmpty() ? null : temporadasDeMasNuevaAMasVieja.get(0));
    }

    /** El próximo evento: el primero por fecha que todavía no terminó y no fue cancelado. */
    private Evento buscarProximoEvento(List<Evento> eventosDeLaTemporada) {
        return eventosDeLaTemporada.stream()
                .filter(evento -> !estaFinalizado(evento))
                .filter(evento -> !EstadoEvento.CANCELADO.equals(evento.getEstado()))
                .min(Comparator.comparing(Evento::getFechaInicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    /** El último evento finalizado: el más reciente por fecha. */
    private Evento buscarUltimoEventoFinalizado(List<Evento> eventosDeLaTemporada) {
        return eventosDeLaTemporada.stream()
                .filter(this::estaFinalizado)
                .max(Comparator.comparing(Evento::getFechaInicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    /** Un evento está finalizado según su estado REAL ahora (no solo el guardado en la base). */
    private boolean estaFinalizado(Evento evento) {
        String estadoReal = ensamblador.estadoEfectivo(evento.getEstado(), evento.getFechaInicio(), evento.getFechaFin());
        return EstadoEvento.FINALIZADO.equals(estadoReal);
    }

    private TemporadaDto convertirTemporadaADto(Temporada temporada) {
        return new TemporadaDto(temporada.getId(), temporada.getCategoria().getId(),
                temporada.getCategoria().getSiglas(), temporada.getAnio(), temporada.getNombre(),
                temporada.getFechaInicio(), temporada.getFechaFin());
    }

    private Categoria buscarCategoriaActiva(String siglas) {
        return categoriaJPARepository.findBySiglasIgnoreCase(siglas)
                .filter(categoria -> !categoria.isEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la categoría '" + siglas + "'"));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaJPARepository.findById(id)
                .filter(c -> !c.isEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("la categoría", id));
    }
}
