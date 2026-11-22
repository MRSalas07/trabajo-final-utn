package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.ResultadoSesionDtos;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Catalogo.EstadoResultado;
import com.tfi.motorsport.backend.model.ResultadoSesion;
import com.tfi.motorsport.backend.model.Sesion;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.repositories.EquipoJPARepository;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import com.tfi.motorsport.backend.repositories.SesionJPARepository;
import com.tfi.motorsport.backend.services.gestion.IParticipacionGestionService;
import com.tfi.motorsport.backend.services.gestion.IResultadoSesionGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.nombre;
import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.texto;

@Service
@RequiredArgsConstructor
@Transactional
public class ResultadoSesionGestionServiceImpl implements IResultadoSesionGestionService {

    private final ResultadoSesionJPARepository resultadoSesionRepo;
    private final SesionJPARepository sesionRepo;
    private final PilotoJPARepository pilotoRepo;
    private final EquipoJPARepository equipoRepo;
    private final IParticipacionGestionService participaciones;
    private final SoporteGestion soporte;

    @Override
    @Transactional(readOnly = true)
    public List<ResultadoSesionDtos.Registro> listar(Long sesionId) {
        soporte.activa(sesionRepo, sesionId, "la sesión");
        return resultadoSesionRepo.tablaDeSesion(sesionId).stream().map(this::registro).toList();
    }

    @Override
    public ResultadoSesionDtos.Registro crear(ResultadoSesionDtos.Request r, Long usuarioId) {
        Sesion s = soporte.activa(sesionRepo, r.sesionId(), "la sesión");
        soporte.activa(pilotoRepo, r.pilotoId(), "el piloto");
        soporte.activa(equipoRepo, r.equipoId(), "el equipo");
        ResultadoSesion res = resultadoSesionRepo.findBySesionIdAndPilotoId(s.getId(), r.pilotoId()).orElse(null);
        if (res != null && !res.isEliminado()) {
            throw new RuleBusinessException("El piloto ya tiene un resultado en esta sesión: modifíquelo", HttpStatus.CONFLICT);
        }
        validarPosicionLibre(s.getId(), r.posicion(), r.pilotoId());
        if (res == null) {
            res = new ResultadoSesion();
            res.setSesion(s);
            res.setPiloto(pilotoRepo.getReferenceById(r.pilotoId()));
        }
        res.setEliminado(false);
        copiar(res, r);
        soporte.alta(res, usuarioId);
        ResultadoSesion guardado = resultadoSesionRepo.save(res);
        Temporada t = s.getEvento().getTemporada();
        participaciones.registrarSiNoExiste(r.equipoId(), r.pilotoId(), t.getCategoria().getId(), t.getAnio(), r.numero(),
                Catalogo.ROL_PILOTO, usuarioId);
        return registro(guardado);
    }

    @Override
    public ResultadoSesionDtos.Registro modificar(Long id, ResultadoSesionDtos.Request r, Long usuarioId) {
        ResultadoSesion res = soporte.activa(resultadoSesionRepo, id, "el resultado");
        if (!res.getPiloto().getId().equals(r.pilotoId()) || !res.getSesion().getId().equals(r.sesionId())) {
            throw new RuleBusinessException("No se puede cambiar la sesión ni el piloto de un resultado: elimínelo y cree otro");
        }
        soporte.activa(equipoRepo, r.equipoId(), "el equipo");
        validarPosicionLibre(r.sesionId(), r.posicion(), r.pilotoId());
        copiar(res, r);
        soporte.cambio(res, usuarioId);
        return registro(res);
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        soporte.baja(soporte.activa(resultadoSesionRepo, id, "el resultado"), usuarioId);
    }

    /** En una sesión no puede haber dos pilotos con la misma posición. */
    private void validarPosicionLibre(Long sesionId, Integer posicion, Long pilotoId) {
        if (posicion == null) {
            return;
        }
        resultadoSesionRepo.tablaDeSesion(sesionId).stream()
                .filter(o -> posicion.equals(o.getPosicion()) && !o.getPiloto().getId().equals(pilotoId))
                .findFirst()
                .ifPresent(o -> {
                    throw new RuleBusinessException("La posición " + posicion + " ya está asignada a "
                            + nombre(o.getPiloto()), HttpStatus.CONFLICT);
                });
    }

    private void copiar(ResultadoSesion res, ResultadoSesionDtos.Request r) {
        res.setEquipo(equipoRepo.getReferenceById(r.equipoId()));
        res.setPosicion(r.posicion());
        res.setNumero(r.numero());
        res.setPuntos(r.puntos());
        res.setTiempo(r.tiempo());
        res.setDiferencia(r.diferencia());
        res.setVueltas(r.vueltas());
        res.setMejorVuelta(r.mejorVuelta());
        res.setEstado(r.estado() != null ? r.estado() : EstadoResultado.FINALIZADO);
        res.setMotivoAbandono(texto(r.motivoAbandono()));
    }

    private ResultadoSesionDtos.Registro registro(ResultadoSesion r) {
        return new ResultadoSesionDtos.Registro(r.getId(), r.getSesion().getId(), r.getPiloto().getId(), nombre(r.getPiloto()),
                r.getEquipo().getId(), r.getEquipo().getNombre(), r.getPosicion(), r.getNumero(), r.getPuntos(),
                r.getTiempo(), r.getDiferencia(), r.getVueltas(), r.getMejorVuelta(), r.getEstado(), r.getMotivoAbandono());
    }
}
