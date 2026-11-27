package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.AuditoriaDto;
import com.tfi.motorsport.backend.dtos.gestionDatos.ParticipacionDtos;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.EquipoPiloto;
import com.tfi.motorsport.backend.repositories.CategoriaJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.EquipoPilotoJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.services.gestion.IParticipacionGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.nombre;
import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.validarFechas;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipacionGestionServiceImpl implements IParticipacionGestionService {

    private final EquipoPilotoJPARepository participacionRepo;
    private final CategoriaJPARepository categoriaRepo;
    private final PilotoJPARepository pilotoRepo;
    private final EquipoJPARepository equipoRepo;
    private final SoporteGestion soporte;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipacionDtos.Registro> listar(Long categoriaId, Integer anio) {
        soporte.activa(categoriaRepo, categoriaId, "la categoría");
        return participacionRepo.listar(categoriaId, anio).stream().map(this::registro).toList();
    }

    @Override
    public ParticipacionDtos.Registro crear(ParticipacionDtos.Request r, Long usuarioId) {
        validarReferencias(r);
        String rol = r.rol() != null ? r.rol() : Catalogo.ROL_PILOTO;
        EquipoPiloto ep = participacionRepo.findByEquipoIdAndPilotoIdAndCategoriaIdAndTemporadaAndRol(
                r.equipoId(), r.pilotoId(), r.categoriaId(), r.temporada(), rol).orElse(null);
        if (ep != null && !ep.isEliminado()) {
            throw new RuleBusinessException("El piloto ya figura en ese equipo para la temporada " + r.temporada(), HttpStatus.CONFLICT);
        }
        if (ep == null) {
            ep = new EquipoPiloto();
        }
        ep.setEliminado(false);
        copiar(ep, r, rol);
        soporte.alta(ep, usuarioId);
        return registro(participacionRepo.save(ep));
    }

    @Override
    public ParticipacionDtos.Registro modificar(Long id, ParticipacionDtos.Request r, Long usuarioId) {
        EquipoPiloto ep = soporte.activa(participacionRepo, id, "la participación");
        validarReferencias(r);
        copiar(ep, r, r.rol() != null ? r.rol() : ep.getRol());
        soporte.cambio(ep, usuarioId);
        return registro(ep);
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        soporte.baja(soporte.activa(participacionRepo, id, "la participación"), usuarioId);
    }

    @Override
    public void registrarSiNoExiste(Long equipoId, Long pilotoId, Long categoriaId, Integer anio, Integer numero,
                                    String rol, Long usuarioId) {
        EquipoPiloto ep = participacionRepo
                .findByEquipoIdAndPilotoIdAndCategoriaIdAndTemporadaAndRol(equipoId, pilotoId, categoriaId, anio, rol)
                .orElse(null);
        if (ep == null) {
            ep = new EquipoPiloto();
            ep.setEquipo(equipoRepo.getReferenceById(equipoId));
            ep.setPiloto(pilotoRepo.getReferenceById(pilotoId));
            ep.setCategoria(categoriaRepo.getReferenceById(categoriaId));
            ep.setTemporada(anio);
            ep.setRol(rol);
            ep.setNumero(numero);
            soporte.alta(ep, usuarioId);
            participacionRepo.save(ep);
        } else if (ep.isEliminado()) {
            ep.setEliminado(false);
            soporte.cambio(ep, usuarioId);
        }
    }

    private void validarReferencias(ParticipacionDtos.Request r) {
        soporte.activa(categoriaRepo, r.categoriaId(), "la categoría");
        soporte.activa(pilotoRepo, r.pilotoId(), "el piloto");
        soporte.activa(equipoRepo, r.equipoId(), "el equipo");
        validarFechas(r.fechaDesde(), r.fechaHasta());
    }

    private void copiar(EquipoPiloto ep, ParticipacionDtos.Request r, String rol) {
        ep.setEquipo(equipoRepo.getReferenceById(r.equipoId()));
        ep.setPiloto(pilotoRepo.getReferenceById(r.pilotoId()));
        ep.setCategoria(categoriaRepo.getReferenceById(r.categoriaId()));
        ep.setTemporada(r.temporada());
        ep.setNumero(r.numero());
        ep.setRol(rol);
        ep.setFechaDesde(r.fechaDesde());
        ep.setFechaHasta(r.fechaHasta());
        ep.setMarca(r.marca() != null && !r.marca().isBlank() ? r.marca().trim() : null);
    }

    private ParticipacionDtos.Registro registro(EquipoPiloto ep) {
        return new ParticipacionDtos.Registro(ep.getId(), ep.getEquipo().getId(), ep.getEquipo().getNombre(),
                ep.getPiloto().getId(), nombre(ep.getPiloto()), ep.getCategoria().getId(), ep.getCategoria().getSiglas(),
                ep.getTemporada(), ep.getNumero(), ep.getRol(), ep.getFechaDesde(), ep.getFechaHasta(), ep.getMarca(),
                AuditoriaDto.de(ep));
    }
}
