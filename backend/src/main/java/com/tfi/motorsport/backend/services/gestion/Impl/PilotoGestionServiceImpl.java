package com.tfi.motorsport.backend.services.gestion.Impl;

import com.tfi.motorsport.backend.dtos.gestionDatos.PilotoDtos;
import com.tfi.motorsport.backend.handlers.exceptions.RuleBusinessException;
import com.tfi.motorsport.backend.model.Piloto;
import com.tfi.motorsport.backend.repositories.PilotoJPARepository;
import com.tfi.motorsport.backend.repositories.ResultadoSesionJPARepository;
import com.tfi.motorsport.backend.repositories.TripulacionJPARepository;
import com.tfi.motorsport.backend.services.gestion.IPilotoGestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.LIMITE_LISTADO;
import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.patron;
import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.texto;
import static com.tfi.motorsport.backend.services.gestion.Impl.SoporteGestion.validarFechas;

@Service
@RequiredArgsConstructor
@Transactional
public class PilotoGestionServiceImpl implements IPilotoGestionService {

    private static final Sort ORDEN_LISTADO = Sort.by("apellido", "nombre", "id");

    private final PilotoJPARepository pilotoRepo;
    private final ResultadoSesionJPARepository resultadoSesionRepo;
    private final TripulacionJPARepository tripulacionRepo;
    private final SoporteGestion soporte;

    @Override
    @Transactional(readOnly = true)
    public List<PilotoDtos.Registro> listar(String texto, int limite) {
        return pilotoRepo.buscar(patron(texto), false, null, null,
                        PageRequest.of(0, Math.min(limite, LIMITE_LISTADO), ORDEN_LISTADO)).stream()
                .map(this::registro).toList();
    }

    @Override
    public PilotoDtos.Registro crear(PilotoDtos.Request r, Long usuarioId) {
        Piloto p = new Piloto();
        copiar(p, r);
        soporte.alta(p, usuarioId);
        return registro(pilotoRepo.save(p));
    }

    @Override
    public PilotoDtos.Registro modificar(Long id, PilotoDtos.Request r, Long usuarioId) {
        Piloto p = soporte.activa(pilotoRepo, id, "el piloto");
        copiar(p, r);
        soporte.cambio(p, usuarioId);
        return registro(p);
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        Piloto p = soporte.activa(pilotoRepo, id, "el piloto");
        // Integridad: se consultan los repositorios (no otros servicios) para no crear dependencias circulares
        long resultados = resultadoSesionRepo.countByPilotoIdAndEliminadoFalse(id);
        long tripulaciones = tripulacionRepo.countByPilotoIdAndEliminadoFalse(id) + tripulacionRepo.countByCopilotoIdAndEliminadoFalse(id);
        if (resultados > 0 || tripulaciones > 0) {
            throw new RuleBusinessException("No se puede dar de baja: el piloto tiene " + resultados
                    + " resultados y " + tripulaciones + " tripulaciones activas. Elimínelos primero.", HttpStatus.CONFLICT);
        }
        soporte.baja(p, usuarioId);
    }

    private void copiar(Piloto p, PilotoDtos.Request r) {
        validarFechas(r.fechaInicioCarrera(), r.fechaFinCarrera());
        p.setNombre(texto(r.nombre()));
        p.setApellido(r.apellido().trim());
        p.setFechaNacimiento(r.fechaNacimiento());
        p.setNacionalidad(texto(r.nacionalidad()));
        p.setPais(texto(r.pais()));
        p.setCodigoPiloto(texto(r.codigo()) != null ? r.codigo().trim().toUpperCase() : null);
        p.setNumeroActual(r.numero());
        p.setFotoUrl(texto(r.fotoUrl()));
        p.setFechaInicioCarrera(r.fechaInicioCarrera());
        p.setFechaFinCarrera(r.fechaFinCarrera());
    }

    private PilotoDtos.Registro registro(Piloto p) {
        return new PilotoDtos.Registro(p.getId(), p.getIdExterno(), p.getNombre(), p.getApellido(), p.getFechaNacimiento(),
                p.getNacionalidad(), p.getPais(), p.getCodigoPiloto(), p.getNumeroActual(), p.getFotoUrl(),
                p.getFechaInicioCarrera(), p.getFechaFinCarrera());
    }
}
