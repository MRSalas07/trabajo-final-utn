package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.model.Categoria;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.services.gestion.ICategoriaGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaGestionServiceImpl implements ICategoriaGestionService {

    private final CategoriaJPARepository categoriaRepo;
    private final SoporteGestion soporte;

    @Override
    public void eliminar(Long id, Long usuarioId) {
        Categoria c = soporte.activa(categoriaRepo, id, "la categoría");
        c.setActivo(false);
        soporte.baja(c, usuarioId);
    }
}
