package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.PalmaresDtos;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.PalmaresJPARepository;
import com.tfi.motorsport.backend.services.gestion.IPalmaresGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.nombre;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PalmaresGestionServiceImpl implements IPalmaresGestionService {

    private final PalmaresJPARepository palmaresRepo;
    private final CategoriaJPARepository categoriaRepo;
    private final SoporteGestion soporte;

    @Override
    public List<PalmaresDtos.Registro> listar(Long categoriaId) {
        soporte.activa(categoriaRepo, categoriaId, "la categoría");
        return palmaresRepo.deCategoria(categoriaId).stream()
                .map(p -> new PalmaresDtos.Registro(p.getId(), p.getPiloto().getId(), nombre(p.getPiloto()),
                        p.getCategoria().getSiglas(), p.getCarreras(), p.getVictorias(), p.getPodios(), p.getPoles(),
                        p.getCampeonatos(), p.getPuntosTotales(), p.getAbandonos()))
                .toList();
    }
}
