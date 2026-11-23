package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.AuditoriaDto;
import com.tfi.motorsport.backend.dtos.gestionDatos.EquipoDtos;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Equipo;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import com.tfi.motorsport.backend.repositories.TripulacionJPARepository;
import com.tfi.motorsport.backend.services.gestion.IEquipoGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.LIMITE_LISTADO;
import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.patron;
import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.texto;

@Service
@RequiredArgsConstructor
@Transactional
public class EquipoGestionServiceImpl implements IEquipoGestionService {

    private final EquipoJPARepository equipoRepo;
    private final ResultadoSesionJPARepository resultadoSesionRepo;
    private final TripulacionJPARepository tripulacionRepo;
    private final SoporteGestion soporte;

    @Override
    @Transactional(readOnly = true)
    public List<EquipoDtos.Registro> listar(String texto, int limite) {
        return equipoRepo.buscarPorTexto(patron(texto), PageRequest.of(0, Math.min(limite, LIMITE_LISTADO)))
                .stream().map(this::registro).toList();
    }

    @Override
    public EquipoDtos.Registro crear(EquipoDtos.Request r, Long usuarioId) {
        Equipo e = new Equipo();
        copiar(e, r);
        soporte.alta(e, usuarioId);
        return registro(equipoRepo.save(e));
    }

    @Override
    public EquipoDtos.Registro modificar(Long id, EquipoDtos.Request r, Long usuarioId) {
        Equipo e = soporte.activa(equipoRepo, id, "el equipo");
        copiar(e, r);
        soporte.cambio(e, usuarioId);
        return registro(e);
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        Equipo e = soporte.activa(equipoRepo, id, "el equipo");
        long resultados = resultadoSesionRepo.countByEquipoIdAndEliminadoFalse(id);
        long tripulaciones = tripulacionRepo.countByEquipoIdAndEliminadoFalse(id);
        if (resultados > 0 || tripulaciones > 0) {
            throw new RuleBusinessException("No se puede dar de baja: el equipo tiene " + resultados
                    + " resultados y " + tripulaciones + " tripulaciones activas. Elimínelos primero.", HttpStatus.CONFLICT);
        }
        soporte.baja(e, usuarioId);
    }

    private void copiar(Equipo e, EquipoDtos.Request r) {
        e.setNombre(r.nombre().trim());
        e.setNombreCorto(texto(r.nombreCorto()));
        e.setNacionalidad(texto(r.nacionalidad()));
        e.setLogoUrl(texto(r.logoUrl()));
        e.setFechaFundacion(r.fechaFundacion());
        e.setSede(texto(r.sede()));
    }

    private EquipoDtos.Registro registro(Equipo e) {
        return new EquipoDtos.Registro(e.getId(), e.getIdExterno(), e.getNombre(), e.getNombreCorto(), e.getNacionalidad(),
                e.getLogoUrl(), e.getFechaFundacion(), e.getSede(),
                AuditoriaDto.de(e));
    }
}
