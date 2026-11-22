package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.TripulacionDtos;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Tripulacion;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoEtapaJPARepository;
import com.tfi.motorsport.backend.repositories.TripulacionJPARepository;
import com.tfi.motorsport.backend.services.gestion.IParticipacionGestionService;
import com.tfi.motorsport.backend.services.gestion.ITripulacionGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.nombre;

@Service
@RequiredArgsConstructor
@Transactional
public class TripulacionGestionServiceImpl implements ITripulacionGestionService {

    private final TripulacionJPARepository tripulacionRepo;
    private final CategoriaJPARepository categoriaRepo;
    private final PilotoJPARepository pilotoRepo;
    private final EquipoJPARepository equipoRepo;
    private final ResultadoEtapaJPARepository resultadoEtapaRepo;
    private final IParticipacionGestionService participaciones;
    private final SoporteGestion soporte;

    @Override
    @Transactional(readOnly = true)
    public List<TripulacionDtos.Registro> listar(Long categoriaId, Integer anio) {
        soporte.activa(categoriaRepo, categoriaId, "la categoría");
        return tripulacionRepo.listar(categoriaId, anio).stream().map(this::registro).toList();
    }

    @Override
    public TripulacionDtos.Registro crear(TripulacionDtos.Request r, Long usuarioId) {
        validar(r);
        Tripulacion t = tripulacionRepo.findByCategoriaIdAndTemporadaAndPilotoIdAndCopilotoIdAndEquipoId(
                r.categoriaId(), r.temporada(), r.pilotoId(), r.copilotoId(), r.equipoId()).orElse(null);
        if (t != null && !t.isEliminado()) {
            throw new RuleBusinessException("Esa tripulación ya existe para la temporada " + r.temporada(), HttpStatus.CONFLICT);
        }
        if (t == null) {
            t = new Tripulacion();
        }
        t.setEliminado(false);
        copiar(t, r);
        soporte.alta(t, usuarioId);
        Tripulacion guardada = tripulacionRepo.save(t);
        participaciones.registrarSiNoExiste(r.equipoId(), r.pilotoId(), r.categoriaId(), r.temporada(), r.numero(),
                Catalogo.ROL_PILOTO, usuarioId);
        participaciones.registrarSiNoExiste(r.equipoId(), r.copilotoId(), r.categoriaId(), r.temporada(), r.numero(),
                Catalogo.ROL_COPILOTO, usuarioId);
        return registro(guardada);
    }

    @Override
    public TripulacionDtos.Registro modificar(Long id, TripulacionDtos.Request r, Long usuarioId) {
        Tripulacion t = soporte.activa(tripulacionRepo, id, "la tripulación");
        validar(r);
        copiar(t, r);
        soporte.cambio(t, usuarioId);
        return registro(t);
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        Tripulacion t = soporte.activa(tripulacionRepo, id, "la tripulación");
        long resultados = resultadoEtapaRepo.countByTripulacionIdAndEliminadoFalse(id);
        if (resultados > 0) {
            throw new RuleBusinessException("No se puede dar de baja: la tripulación tiene " + resultados
                    + " resultados de etapa activos. Elimínelos primero.", HttpStatus.CONFLICT);
        }
        soporte.baja(t, usuarioId);
    }

    private void validar(TripulacionDtos.Request r) {
        if (Objects.equals(r.pilotoId(), r.copilotoId())) {
            throw new RuleBusinessException("El piloto y el copiloto deben ser personas distintas");
        }
        soporte.activa(categoriaRepo, r.categoriaId(), "la categoría");
        soporte.activa(pilotoRepo, r.pilotoId(), "el piloto");
        soporte.activa(pilotoRepo, r.copilotoId(), "el copiloto");
        soporte.activa(equipoRepo, r.equipoId(), "el equipo");
    }

    private void copiar(Tripulacion t, TripulacionDtos.Request r) {
        t.setCategoria(categoriaRepo.getReferenceById(r.categoriaId()));
        t.setTemporada(r.temporada());
        t.setPiloto(pilotoRepo.getReferenceById(r.pilotoId()));
        t.setCopiloto(pilotoRepo.getReferenceById(r.copilotoId()));
        t.setEquipo(equipoRepo.getReferenceById(r.equipoId()));
        t.setNumero(r.numero());
    }

    private TripulacionDtos.Registro registro(Tripulacion t) {
        return new TripulacionDtos.Registro(t.getId(), t.getCategoria().getId(), t.getCategoria().getSiglas(), t.getTemporada(),
                t.getPiloto().getId(), nombre(t.getPiloto()), t.getCopiloto().getId(), nombre(t.getCopiloto()),
                t.getEquipo().getId(), t.getEquipo().getNombre(), t.getNumero());
    }
}
