package com.tfi.motorsport.backend.services.Impl;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaCreateDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaUpdateDto;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.services.ICategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
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
        Categoria categoria = categoriaJPARepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró la categoría con ID " + id));
        return CategoriaDto.fromEntity(categoria);
    }

    @Override
    public List<CategoriaDto> obtenerCategoriasPorFiltro(Boolean activo) {
        List<Categoria> categorias = categoriaJPARepository.findAllByActivo(activo);
        return categorias.stream()
                .map(CategoriaDto::fromEntity)
                .toList();
    }

    @Override
    public CategoriaDto crearCategoria(CategoriaCreateDto dto) {
        Categoria categoria = new Categoria();
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
        Categoria categoria = categoriaJPARepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró la categoría con ID " + id));
        categoria.setNombre(dto.nombre());
        categoria.setSiglas(dto.siglas());
        categoria.setDescripcion(dto.descripcion());
        categoria.setPaisOrigen(dto.paisOrigen());
        categoria.setLogoUrl(dto.logoUrl());
        return CategoriaDto.fromEntity(categoriaJPARepository.save(categoria));
    }
}
