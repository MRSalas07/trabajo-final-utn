package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.EventoDtos;
import com.tfi.motorsport.backend.model.Catalogo.EstadoEvento;
import com.tfi.motorsport.backend.model.Evento;
import com.tfi.motorsport.backend.model.Temporada;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.repositories.TemporadaJPARepository;
import com.tfi.motorsport.backend.services.ZonaHoraria;
import com.tfi.motorsport.backend.services.gestion.IEventoGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.texto;
import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.validarInstantes;

@Service
@RequiredArgsConstructor
@Transactional
public class EventoGestionServiceImpl implements IEventoGestionService {

    private final EventoJPARepository eventoRepo;
    private final TemporadaJPARepository temporadaRepo;
    private final SoporteGestion soporte;

    @Override
    @Transactional(readOnly = true)
    public List<EventoDtos.Registro> listar(Long temporadaId) {
        soporte.activa(temporadaRepo, temporadaId, "la temporada");
        return eventoRepo.listarPorTemporada(temporadaId).stream().map(this::registro).toList();
    }

    @Override
    public EventoDtos.Registro crear(EventoDtos.Request r, Long usuarioId) {
        Evento e = new Evento();
        e.setTemporada(soporte.activa(temporadaRepo, r.temporadaId(), "la temporada"));
        copiar(e, r);
        soporte.alta(e, usuarioId);
        return registro(eventoRepo.save(e));
    }

    @Override
    public EventoDtos.Registro modificar(Long id, EventoDtos.Request r, Long usuarioId) {
        Evento e = soporte.activa(eventoRepo, id, "el evento");
        if (!e.getTemporada().getId().equals(r.temporadaId())) {
            e.setTemporada(soporte.activa(temporadaRepo, r.temporadaId(), "la temporada"));
        }
        copiar(e, r);
        soporte.cambio(e, usuarioId);
        return registro(e);
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        soporte.baja(soporte.activa(eventoRepo, id, "el evento"), usuarioId);
    }

    private void copiar(Evento e, EventoDtos.Request r) {
        validarInstantes(r.inicio(), r.fin());
        e.setNombre(r.nombre().trim());
        e.setNombreCorto(texto(r.nombreCorto()));
        e.setNumeroFecha(r.numeroFecha());
        e.setPais(texto(r.pais()));
        e.setCiudad(texto(r.ciudad()));
        e.setCircuito(texto(r.circuito()));
        e.setZonaHoraria(texto(r.zonaHoraria()) == null ? "UTC" : ZonaHoraria.resolverZonaHoraria(r.zonaHoraria()).getId());
        e.setFechaInicio(r.inicio());
        e.setFechaFin(r.fin() != null ? r.fin() : r.inicio());
        e.setEstado(r.estado() != null ? r.estado() : EstadoEvento.PROGRAMADO);
    }

    private EventoDtos.Registro registro(Evento e) {
        Temporada t = e.getTemporada();
        return new EventoDtos.Registro(e.getId(), t.getId(), t.getAnio(), t.getCategoria().getSiglas(), e.getIdExterno(),
                e.getNombre(), e.getNombreCorto(), e.getNumeroFecha(), e.getPais(), e.getCiudad(), e.getCircuito(),
                e.getZonaHoraria(), e.getFechaInicio(), e.getFechaFin(), e.getEstado());
    }
}
