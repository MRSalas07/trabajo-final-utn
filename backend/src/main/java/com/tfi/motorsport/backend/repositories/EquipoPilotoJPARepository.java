package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.EquipoPiloto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoPilotoJPARepository extends JpaRepository<EquipoPiloto, Long> {

    /** Trayectoria de un piloto: de la participación más reciente a la más antigua. */
    @Query("""
            select ep from EquipoPiloto ep join fetch ep.equipo join fetch ep.categoria
            where ep.piloto.id = :pilotoId and ep.eliminado = false
            order by ep.temporada desc, ep.fechaDesde desc nulls last
            """)
    List<EquipoPiloto> trayectoriaDePiloto(Long pilotoId);

    /** Plantel histórico de un equipo, agrupable por temporada. */
    @Query("""
            select ep from EquipoPiloto ep join fetch ep.piloto p join fetch ep.categoria
            where ep.equipo.id = :equipoId and ep.eliminado = false
            order by ep.temporada desc, ep.nroPiloto asc nulls last, p.apellido
            """)
    List<EquipoPiloto> plantelDeEquipo(Long equipoId);

    /*
     * Participaciones de varios pilotos en una sola consulta (evita N+1 en el listado).
     * Respeta los mismos filtros opcionales del listado y viene ordenada de la más reciente
     * a la más antigua: la primera de cada piloto es su equipo actual.
     */
    @Query("""
            select ep from EquipoPiloto ep join fetch ep.equipo
            where ep.piloto.id in :pilotoIds and ep.eliminado = false
              and (:categoriaId is null or ep.categoria.id = :categoriaId)
              and (:anio is null or ep.temporada = :anio)
            order by ep.temporada desc, ep.fechaDesde desc nulls last
            """)
    List<EquipoPiloto> participacionesDePilotos(Collection<Long> pilotoIds, Long categoriaId, Integer anio);

    Optional<EquipoPiloto> findByEquipoIdAndPilotoIdAndCategoriaIdAndTemporadaAndRol(
            Long equipoId, Long pilotoId, Long categoriaId, Integer temporada, String rol);
}
