package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.EtapaDtos;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Catalogo;
import com.tfi.motorsport.backend.model.Catalogo.EstadoEvento;
import com.tfi.motorsport.backend.model.Etapa;
import com.tfi.motorsport.backend.model.Evento;
import com.tfi.motorsport.backend.repositories.EtapaJPARepository;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.services.gestion.IEtapaGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.validarInstantes;

@Service
@RequiredArgsConstructor
@Transactional
public class EtapaGestionServiceImpl implements IEtapaGestionService {

    private final EtapaJPARepository etapaRepo;
    private final EventoJPARepository eventoRepo;
    private final SoporteGestion soporte;

    @Override
    @Transactional(readOnly = true)
    public List<EtapaDtos.Registro> listar(Long eventoId) {
        soporte.activa(eventoRepo, eventoId, "el evento");
        return etapaRepo.listarPorEventos(List.of(eventoId)).stream().map(this::registro).toList();
    }

    @Override
    public EtapaDtos.Registro crear(EtapaDtos.Request r, Long usuarioId) {
        Evento evento = soporte.activa(eventoRepo, r.eventoId(), "el evento");
        Etapa et = null;
        if (Boolean.TRUE.equals(r.clasificacionGeneral())) {
            // Solo una clasificación general por evento (índice único evento + id_externo)
            et = etapaRepo.findByEventoIdAndIdExterno(evento.getId(), Catalogo.ETAPA_GENERAL).orElse(null);
            if (et != null && !et.isEliminado()) {
                throw new RuleBusinessException("El evento ya tiene una clasificación general", HttpStatus.CONFLICT);
            }
        }
        if (et == null) {
            et = new Etapa();
            et.setEvento(evento);
            et.setIdExterno(Boolean.TRUE.equals(r.clasificacionGeneral()) ? Catalogo.ETAPA_GENERAL : null);
        }
        et.setEliminado(false);
        copiar(et, r);
        soporte.alta(et, usuarioId);
        return registro(etapaRepo.save(et));
    }

    @Override
    public EtapaDtos.Registro modificar(Long id, EtapaDtos.Request r, Long usuarioId) {
        Etapa et = soporte.activa(etapaRepo, id, "la etapa");
        if (!et.getEvento().getId().equals(r.eventoId())) {
            et.setEvento(soporte.activa(eventoRepo, r.eventoId(), "el evento"));
        }
        copiar(et, r);
        soporte.cambio(et, usuarioId);
        return registro(et);
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        soporte.baja(soporte.activa(etapaRepo, id, "la etapa"), usuarioId);
    }

    private void copiar(Etapa et, EtapaDtos.Request r) {
        validarInstantes(r.inicio(), r.fin());
        et.setNumero(r.numero());
        et.setNombre(r.nombre().trim());
        et.setDia(r.dia());
        et.setDistancia(r.distancia());
        et.setFechaInicio(r.inicio());
        et.setFechaFin(r.fin());
        et.setEstado(r.estado() != null ? r.estado() : EstadoEvento.PROGRAMADO);
    }

    private EtapaDtos.Registro registro(Etapa et) {
        return new EtapaDtos.Registro(et.getId(), et.getEvento().getId(), et.getIdExterno(), et.getNumero(), et.getNombre(),
                et.getDia(), et.getDistancia(), et.getFechaInicio(), et.getFechaFin(), et.getEstado(),
                Catalogo.ETAPA_GENERAL.equals(et.getIdExterno()));
    }
}
