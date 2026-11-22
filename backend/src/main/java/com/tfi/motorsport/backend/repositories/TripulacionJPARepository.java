package com.tfi.motorsport.backend.repositories;

import com.tfi.motorsport.backend.model.Tripulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripulacionJPARepository extends JpaRepository<Tripulacion, Long> {

    long countByPilotoIdAndEliminadoFalse(Long pilotoId);

    long countByCopilotoIdAndEliminadoFalse(Long pilotoId);

    long countByEquipoIdAndEliminadoFalse(Long equipoId);

    Optional<Tripulacion> findByCategoriaIdAndTemporadaAndPilotoIdAndCopilotoIdAndEquipoId(
            Long categoriaId, Integer anio, Long pilotoId, Long copilotoId, Long equipoId);

    @Query("""
            select tr from Tripulacion tr join fetch tr.piloto join fetch tr.copiloto join fetch tr.equipo join fetch tr.categoria c
            where tr.eliminado = false and c.id = :categoriaId and (:anio is null or tr.temporada = :anio)
            order by tr.temporada desc, tr.numero
            """)
    List<Tripulacion> listar(Long categoriaId, Integer anio);
}


