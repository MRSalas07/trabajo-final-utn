package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.AuditoriaDto;
import com.tfi.motorsport.backend.dtos.gestionDatos.TemporadaDtos;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.TemporadaJPARepository;
import com.tfi.motorsport.backend.services.gestion.ITemporadaGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.texto;
import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.validarFechas;

@Service
@RequiredArgsConstructor
@Transactional
public class TemporadaGestionServiceImpl implements ITemporadaGestionService {

    private final TemporadaJPARepository temporadaRepo;
    private final CategoriaJPARepository categoriaRepo;
    private final SoporteGestion soporte;

    @Override
    @Transactional(readOnly = true)
    public List<TemporadaDtos.Registro> listar(Long categoriaId) {
        soporte.activa(categoriaRepo, categoriaId, "la categoría");
        return temporadaRepo.findByCategoriaIdAndEliminadoFalseOrderByAnioDesc(categoriaId).stream().map(this::registro).toList();
    }

    @Override
    public TemporadaDtos.Registro crear(TemporadaDtos.Request r, Long usuarioId) {
        Categoria c = soporte.activa(categoriaRepo, r.categoriaId(), "la categoría");
        validarFechas(r.fechaInicio(), r.fechaFin());
        // El índice único (categoría, año) incluye las bajas: si existía dada de baja, se reactiva
        Temporada t = temporadaRepo.findByCategoriaIdAndAnio(c.getId(), r.anio()).orElse(null);
        if (t != null && !t.isEliminado()) {
            throw new RuleBusinessException("Ya existe la temporada " + r.anio() + " de " + c.getSiglas(), HttpStatus.CONFLICT);
        }
        if (t == null) {
            t = new Temporada();
            t.setCategoria(c);
            t.setAnio(r.anio());
        }
        t.setEliminado(false);
        copiar(t, r);
        soporte.alta(t, usuarioId);
        return registro(temporadaRepo.save(t));
    }

    @Override
    public TemporadaDtos.Registro modificar(Long id, TemporadaDtos.Request r, Long usuarioId) {
        Temporada t = soporte.activa(temporadaRepo, id, "la temporada");
        validarFechas(r.fechaInicio(), r.fechaFin());
        if (!t.getAnio().equals(r.anio()) || !t.getCategoria().getId().equals(r.categoriaId())) {
            Categoria c = soporte.activa(categoriaRepo, r.categoriaId(), "la categoría");
            temporadaRepo.findByCategoriaIdAndAnio(c.getId(), r.anio()).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
                throw new RuleBusinessException("Ya existe la temporada " + r.anio() + " de " + c.getSiglas(), HttpStatus.CONFLICT);
            });
            t.setCategoria(c);
            t.setAnio(r.anio());
        }
        copiar(t, r);
        soporte.cambio(t, usuarioId);
        return registro(t);
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        soporte.baja(soporte.activa(temporadaRepo, id, "la temporada"), usuarioId);
    }

    private void copiar(Temporada t, TemporadaDtos.Request r) {
        t.setNombre(texto(r.nombre()) != null ? r.nombre().trim() : t.getCategoria().getSiglas() + " " + r.anio());
        t.setFechaInicio(r.fechaInicio());
        t.setFechaFin(r.fechaFin());
    }

    private TemporadaDtos.Registro registro(Temporada t) {
        return new TemporadaDtos.Registro(t.getId(), t.getCategoria().getId(), t.getCategoria().getSiglas(), t.getAnio(),
                t.getNombre(), t.getFechaInicio(), t.getFechaFin(),
                AuditoriaDto.de(t));
    }
}
