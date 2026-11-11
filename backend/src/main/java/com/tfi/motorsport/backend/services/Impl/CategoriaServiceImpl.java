package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaCreateDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaUpdateDto;
import com.tfi.motorsport.backend.handlers.exceptions.ResourceNotFoundException;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.services.ICategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaServiceImpl implements ICategoriaService {

    private final CategoriaJPARepository categoriaJPARepository;


    @Override
    public List<CategoriaDto> obtenerCategorias() {
        List<Categoria> categorias = categoriaJPARepository.findAll();
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
        categoria.setNombre(dto.nombre());
        categoria.setSiglas(dto.siglas());
        categoria.setDescripcion(dto.descripcion());
        categoria.setPaisOrigen(dto.paisOrigen());
        categoria.setLogoUrl(dto.logoUrl());
        categoria.setActivo(true);
        return CategoriaDto.fromEntity(categoriaJPARepository.save(categoria));
    }

    @Override
    public CategoriaDto modificarCategoria(Long id, CategoriaUpdateDto dto) {
        Categoria categoria = buscarCategoria(id);
        String siglas = dto.siglas().trim().toUpperCase();
        categoriaJPARepository.findBySiglasIgnoreCase(siglas)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new RuleBusinessException("Las siglas '" + siglas + "' ya están en uso", HttpStatus.CONFLICT);
                });
        categoria.setNombre(dto.nombre());
        categoria.setSiglas(dto.siglas());
        categoria.setDescripcion(dto.descripcion());
        categoria.setPaisOrigen(dto.paisOrigen());
        categoria.setLogoUrl(dto.logoUrl());
        if (dto.activo() != null) {
            categoria.setActivo(dto.activo());
        }
        return CategoriaDto.fromEntity(categoriaJPARepository.save(categoria));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaJPARepository.findById(id)
                .filter(c -> !c.isEliminado())
                .orElseThrow(() -> new ResourceNotFoundException("la categoría", id));
    }
}
