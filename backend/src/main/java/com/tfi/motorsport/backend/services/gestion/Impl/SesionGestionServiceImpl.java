package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.SesionDtos;
import com.tfi.motorsport.backend.model.Catalogo.EstadoEvento;
import com.tfi.motorsport.backend.model.Sesion;
import com.tfi.motorsport.backend.repositories.EventoJPARepository;
import com.tfi.motorsport.backend.repositories.SesionJPARepository;
import com.tfi.motorsport.backend.services.gestion.ISesionGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.validarInstantes;

@Service
@RequiredArgsConstructor
@Transactional
public class SesionGestionServiceImpl implements ISesionGestionService {

    private final SesionJPARepository sesionRepo;
    private final EventoJPARepository eventoRepo;
    private final SoporteGestion soporte;

    @Override
    @Transactional(readOnly = true)
    public List<SesionDtos.Registro> listar(Long eventoId) {
        soporte.activa(eventoRepo, eventoId, "el evento");
        return sesionRepo.listarPorEventos(List.of(eventoId)).stream().map(this::registro).toList();
    }

    @Override
    public SesionDtos.Registro crear(SesionDtos.Request r, Long usuarioId) {
        Sesion s = new Sesion();
        s.setEvento(soporte.activa(eventoRepo, r.eventoId(), "el evento"));
        copiar(s, r);
        soporte.alta(s, usuarioId);
        return registro(sesionRepo.save(s));
    }

    @Override
    public SesionDtos.Registro modificar(Long id, SesionDtos.Request r, Long usuarioId) {
        Sesion s = soporte.activa(sesionRepo, id, "la sesión");
        if (!s.getEvento().getId().equals(r.eventoId())) {
            s.setEvento(soporte.activa(eventoRepo, r.eventoId(), "el evento"));
        }
        copiar(s, r);
        soporte.cambio(s, usuarioId);
        return registro(s);
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        soporte.baja(soporte.activa(sesionRepo, id, "la sesión"), usuarioId);
    }

    private void copiar(Sesion s, SesionDtos.Request r) {
        validarInstantes(r.inicio(), r.fin());
        s.setNombre(r.nombre().trim());
        s.setTipo(r.tipo());
        s.setNumero(r.numero());
        s.setFechaInicio(r.inicio());
        s.setFechaFin(r.fin());
        s.setVueltasProgramadas(r.vueltasProgramadas());
        s.setDistancia(r.distancia());
        s.setEstado(r.estado() != null ? r.estado() : EstadoEvento.PROGRAMADO);
    }

    private SesionDtos.Registro registro(Sesion s) {
        return new SesionDtos.Registro(s.getId(), s.getEvento().getId(), s.getIdExterno(), s.getNombre(), s.getTipo(),
                s.getNumero(), s.getFechaInicio(), s.getFechaFin(), s.getVueltasProgramadas(), s.getDistancia(),
                s.getEstado());
    }
}
