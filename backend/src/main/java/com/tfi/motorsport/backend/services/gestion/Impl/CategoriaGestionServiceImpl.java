package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.categoria.CategoriaCreateDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaDto;
import com.tfi.motorsport.backend.dtos.categoria.CategoriaUpdateDto;
import com.tfi.motorsport.backend.dtos.gestionDatos.AuditoriaDto;
import com.tfi.motorsport.backend.dtos.gestionDatos.CategoriaDtos;
import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.services.ICategoriaService;
import com.tfi.motorsport.backend.services.gestion.ICategoriaGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaGestionServiceImpl implements ICategoriaGestionService {

    private final CategoriaJPARepository categoriaRepo;
    private final ICategoriaService categoriaService;
    private final SoporteGestion soporte;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaDtos.Registro> listar() {
        return categoriaRepo.findAllByEliminadoFalseOrderByIdAsc().stream().map(this::registro).toList();
    }

    /**
     * Las validaciones son las de ICategoriaService. Corre en la misma transacción, así que la
     * categoría y su auditoría se guardan juntas (si algo falla, no queda ninguna de las dos).
     */
    @Override
    public CategoriaDto crear(CategoriaCreateDto dto, Long usuarioId) {
        CategoriaDto creada = categoriaService.crearCategoria(dto);
        soporte.alta(soporte.activa(categoriaRepo, creada.id(), "la categoría"), usuarioId);
        return creada;
    }

    @Override
    public CategoriaDto modificar(Long id, CategoriaUpdateDto dto, Long usuarioId) {
        CategoriaDto modificada = categoriaService.modificarCategoria(id, dto);
        soporte.cambio(soporte.activa(categoriaRepo, id, "la categoría"), usuarioId);
        return modificada;
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        Categoria c = soporte.activa(categoriaRepo, id, "la categoría");
        c.setActivo(false);
        soporte.baja(c, usuarioId);
    }

    private CategoriaDtos.Registro registro(Categoria c) {
        return new CategoriaDtos.Registro(c.getId(), c.getNombre(), c.getSiglas(), c.getDescripcion(),
                c.getPaisOrigen(), c.getLogoUrl(), c.getActivo(), AuditoriaDto.de(c));
    }
}
