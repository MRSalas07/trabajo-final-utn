package com.tfi.motorsport.backend.services;


import com.tfi.motorsport.backend.dtos.categoria.CategoriaDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaCreateDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaUpdateDto;

import java.util.List;

public interface ICategoriaService {

    List<CategoriaDto> obtenerCategorias();

    CategoriaDto obtenerCategoria(Long id);

    List<CategoriaDto> obtenerCategoriasPorFiltro(Boolean activo);

    CategoriaDto crearCategoria(CategoriaCreateDto dto);

    CategoriaDto modificarCategoria(Long id, CategoriaUpdateDto dto);

}
