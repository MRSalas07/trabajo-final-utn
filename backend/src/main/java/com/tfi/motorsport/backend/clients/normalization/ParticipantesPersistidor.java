package com.tfi.motorsport.backend.clients.normalization;

import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.EquipoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.PilotoNorm;
import com.tfi.motorsport.backend.clients.normalization.ModeloNormalizado.ResultadoEtapaNorm;
import com.tfi.motorsport.backend.model.*;
import com.tfi.motorsport.backend.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Guarda a los PARTICIPANTES: pilotos, equipos, tripulaciones de rally y su relación
 * equipo-piloto (tabla equipo_x_piloto).
 *
 * Todos los métodos hacen UPSERT por id_externo (si existe se actualiza, si no se crea)
 * y usan las cachés del contexto para buscar cada piloto o equipo una sola vez por ejecución.
 * Se llaman siempre dentro de la transacción del evento que abre NormalizadorService.
 */
@Component
@RequiredArgsConstructor
class ParticipantesPersistidor {

    private final CategoriaJPARepository categoriaRepo;
    private final PilotoJPARepository pilotoRepo;
    private final EquipoJPARepository equipoRepo;
    private final EquipoPilotoJPARepository equipoPilotoRepo;
    private final TripulacionJPARepository tripulacionRepo;

    /** Piloto global (puede correr en varias categorías). Se actualiza una vez por ejecución. */
    Long upsertPiloto(PilotoNorm p, ContextoNormalizacion ctx) {
        Long cacheado = ctx.pilotos.get(p.idExterno());
        if (cacheado != null) {
            return cacheado;
        }
        Piloto piloto = pilotoRepo.findByIdExterno(p.idExterno()).orElse(null);
        if (piloto == null) {
            piloto = new Piloto();
            piloto.setIdExterno(p.idExterno());
            ctx.pilotosNuevos++;
        }
        // Solo se pisan los campos que la fuente informa (no se borran datos cargados por otra vía)
        if (p.nombre() != null) piloto.setNombre(p.nombre().trim());
        piloto.setApellido(p.apellido().trim());
        if (p.fechaNacimiento() != null) piloto.setFechaNacimiento(p.fechaNacimiento());
        if (p.nacionalidad() != null) piloto.setNacionalidad(p.nacionalidad());
        if (p.pais() != null) piloto.setPais(p.pais());
        if (p.codigo() != null) piloto.setCodigoPiloto(p.codigo());
        if (p.numero() != null) piloto.setNumeroActual(p.numero());
        Long id = pilotoRepo.save(piloto).getId();
        ctx.pilotos.put(p.idExterno(), id);
        return id;
    }

    Long upsertEquipo(EquipoNorm e, ContextoNormalizacion ctx) {
        Long cacheado = ctx.equipos.get(e.idExterno());
        if (cacheado != null) {
            return cacheado;
        }
        Equipo equipo = equipoRepo.findByIdExterno(e.idExterno()).orElse(null);
        if (equipo == null) {
            equipo = new Equipo();
            equipo.setIdExterno(e.idExterno());
            ctx.equiposNuevos++;
        }
        equipo.setNombre(e.nombre().trim());
        if (e.nombreCorto() != null) equipo.setNombreCorto(e.nombreCorto());
        if (e.nacionalidad() != null) equipo.setNacionalidad(e.nacionalidad());
        Long id = equipoRepo.save(equipo).getId();
        ctx.equipos.put(e.idExterno(), id);
        return id;
    }

    /** Tripulación de rally: piloto + copiloto + equipo en una categoría y temporada. */
    Long upsertTripulacion(ResultadoEtapaNorm r, ContextoNormalizacion ctx) {
        Long pilotoId = upsertPiloto(r.piloto(), ctx);
        Long copilotoId = upsertPiloto(r.copiloto(), ctx);
        Long equipoId = upsertEquipo(r.equipo(), ctx);
        Tripulacion tr = tripulacionRepo
                .findByCategoriaIdAndTemporadaAndPilotoIdAndCopilotoIdAndEquipoId(
                        ctx.categoriaId, ctx.anio, pilotoId, copilotoId, equipoId)
                .orElseGet(() -> {
                    Tripulacion nueva = new Tripulacion();
                    nueva.setCategoria(categoriaRepo.getReferenceById(ctx.categoriaId));
                    nueva.setTemporada(ctx.anio);
                    nueva.setPiloto(pilotoRepo.getReferenceById(pilotoId));
                    nueva.setCopiloto(pilotoRepo.getReferenceById(copilotoId));
                    nueva.setEquipo(equipoRepo.getReferenceById(equipoId));
                    return nueva;
                });
        tr.setNumero(r.numero());
        Long id = tripulacionRepo.save(tr).getId();
        // Ambos integrantes quedan registrados en equipo_x_piloto (perfiles y listados unificados)
        registrarParticipacion(equipoId, pilotoId, r.numero(), Catalogo.ROL_PILOTO, ctx);
        registrarParticipacion(equipoId, copilotoId, r.numero(), Catalogo.ROL_COPILOTO, ctx);
        return id;
    }

    /** Registra (una sola vez) que un piloto corrió para un equipo en una categoría y temporada. */
    void registrarParticipacion(Long equipoId, Long pilotoId, Integer numero, String rol, ContextoNormalizacion ctx) {
        registrarParticipacion(equipoId, pilotoId, numero, rol, null, ctx);
    }

    /**
     * Igual que el anterior, y además guarda la marca/motor del piloto en la participación (TC).
     * Si la marca viene vacía no se toca la que ya tenía (por ejemplo, la cargada a mano en el ABM).
     */
    void registrarParticipacion(Long equipoId, Long pilotoId, Integer numero, String rol, String marca,
                                ContextoNormalizacion ctx) {
        String clave = equipoId + "-" + pilotoId + "-" + rol;
        String marcaLimpia = marca != null && !marca.isBlank() ? marca.trim() : null;
        boolean primeraVez = ctx.participaciones.add(clave);
        if (!primeraVez && (marcaLimpia == null || marcaLimpia.equals(ctx.marcas.get(clave)))) {
            return; // ya registrada en esta ejecución (y sin marca nueva para guardar)
        }
        EquipoPiloto ep = equipoPilotoRepo
                .findByEquipoIdAndPilotoIdAndCategoriaIdAndTemporadaAndRol(equipoId, pilotoId, ctx.categoriaId, ctx.anio, rol)
                .orElse(null);
        if (ep == null) {
            ep = new EquipoPiloto();
            ep.setEquipo(equipoRepo.getReferenceById(equipoId));
            ep.setPiloto(pilotoRepo.getReferenceById(pilotoId));
            ep.setCategoria(categoriaRepo.getReferenceById(ctx.categoriaId));
            ep.setTemporada(ctx.anio);
            ep.setRol(rol);
            ep.setNumero(numero);
            ep.setMarca(marcaLimpia);
            equipoPilotoRepo.save(ep);
        } else {
            // Sin save(): la entidad está administrada por JPA dentro de la transacción
            // y los cambios se guardan solos al confirmarla (dirty checking).
            if (numero != null && !numero.equals(ep.getNumero())) {
                ep.setNumero(numero);
            }
            if (marcaLimpia != null && !marcaLimpia.equals(ep.getMarca())) {
                ep.setMarca(marcaLimpia);
            }
        }
        if (marcaLimpia != null) {
            ctx.marcas.put(clave, marcaLimpia);
        }
    }
}
