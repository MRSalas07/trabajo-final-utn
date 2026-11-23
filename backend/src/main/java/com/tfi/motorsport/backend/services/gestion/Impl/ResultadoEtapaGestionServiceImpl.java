package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.AuditoriaDto;
import com.tfi.motorsport.backend.dtos.gestionDatos.ResultadoEtapaDtos;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Catalogo.EstadoResultado;
import com.tfi.motorsport.backend.model.Etapa;
import com.tfi.motorsport.backend.model.ResultadoEtapa;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.model.Tripulacion;
import com.tfi.motorsport.backend.repositories.EtapaJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoEtapaJPARepository;
import com.tfi.motorsport.backend.repositories.TripulacionJPARepository;
import com.tfi.motorsport.backend.services.gestion.IResultadoEtapaGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.nombre;
import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.texto;

@Service
@RequiredArgsConstructor
@Transactional
public class ResultadoEtapaGestionServiceImpl implements IResultadoEtapaGestionService {

    private final ResultadoEtapaJPARepository resultadoEtapaRepo;
    private final EtapaJPARepository etapaRepo;
    private final TripulacionJPARepository tripulacionRepo;
    private final SoporteGestion soporte;

    @Override
    @Transactional(readOnly = true)
    public List<ResultadoEtapaDtos.Registro> listar(Long etapaId) {
        soporte.activa(etapaRepo, etapaId, "la etapa");
        return resultadoEtapaRepo.tablaDeEtapa(etapaId).stream().map(this::registro).toList();
    }

    @Override
    public ResultadoEtapaDtos.Registro crear(ResultadoEtapaDtos.Request r, Long usuarioId) {
        Etapa et = soporte.activa(etapaRepo, r.etapaId(), "la etapa");
        Tripulacion tr = soporte.activa(tripulacionRepo, r.tripulacionId(), "la tripulación");
        validarTripulacionDeEtapa(et, tr);
        ResultadoEtapa res = resultadoEtapaRepo.findByEtapaIdAndTripulacionId(et.getId(), tr.getId()).orElse(null);
        if (res != null && !res.isEliminado()) {
            throw new RuleBusinessException("La tripulación ya tiene un resultado en esta etapa: modifíquelo", HttpStatus.CONFLICT);
        }
        if (res == null) {
            res = new ResultadoEtapa();
            res.setEtapa(et);
            res.setTripulacion(tr);
        }
        res.setEliminado(false);
        copiar(res, r);
        soporte.alta(res, usuarioId);
        return registro(resultadoEtapaRepo.save(res));
    }

    @Override
    public ResultadoEtapaDtos.Registro modificar(Long id, ResultadoEtapaDtos.Request r, Long usuarioId) {
        ResultadoEtapa res = soporte.activa(resultadoEtapaRepo, id, "el resultado");
        if (!res.getTripulacion().getId().equals(r.tripulacionId()) || !res.getEtapa().getId().equals(r.etapaId())) {
            throw new RuleBusinessException("No se puede cambiar la etapa ni la tripulación de un resultado: elimínelo y cree otro");
        }
        copiar(res, r);
        soporte.cambio(res, usuarioId);
        return registro(res);
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        soporte.baja(soporte.activa(resultadoEtapaRepo, id, "el resultado"), usuarioId);
    }

    /** La tripulación debe ser de la misma categoría y temporada que el rally de la etapa. */
    private void validarTripulacionDeEtapa(Etapa et, Tripulacion tr) {
        Temporada t = et.getEvento().getTemporada();
        if (!tr.getCategoria().getId().equals(t.getCategoria().getId()) || !Objects.equals(tr.getTemporada(), t.getAnio())) {
            throw new RuleBusinessException("La tripulación no corresponde a " + t.getCategoria().getSiglas() + " " + t.getAnio());
        }
    }

    private void copiar(ResultadoEtapa res, ResultadoEtapaDtos.Request r) {
        res.setPosicion(r.posicion());
        res.setTiempo(r.tiempo());
        res.setDiferencia(r.diferencia());
        res.setPuntos(r.puntos());
        res.setEstado(r.estado() != null ? r.estado() : EstadoResultado.FINALIZADO);
        res.setMotivoAbandono(texto(r.motivoAbandono()));
    }

    private ResultadoEtapaDtos.Registro registro(ResultadoEtapa r) {
        Tripulacion t = r.getTripulacion();
        return new ResultadoEtapaDtos.Registro(r.getId(), r.getEtapa().getId(), t.getId(),
                nombre(t.getPiloto()) + " / " + nombre(t.getCopiloto()), r.getPosicion(), r.getTiempo(),
                r.getDiferencia(), r.getPuntos(), r.getEstado(), r.getMotivoAbandono(),
                AuditoriaDto.de(r));
    }
}
